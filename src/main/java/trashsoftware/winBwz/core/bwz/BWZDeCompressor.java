package trashsoftware.winBwz.core.bwz;

import trashsoftware.winBwz.core.DeCompressor;
import trashsoftware.winBwz.core.bwz.bwt.BWTDecoder;
import trashsoftware.winBwz.core.options.BWZOptions;
import trashsoftware.winBwz.core.options.EntropyMethod;
import trashsoftware.winBwz.huffman.MapCompressor.BwzMapDeCompressor;
import trashsoftware.winBwz.longHuffman.LongHuffmanDecompressorRam;
import trashsoftware.winBwz.longHuffman.LongHuffmanInputStream;
import trashsoftware.winBwz.packer.pz.PzUnPacker;
import trashsoftware.winBwz.rangeCodec.AdaptiveFrequencyTable;
import trashsoftware.winBwz.rangeCodec.FrequencyTable;
import trashsoftware.winBwz.rangeCodec.LongRangeInputStream;
import trashsoftware.winBwz.utility.Bytes;
import trashsoftware.winBwz.utility.Util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
        byte[] maps = new BWTDecoder(mftMap, origRow).Decode();

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

        ArrayList<DecodeThreadNewHuf> blockList = new ArrayList<>();

        ByteBuffer headBytes = ByteBuffer.allocate(12);
        ByteBuffer mainBuf = ByteBuffer.allocate(windowSize);
        
        byte[][] mainBuffers = new byte[threadNum][windowSize];

        int threadHufIndex = 0;
        while (true) {
            int read0 = fc.read(headBytes);
            if (read0 <= 0) break;  // EOF

            headBytes.flip();
            byte[] head = Util.byteBufferContent(headBytes);
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

            ByteBuffer mapBuf = ByteBuffer.allocate(mapLen);
            if (fc.read(mapBuf) != mapLen) {
                throw new RuntimeException("Cannot read sufficient bytes");
            }
            mapBuf.flip();
            List<byte[]> hufMaps = new ArrayList<>();
            fillMaps(hufMaps, Util.byteBufferContent(mapBuf), mapLen, origRow);

            int mainPartLen = bwzBlockLen - mapLen - 12;
            mainBuf.clear();
            if (mainBuf.capacity() < mainPartLen) {
                mainBuf = ByteBuffer.allocate(mainPartLen);
            }
            
            mainBuf.limit(mainPartLen);
            if (fc.read(mainBuf) != mainPartLen) {
                throw new RuntimeException("Cannot read sufficient bytes");
            }
            mainBuf.flip();
            
            if (mainBuffers[threadHufIndex].length < mainPartLen) {
                // for safety
                mainBuffers[threadHufIndex] = new byte[mainPartLen];
            }
            byte[] mainBuffer = mainBuffers[threadHufIndex];
            
            System.arraycopy(mainBuf.array(), 0, mainBuffer, 0, mainPartLen);
            blockList.add(new DecodeThreadNewHuf(mainBuffer, hufMaps, mainPartLen));
            threadHufIndex++;

            if (blockList.size() == threadNum) {
                threadHufIndex = 0;
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
        ArrayList<DecTextContainer> blockList = new ArrayList<>();

        byte[] flagFlag;
        byte[] sizeBytes;
        byte[] blockLenBytes;
        ArrayDeque<Integer> rngBlockLengths = new ArrayDeque<>();

        LongRangeInputStream ris = new LongRangeInputStream(fc, windowSize);

//        int indexFromBegin = 0;

        while (true) {
//            System.out.println("Reading next block");
            flagFlag = ris.readPlain(1);
            if (flagFlag == null) {
                break;  // Reach the end of the stream.
            }
            if (flagFlag[0] == -1) {
                // no compression
                sizeBytes = ris.readPlain(4);
                int plainLength = Bytes.bytesToInt32(sizeBytes);
//                System.out.println(plainLength);
                byte[] plainText = ris.readPlain(plainLength);
                blockList.add(new DecTextContainer(null, plainText));
            } else {
                int entropyOrdinal = (flagFlag[0] & 0xff) & 0x0f;

                if (entropyOrdinal != options.getEntropyMethod().ordinal()) {
                    throw new RuntimeException("Entropy method mismatch. Got byte " + flagFlag[0]);
                }
                sizeBytes = ris.readPlain(7);
//                if (sizeBytes == null) {
//                    break;  // Reach the end of the stream.
//                }
                int bwzBlockLen = (int) Bytes.bytesToInt32(sizeBytes, 0);
                
                // todo: parallel range decoding

                int nEntropyBlocks = Bytes.bytesToInt24(sizeBytes, 4);
                for (int i = 0; i < nEntropyBlocks; i++) {
                    blockLenBytes = ris.readPlain(4);
                    int rngEncLen = (int) Bytes.bytesToInt32(blockLenBytes, 0);
                    rngBlockLengths.addLast(rngEncLen);
                }

                List<int[]> blockRngDecList = new ArrayList<>();
                while (!rngBlockLengths.isEmpty()) {
                    FrequencyTable ft = new AdaptiveFrequencyTable(BWZCompressor.HUFFMAN_TABLE_SIZE);
                    ris.setFrequencyTable(ft);
                    int rngEncLen = rngBlockLengths.removeFirst();
//                System.out.println(indexFromBegin + " " + rngEncLen);
                    int[] nextDec = ris.readNextCompressedBlock(rngEncLen, BWZCompressor.HUFFMAN_END_SIG);
                    blockRngDecList.add(nextDec);
//                indexFromBegin++;
                }

                int blockDecLen = totalLength(blockRngDecList);
                int[] blockRngDec = new int[blockDecLen];
                int index = 0;
                for (int[] part : blockRngDecList) {
                    System.arraycopy(part, 0, blockRngDec, index, part.length);
                    index += part.length;
                }

                blockList.add(new DecTextContainer(blockRngDec, null));
            }

            // got enough blocks for multi-threading
            if (blockList.size() == threadNum) {
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

        if (!blockList.isEmpty()) {
            // deal with last block
            bwtDecodeBlock(blockList, out);

            if (unPacker != null) {
                long currentTime = System.currentTimeMillis();
                updateInfo(currentTime, lastCheckTime);
                lastCheckTime = currentTime;
            }
        }
    }

    private void bwtDecodeParallelHuffman(List<DecodeThreadNewHuf> blockList, OutputStream out)
            throws InterruptedException, IOException {
        ExecutorService es = Executors.newCachedThreadPool();
        DecodeThreadNewHuf[] threads = blockList.toArray(new DecodeThreadNewHuf[0]);
        for (DecodeThreadNewHuf thread : threads) {
            es.execute(thread);
        }
        blockList.clear();

        es.shutdown();
        if (!es.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES))
            throw new RuntimeException("Compress thread not terminated.");  // Wait for all threads complete.

        for (DecodeThreadNewHuf dt : threads) {
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
        } else if (options.getEntropyMethod() == EntropyMethod.ADAPTIVE_RANGE) {
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

//            long t1 = System.currentTimeMillis();
                int[] rld = new ZeroRLCDecoder(textContainer.text, windowSize + 4).Decode();
//            long t2 = System.currentTimeMillis();
                pos += rld.length / 2;
//            if (unPacker != null) unPacker.progress.set(pos);
                int[] mtf = new MTFInverse(rld).decode(257);
//            long t3 = System.currentTimeMillis();
                textContainer.result = new BWTDecoder(mtf).Decode();
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

    class DecodeThreadNewHuf implements Runnable {

        byte[] encodedText;
        List<byte[]> huffmanMaps;
        byte[] result;
        int textRealLen;

        /**
         * Creates a new {@code DecodeThread} instance.
         */
        DecodeThreadNewHuf(byte[] encodedText, List<byte[]> huffmanMaps, int textRealLen) {
            this.encodedText = encodedText;
            this.huffmanMaps = huffmanMaps;
            this.textRealLen = textRealLen;
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
                
                int[] rld = new ZeroRLCDecoder(fullText, windowSize + 4).Decode();
                pos += rld.length / 2;
                int[] mtf = new MTFInverse(rld).decode(257);
                result = new BWTDecoder(mtf).Decode();
                pos = pos - rld.length / 2 + result.length;

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Returns the text after decompression.
         *
         * @return the text after decompression
         */
        byte[] getResult() {
            return result;
        }
    }
}
