package trashsoftware.winBwz.core.bwz;

import trashsoftware.winBwz.core.DeCompressor;
import trashsoftware.winBwz.core.bwz.bwt.BWTDecoder;
import trashsoftware.winBwz.core.options.BWZOptions;
import trashsoftware.winBwz.core.options.EntropyMethod;
import trashsoftware.winBwz.huffman.MapCompressor.BwzMapDeCompressor;
import trashsoftware.winBwz.longHuffman.LongHuffmanDecompressorRam;
import trashsoftware.winBwz.longHuffman.LongHuffmanInputStream;
import trashsoftware.winBwz.packer.pz.PzUnPacker;
import trashsoftware.winBwz.rangeCodec.freq.*;
import trashsoftware.winBwz.rangeCodec.RangeDecompressorRam;
import trashsoftware.winBwz.utility.Bytes;
import trashsoftware.winBwz.utility.Util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;

//import trashsoftware.win_bwz.LongHuffman.LongHuffmanInputStreamBit;
//import java.util.Arrays;

/**
 * BWZ-algorithm decompressor, implements the {@code DeCompressor} interface.
 *
 * @author zbh
 * @see DeCompressor
 * @since 0.5
 */
public class BWZDeCompressor implements DeCompressor {

    private final int entropyChunkBaseSize;
    private final int windowSize;

    private final FileChannel fc;
    //    private InputStream fis;
    private final LinkedList<byte[]> huffmanMaps = new LinkedList<>();
    PzUnPacker unPacker;
    private int algVersion = BWZCompressor.VERSION;
    boolean isRunning = true;
    long ratio, pos;
    private int threadNum = 1;  // Default thread number.
    private long lastUpdateProgress;
    BWZOptions options;

    private long lastCheckTime;
//    private long outPosition;
//    long initBytePos;

    /**
     * Constructs a new {@code BWZDeCompressor} instance.
     *
     * @param inFile  the name or path of the file to be uncompressed
     * @param options bwz options  
     * @throws IOException if the file is not readable
     */
    public BWZDeCompressor(String inFile,
                           long startPos,
                           BWZOptions options)
            throws IOException {
        this.windowSize = options.getWindowSize();
        this.options = options;

        fc = new FileInputStream(inFile).getChannel();
        fc.position(startPos);
        ByteBuffer buffer = ByteBuffer.allocate(1);

        int r;
        r = fc.read(buffer);

        if (r != 1) throw new IOException("Error occurs during reading");
        entropyChunkBaseSize = (int) Math.pow(2, buffer.get(0));
    }

    private void fillMaps(List<byte[]> huffmanMaps, byte[] block, int mapLen, int origRow) throws IOException {
        byte[] cmpMap = new byte[mapLen];
        System.arraycopy(block, 0, cmpMap, 0, mapLen);

        int maxMapsLen = (windowSize / entropyChunkBaseSize + 1) * 259;
//        System.out.println(maxMapsLen);
//        System.out.println(windowSize + " " + entropyChunkBaseSize);
        byte[] uncMap = new BwzMapDeCompressor(cmpMap).
                Uncompress(maxMapsLen, 32);
        int[] uncMapInt = new int[uncMap.length];
        for (int i = 0; i < uncMap.length; ++i) {
            uncMapInt[i] = uncMap[i] & 0xff;
        }
        int[] rldMap = new ZeroRLCDecoder(uncMapInt, maxMapsLen).Decode();
        int[] mftMap = new MTFInverse(rldMap).decode(257);
        byte[] maps = BWTDecoder.createWithOrigIndex(mftMap, origRow).Decode();

        int i = 0;
        while (i < maps.length) {
            byte[] map = new byte[BWZCompressor.HUFFMAN_TABLE_SIZE];
            System.arraycopy(maps, i, map, 0, BWZCompressor.HUFFMAN_TABLE_SIZE);
            i += BWZCompressor.HUFFMAN_TABLE_SIZE;
            huffmanMaps.add(map);
        }
    }

    private void decodeHuffmanOld(OutputStream out, FileChannel fc) throws Exception {
        int huffmanBlockForEachWindow;
        if (windowSize <= entropyChunkBaseSize) huffmanBlockForEachWindow = 1;
        else huffmanBlockForEachWindow = windowSize / entropyChunkBaseSize;

        ArrayList<DecTextContainer> blockList = new ArrayList<>();
        ArrayList<int[]> huffmanBlockList = new ArrayList<>();

        LongHuffmanInputStream his =
                new LongHuffmanInputStream(fc, BWZCompressor.HUFFMAN_TABLE_SIZE, windowSize);
        int[] huffmanResult;
        byte[] headBytes;
        byte[] blockBytes;

//        boolean blockPlain = false;

        while (true) {
            if (huffmanMaps.isEmpty()) {
                headBytes = his.readPlain(6);
                if (headBytes == null) break;  // Reach the end of the stream.

                int mapLen = Bytes.bytesToInt24(headBytes, 0);
                int origRow = Bytes.bytesToInt24(headBytes, 3);
//                System.out.format("%d %d %d\n", flagLen, mapLen, origRow);
                blockBytes = his.readPlain(mapLen);
                if (blockBytes == null) {
                    throw new RuntimeException("Cannot read block");
                }

                fillMaps(huffmanMaps, blockBytes, mapLen, origRow);
            }

            byte[] map = huffmanMaps.removeFirst();
            huffmanResult = his.readNextCompressedBlock(map, BWZCompressor.HUFFMAN_END_SIG);

            huffmanBlockList.add(huffmanResult);

            // Got a complete block for rlc decoding, or reaches the end of file.
            if (huffmanBlockList.size() == huffmanBlockForEachWindow || huffmanMaps.isEmpty()) {
                int totalLen = totalLength(huffmanBlockList);
                int[] concatenateHuffman = new int[totalLen];
                int index = 0;
                for (int[] s : huffmanBlockList) {
                    System.arraycopy(s, 0, concatenateHuffman, index, s.length);
                    index += s.length;
                }

                blockList.add(new DecTextContainer(concatenateHuffman, null));
                huffmanBlockList.clear();

                // Got enough blocks to start multi-threading, or reaches the end of file.
                if (blockList.size() == threadNum || huffmanMaps.isEmpty()) {
                    bwtDecodeBlock(blockList, out);

                    if (unPacker != null) {
                        if (unPacker.isInterrupted) {
                            break;
                        } else {
                            long currentTime = System.currentTimeMillis();
                            updateInfo(currentTime, lastCheckTime);
                            lastCheckTime = currentTime;
                        }
                    }
                }
            }
        }
    }

    private void decodeHuffman(OutputStream out, FileChannel fc) throws Exception {
        assert algVersion >= 2;

        ArrayList<DecodeThreadHuf> blockList = new ArrayList<>();

        ByteBuffer headBytes = ByteBuffer.allocate(16);
        // minimize re-allocation and byte copy
        ByteBuffer[] mainBuffers = new ByteBuffer[threadNum];
        for (int i = 0; i < mainBuffers.length; i++) {
            mainBuffers[i] = ByteBuffer.allocate(windowSize);
        }

        int threadChunkIndex = 0;
        while (true) {
            int read0 = fc.read(headBytes);
            if (read0 <= 0) break;  // EOF

            headBytes.flip();
            byte[] head = Util.byteBufferContentExact(headBytes);
            if (head[0] == -1) {
                // plain text
                throw new RuntimeException("Huffman block is not supposed to be plain.");
            }
            int entropyOrdinal = (head[0] & 0xff) & 0x0f;
            if (entropyOrdinal != options.getEntropyMethod().ordinal()) {
                throw new RuntimeException("Entropy method mismatch.");
            }

            int bwzBlockLen = (int) Bytes.bytesToInt32(head, 2);

            int mapLen = Bytes.bytesToInt24(head, 6);
            int origRow = Bytes.bytesToInt24(head, 9);
            long origTextCrc = Bytes.bytesToInt32(head, 12);

            ByteBuffer mapBuf = ByteBuffer.allocate(mapLen);
            if (fc.read(mapBuf) != mapLen) {
                throw new RuntimeException("Cannot read sufficient bytes");
            }
            mapBuf.flip();
            List<byte[]> hufMaps = new ArrayList<>();
            fillMaps(hufMaps, Util.byteBufferContentExact(mapBuf), mapLen, origRow);

            int mainPartLen = bwzBlockLen - mapLen - 16;
            ByteBuffer mainBuf = mainBuffers[threadChunkIndex];
            if (mainBuf.capacity() < mainPartLen) {
                mainBuf = ByteBuffer.allocate(mainPartLen);
                mainBuffers[threadChunkIndex] = mainBuf;
            }

            mainBuf.limit(mainPartLen);
            if (fc.read(mainBuf) != mainPartLen) {
                throw new RuntimeException("Cannot read sufficient bytes");
            }
            mainBuf.flip();

            blockList.add(new DecodeThreadHuf(mainBuf.array(), hufMaps, mainPartLen, origTextCrc));
            threadChunkIndex++;

            if (blockList.size() == threadNum) {
                threadChunkIndex = 0;
                bwtDecodeParallelHuffman(blockList, out);

                if (unPacker != null) {
                    if (unPacker.isInterrupted) {
                        break;
                    } else {
                        long currentTime = System.currentTimeMillis();
                        updateInfo(currentTime, lastCheckTime);
                        lastCheckTime = currentTime;
                    }
                }
            }
        }

        if (!blockList.isEmpty()) {
            // deal with last block
            bwtDecodeParallelHuffman(blockList, out);

            if (unPacker != null) {
                long currentTime = System.currentTimeMillis();
                updateInfo(currentTime, lastCheckTime);
                lastCheckTime = currentTime;
            }
        }
    }

    private void decodeAdaptiveRange(OutputStream out, FileChannel fc) throws Exception {
        List<DecodeThreadRange> blockList = new ArrayList<>();

        ByteBuffer flagFlag = ByteBuffer.allocate(1);
        ByteBuffer headBytes = ByteBuffer.allocate(11);
        ByteBuffer rngLengthBuf = ByteBuffer.allocate(4);

        // minimize re-allocation and byte copy
        ByteBuffer[] mainBuffers = new ByteBuffer[threadNum];
        for (int i = 0; i < mainBuffers.length; i++) {
            mainBuffers[i] = ByteBuffer.allocate(windowSize);
        }

        int threadChunkIndex = 0;
        while (true) {
            flagFlag.clear();
            int read0 = fc.read(flagFlag);
            if (read0 <= 0) break;  // EOF
            flagFlag.flip();

            byte methodFlag = flagFlag.get(0);
            boolean plain = methodFlag == -1;

            int headLen;
            if (plain) {
                // plain text
                headLen = 9;
            } else {
                headLen = 12;
            }
            headBytes.clear();
            headBytes.limit(headLen - 1);
            if (fc.read(headBytes) != headLen - 1)
                throw new RuntimeException("Cannot read sufficient bytes");
            headBytes.flip();

            byte[] head = Util.byteBufferContentExact(headBytes);
            int blockLength = (int) Bytes.bytesToInt32(head, 0);
            long origTextCrc = Bytes.bytesToInt32(head, head.length - 4);
//            System.out.println(blockLength);

            List<Integer> rangeEncLengths = null;
            if (!plain) {
                rangeEncLengths = new ArrayList<>();
                int nEntropyBlocks = Bytes.bytesToInt24(head, 4);
                int toRead = nEntropyBlocks * 4;
//                System.out.println(toRead);
                if (rngLengthBuf.capacity() < toRead) rngLengthBuf = ByteBuffer.allocate(toRead);
                rngLengthBuf.clear();
                rngLengthBuf.limit(toRead);
                if (fc.read(rngLengthBuf) != toRead)
                    throw new RuntimeException("Cannot read sufficient bytes");
                rngLengthBuf.flip();

                byte[] buf = rngLengthBuf.array();
                for (int i = 0; i < nEntropyBlocks; i++) {
                    rangeEncLengths.add((int) Bytes.bytesToInt32(buf, i * 4));
                }
            }

            int mainPartLen = blockLength - headLen;
            ByteBuffer mainBuf = mainBuffers[threadChunkIndex];
            if (mainBuf.capacity() < mainPartLen) {
                mainBuf = ByteBuffer.allocate(mainPartLen);
                mainBuffers[threadChunkIndex] = mainBuf;
            }
            mainBuf.clear();
            mainBuf.limit(mainPartLen);
            if (fc.read(mainBuf) != mainPartLen) {
                throw new RuntimeException("Cannot read sufficient bytes");
            }
            mainBuf.flip();
            if (plain) {
                blockList.add(
                        new DecodeThreadRange(
                                null,
                                mainPartLen,
                                methodFlag,
                                null,
                                mainBuf.array(),
                                origTextCrc));
            } else {
                int entropyOrdinal = methodFlag & 0x0f;
                if (entropyOrdinal != options.getEntropyMethod().ordinal()) {
                    throw new RuntimeException("Entropy method mismatch.");
                }

//                boolean order1Context = options.getEntropyMethod() == EntropyMethod.CONTEXT_ADAPTIVE_RANGE;
                blockList.add(
                        new DecodeThreadRange(
                                mainBuf.array(),
                                mainPartLen,
                                methodFlag,
                                rangeEncLengths,
                                null,
                                origTextCrc));
            }

            threadChunkIndex++;

            if (blockList.size() == threadNum) {
                threadChunkIndex = 0;
                bwtDecodeParallelRange(blockList, out);

                if (unPacker != null) {
                    if (unPacker.isInterrupted) {
                        break;
                    } else {
                        long currentTime = System.currentTimeMillis();
                        updateInfo(currentTime, lastCheckTime);
                        lastCheckTime = currentTime;
                    }
                }
            }
        }
        if (!blockList.isEmpty()) {
            // deal with last block
            bwtDecodeParallelRange(blockList, out);

            if (unPacker != null) {
                long currentTime = System.currentTimeMillis();
                updateInfo(currentTime, lastCheckTime);
                lastCheckTime = currentTime;
            }
        }
    }

    private void bwtDecodeParallelHuffman(List<DecodeThreadHuf> blockList, OutputStream out)
            throws InterruptedException, IOException {
        ExecutorService es = Executors.newCachedThreadPool();
        DecodeThreadHuf[] threads = blockList.toArray(new DecodeThreadHuf[0]);
        for (DecodeThreadHuf thread : threads) {
            es.execute(thread);
        }
        blockList.clear();

        es.shutdown();
        if (!es.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES))
            throw new RuntimeException("Compress thread not terminated.");  // Wait for all threads complete.

        for (DecodeThreadHuf dt : threads) {
            byte[] result = dt.getResult();
            out.write(result);
        }
    }

    private void bwtDecodeParallelRange(List<DecodeThreadRange> blockList, OutputStream out)
            throws InterruptedException, IOException {
        ExecutorService es = Executors.newCachedThreadPool();
        DecodeThreadRange[] threads = blockList.toArray(new DecodeThreadRange[0]);
        for (DecodeThreadRange thread : threads) {
            es.execute(thread);
        }
        blockList.clear();

        es.shutdown();
        if (!es.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES))
            throw new RuntimeException("Compress thread not terminated.");  // Wait for all threads complete.

        for (DecodeThreadRange dt : threads) {
            byte[] result = dt.getResult();
            out.write(result);
        }
    }

    private void bwtDecodeBlock(List<DecTextContainer> blockList, OutputStream out)
            throws InterruptedException, IOException {
        ExecutorService es = Executors.newCachedThreadPool();
        DecodeThread[] threads = new DecodeThread[blockList.size()];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new DecodeThread(blockList.get(i));
            es.execute(threads[i]);
        }
        blockList.clear();

        es.shutdown();
        if (!es.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES))
            throw new RuntimeException("Compress thread not terminated.");  // Wait for all threads complete.

        for (DecodeThread dt : threads) {
            byte[] result = dt.getResult();
            out.write(result);
        }
    }

    private void decode(OutputStream out, FileChannel fc) throws Exception {
        lastCheckTime = System.currentTimeMillis();

        if (options.getEntropyMethod() == EntropyMethod.BLOCK_HUFFMAN) {
            if (algVersion < 2) {
                decodeHuffmanOld(out, fc);
            } else {
                decodeHuffman(out, fc);
            }
        } else if (options.getEntropyMethod() == EntropyMethod.ADAPTIVE_RANGE ||
                options.getEntropyMethod() == EntropyMethod.AUTO_ADAPTIVE_RANGE) {
            decodeAdaptiveRange(out, fc);
        } else {
            throw new IllegalArgumentException("Unsupported entropy method " + options.getEntropyMethod().name());
        }

        fc.close();
    }

    /**
     * Uncompress to the output stream.
     *
     * @param outFile the target output file
     * @throws Exception if the stream is not writable.
     */
    public void uncompress(String outFile) throws Exception {
        FileOutputStream fos = new FileOutputStream(outFile);
        uncompress(fos);
        fos.flush();
        fos.close();
    }

    /**
     * Uncompress to the output stream.
     *
     * @param out the target output stream.
     * @throws Exception if the stream is not writable.
     */
    @Override
    public void uncompress(OutputStream out) throws Exception {
        try {
            decode(out, fc);
        } catch (InterruptedException | ClosedByInterruptException e) {
            // If the user interrupts the decompression process.
        } catch (Exception e) {
            fc.close();
            throw e;
        }
        fc.close();
//        fis.close();
        isRunning = false;
    }

    @Override
    public long getOutputSize() {
        return pos;
    }

    private void updateInfo(long currentTime, long lastCheckTime) {
        unPacker.progress.set(pos);
        int newUpdated = (int) (pos - lastUpdateProgress);
        lastUpdateProgress = unPacker.progress.get();
        ratio = (long) ((double) newUpdated / (currentTime - lastCheckTime) * 1.024);
    }

    private int totalLength(Collection<int[]> c) {
        int len = 0;
        for (int[] s : c) len += s.length;
        return len;
    }

    /**
     * Sets up the parent.
     *
     * @param unPacker parent {@code UnPacker} which launched this {@code BWZDeCompressor}.
     */
    @Override
    public void setUnPacker(PzUnPacker unPacker) {
        this.unPacker = unPacker;
        algVersion = unPacker.getAlgVersion();
    }

    /**
     * Deletes all temporary files.
     */
    @Override
    public void deleteCache() {
    }

    /**
     * Sets up the thread number.
     *
     * @param threadNum the thread number.
     */
    @Override
    public void setThreads(int threadNum) {
        this.threadNum = threadNum;
    }

    static class DecTextContainer {
        int[] text;
        byte[] result;

        DecTextContainer(int[] encText, byte[] result) {
            this.text = encText;
            this.result = result;
        }
    }

    /**
     * An implementation of {@code Runnable} that uncompress a single block using bwz algorithm.
     *
     * @author zbh
     * @see Runnable
     * @since 0.5
     */
    class DecodeThread implements Runnable {

        private final DecTextContainer textContainer;

        /**
         * Creates a new {@code DecodeThread} instance.
         */
        DecodeThread(DecTextContainer container) {
            this.textContainer = container;
        }

        /**
         * Starts this {@code DecodeThread}.
         */
        @Override
        public void run() {
            if (textContainer.result == null) {
                int sizeBytes = algVersion < 2 ? 3 : 4;

//            long t1 = System.currentTimeMillis();
                int[] rld = new ZeroRLCDecoder(textContainer.text, windowSize + sizeBytes + 1).Decode();
//            long t2 = System.currentTimeMillis();
                pos += rld.length / 2;
//            if (unPacker != null) unPacker.progress.set(pos);
                int[] mtf = new MTFInverse(rld).decode(257);
//            long t3 = System.currentTimeMillis();

                textContainer.result = BWTDecoder.createFromCmpText(mtf, sizeBytes).Decode();
//            long t4 = System.currentTimeMillis();
//            rldTotal += t2 - t1;
//            mtfTotal += t3 - t2;
//            bwtTotal += t4 - t3;
//        System.out.println(String.format("rld: %d, mtf: %d, bwt: %d", rldTotal, mtfTotal, bwtTotal));
                pos = pos - rld.length / 2 + textContainer.result.length;
            } else {
                // plain text, do nothing
                pos += textContainer.result.length;
            }
        }

        /**
         * Returns the text after decompression.
         *
         * @return the text after decompression
         */
        byte[] getResult() {
            return textContainer.result;
        }
    }

    abstract class ParallelDecodeThread implements Runnable {
        byte[] encodedText;
        byte[] result;
        int textRealLen;
        final long origCrc;

        ParallelDecodeThread(byte[] encodedText, int textRealLen, long origCrc) {
            this.encodedText = encodedText;
            this.textRealLen = textRealLen;
            this.origCrc = origCrc;
        }

        protected void mtfBwtInverse(int[] entropyDec) {
            int[] rld = new ZeroRLCDecoder(entropyDec, windowSize + 5).Decode();
            pos += rld.length / 2;
            int[] mtf = new MTFInverse(rld).decode(257);
            result = BWTDecoder.createFromCmpText(mtf, 4).Decode();
            pos = pos - rld.length / 2 + result.length;
        }

        /**
         * Returns the text after decompression.
         *
         * @return the text after decompression
         */
        byte[] getResult() {
            return result;
        }

        void check(int length) {
            CRC32 crc32 = new CRC32();
            crc32.update(result, 0, length);
            long res = crc32.getValue();
            if (res != origCrc) {
                System.err.printf("Block CRC check failed! Orig was: %08X, got: %08X\n", origCrc, res);
            }
        }
    }

    class DecodeThreadHuf extends ParallelDecodeThread {

        List<byte[]> huffmanMaps;

        /**
         * Creates a new {@code DecodeThread} instance.
         */
        DecodeThreadHuf(byte[] encodedText, List<byte[]> huffmanMaps, int textRealLen, long origCrc) {
            super(encodedText, textRealLen, origCrc);
            this.huffmanMaps = huffmanMaps;
        }

        /**
         * Starts this {@code DecodeThread}.
         */
        @Override
        public void run() {
            try {
                LongHuffmanDecompressorRam hufDec = new LongHuffmanDecompressorRam(encodedText,
                        textRealLen,
                        BWZCompressor.HUFFMAN_TABLE_SIZE);

                List<int[]> hufDecodedRes = new ArrayList<>();
                int totalHufDecLen = 0;
                for (byte[] map : huffmanMaps) {
                    int[] dec = hufDec.readNextCompressedBlock(map, BWZCompressor.HUFFMAN_END_SIG);
                    hufDecodedRes.add(dec);
                    totalHufDecLen += dec.length;
                }

                int[] fullText = new int[totalHufDecLen];
                int idx = 0;
                for (int[] dec : hufDecodedRes) {
                    System.arraycopy(dec, 0, fullText, idx, dec.length);
                    idx += dec.length;
                }

                mtfBwtInverse(fullText);
                check(result.length);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    class DecodeThreadRange extends ParallelDecodeThread {

        List<Integer> entropyEncLengths;
        final int flagByte;

        DecodeThreadRange(byte[] encodedText,
                          int textRealLen,
                          int flagByte,
                          List<Integer> entropyEncLengths,
                          byte[] result,
                          long origCrc) {
            super(encodedText, textRealLen, origCrc);

            this.entropyEncLengths = entropyEncLengths;
            this.result = result;
            this.flagByte = flagByte;
        }

        private int[] rangeDecodeRegular() throws IOException {
            RangeDecompressorRam rangeDecoder = new RangeDecompressorRam(
                    encodedText,
                    textRealLen
            );
            int ftClassRep = flagByte >>> 4;
            FrequencyTable ft;
            if (ftClassRep == 1) {
                ft = new FixedRangeFrequencyTable(BWZCompressor.HUFFMAN_TABLE_SIZE, BWZCompressor.HUFFMAN_END_SIG, 4);
            } else if (ftClassRep == 2) {
                ft = new AdaptiveOrder1FrequencyTable(BWZCompressor.HUFFMAN_TABLE_SIZE, BWZCompressor.HUFFMAN_END_SIG);
            } else if (ftClassRep >= 3 && ftClassRep < 8) {
                int detailClass = ftClassRep - 3;
                if (detailClass == 0) {
                    ft = AdaptiveCustomRangeFrequencyTable.createTypical(BWZCompressor.HUFFMAN_TABLE_SIZE, BWZCompressor.HUFFMAN_END_SIG);
                } else if (detailClass == 1) {
                    ft = AdaptiveCustomRangeFrequencyTable.createExpRanged(BWZCompressor.HUFFMAN_TABLE_SIZE, BWZCompressor.HUFFMAN_END_SIG);
                } else {
                    throw new RuntimeException("Cannot interpret range frequency table " + ftClassRep);
                }
            } else if (ftClassRep == 8) {
                ft = new FixedRangeOrder2FrequencyTable(BWZCompressor.HUFFMAN_TABLE_SIZE, BWZCompressor.HUFFMAN_END_SIG, 256);
            } else {
                ft = new AdaptiveFrequencyTable(BWZCompressor.HUFFMAN_TABLE_SIZE);
            }
            rangeDecoder.setFrequencyTable(ft);
            
            List<int[]> rangeDecChunks = new ArrayList<>();
            int totalDecLen = 0;
            for (int encLen : entropyEncLengths) {
                int[] rngDec = rangeDecoder.readNextCompressedBlock(encLen, BWZCompressor.HUFFMAN_END_SIG);
                totalDecLen += rngDec.length;
                rangeDecChunks.add(rngDec);
                ft.reset();  // reset for next iteration
            }
            int[] rngRes = new int[totalDecLen];
            int index = 0;
            for (int[] rngDec : rangeDecChunks) {
                System.arraycopy(rngDec, 0, rngRes, index, rngDec.length);
                index += rngDec.length;
            }
            return rngRes;
        }

        @Override
        public void run() {
            if (result != null) {
                pos += textRealLen;
                check(textRealLen);
                return;
            }

            try {
                int[] rangeDec = rangeDecodeRegular();
                mtfBwtInverse(rangeDec);
                check(result.length);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
