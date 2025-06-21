package trashsoftware.winBwz.core.bwz;

import trashsoftware.winBwz.core.DeCompressor;
import trashsoftware.winBwz.core.bwz.bwt.BWTDecoder;
import trashsoftware.winBwz.core.options.BWZOptions;
import trashsoftware.winBwz.huffman.MapCompressor.BwzMapDeCompressor;
import trashsoftware.winBwz.longHuffman.LongHuffmanInputStream;
import trashsoftware.winBwz.packer.pz.PzUnPacker;
import trashsoftware.winBwz.rangeCodec.AdaptiveFrequencyTable;
import trashsoftware.winBwz.rangeCodec.FrequencyTable;
import trashsoftware.winBwz.rangeCodec.LongRangeInputStream;
import trashsoftware.winBwz.utility.Bytes;

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

    private final int huffmanBlockMaxSize;
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
        huffmanBlockMaxSize = (int) Math.pow(2, buffer.get(0));
    }

    private void fillMaps(byte[] block, int mapLen, int origRow) throws IOException {
        byte[] cmpMap = new byte[mapLen];
        System.arraycopy(block, 0, cmpMap, 0, mapLen);

        int maxMapsLen = (windowSize / huffmanBlockMaxSize + 1) * 259;
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
            huffmanMaps.addLast(map);
        }
    }

    private void decodeHuffman(OutputStream out, FileChannel fc) throws Exception {
        int huffmanBlockForEachWindow;
        if (windowSize <= huffmanBlockMaxSize) huffmanBlockForEachWindow = 1;
        else huffmanBlockForEachWindow = windowSize / huffmanBlockMaxSize;

        ArrayList<int[]> blockList = new ArrayList<>();
        ArrayList<int[]> huffmanBlockList = new ArrayList<>();

        LongHuffmanInputStream his =
                new LongHuffmanInputStream(fc, BWZCompressor.HUFFMAN_TABLE_SIZE, windowSize);
        int[] huffmanResult;
        byte[] flagBytes;
        byte[] headBytes;
        byte[] blockBytes;

        while (true) {
            if (huffmanMaps.isEmpty()) {
                if (algVersion >= 2) {
                    flagBytes = his.readPlain(2);
                    if (flagBytes == null) break;  // Reach the end of the stream.

                    int entropyOrdinal = (flagBytes[0] & 0xff) & 0x0f;
                    if (entropyOrdinal != options.getEntropyMethod().ordinal()) {
                        throw new RuntimeException("Entropy method mismatch.");
                    }
                }

                headBytes = his.readPlain(6);
                if (headBytes == null) break;  // Reach the end of the stream.

                int mapLen = Bytes.bytesToInt24(headBytes, 0);
                int origRow = Bytes.bytesToInt24(headBytes, 3);
//                System.out.format("%d %d %d\n", flagLen, mapLen, origRow);
                blockBytes = his.readPlain(mapLen);
                if (blockBytes == null) {
                    throw new RuntimeException("Cannot read block");
                }

                fillMaps(blockBytes, mapLen, origRow);
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

                blockList.add(concatenateHuffman);
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

    private void decodeAdaptiveRange(OutputStream out, FileChannel fc) throws Exception {
        ArrayList<int[]> blockList = new ArrayList<>();
        
        byte[] flagBytes;

        LongRangeInputStream ris = new LongRangeInputStream(fc, windowSize);
        
        while (true) {
            FrequencyTable ft = new AdaptiveFrequencyTable(BWZCompressor.HUFFMAN_TABLE_SIZE);
            ris.setFrequencyTable(ft);
//            System.out.println("Read next block");
            
            flagBytes = ris.readPlain(6);
            if (flagBytes == null) {
                break;  // Reach the end of the stream.
            }
            int entropyOrdinal = (flagBytes[0] & 0xff) & 0x0f;
            if (entropyOrdinal != options.getEntropyMethod().ordinal()) {
                throw new RuntimeException("Entropy method mismatch. Got byte " + flagBytes[0]);
            }
            int rngEncLen = (int) Bytes.bytesToInt32(flagBytes, 2);
            
            int[] nextDec = ris.readNextCompressedBlock(rngEncLen, BWZCompressor.HUFFMAN_END_SIG);
//            System.out.println("Dec len: " + nextDec.length);
            blockList.add(nextDec);

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

    private void bwtDecodeBlock(List<int[]> blockList, OutputStream out)
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

        if (options.getEntropyMethod() == BWZOptions.EntropyMethod.BLOCK_HUFFMAN) {
            decodeHuffman(out, fc);
        } else if (options.getEntropyMethod() == BWZOptions.EntropyMethod.ADAPTIVE_RANGE) {
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

    /**
     * An implementation of {@code Runnable} that uncompress a single block using bwz algorithm.
     *
     * @author zbh
     * @see Runnable
     * @since 0.5
     */
    class DecodeThread implements Runnable {

        //        private static long rldTotal, mtfTotal, bwtTotal;
        private final int[] text;
        private byte[] result;

        /**
         * Creates a new {@code DecodeThread} instance.
         *
         * @param text       the text to be decode
         */
        DecodeThread(int[] text) {
            this.text = text;
        }

        /**
         * Starts this {@code DecodeThread}.
         */
        @Override
        public void run() {
//            long t1 = System.currentTimeMillis();
            int[] rld = new ZeroRLCDecoder(text, windowSize + 4).Decode();
//            long t2 = System.currentTimeMillis();
            pos += rld.length / 2;
//            if (unPacker != null) unPacker.progress.set(pos);
            int[] mtf = new MTFInverse(rld).decode(257);
//            long t3 = System.currentTimeMillis();
            result = new BWTDecoder(mtf).Decode();
//            long t4 = System.currentTimeMillis();
//            rldTotal += t2 - t1;
//            mtfTotal += t3 - t2;
//            bwtTotal += t4 - t3;
//        System.out.println(String.format("rld: %d, mtf: %d, bwt: %d", rldTotal, mtfTotal, bwtTotal));
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
    }
}
