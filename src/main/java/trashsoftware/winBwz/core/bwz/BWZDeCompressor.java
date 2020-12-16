package trashsoftware.winBwz.core.bwz;

import trashsoftware.winBwz.core.DeCompressor;
import trashsoftware.winBwz.core.bwz.bwt.BWTDecoder;
import trashsoftware.winBwz.huffman.MapCompressor.BwzMapDeCompressor;
import trashsoftware.winBwz.longHuffman.LongHuffmanInputStream;
import trashsoftware.winBwz.packer.PzUnPacker;
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

    private final int huffmanBlockMaxSize;
    private final int windowSize;

    private final FileChannel fc;
    //    private InputStream fis;
    private final LinkedList<byte[]> huffmanMaps = new LinkedList<>();
    PzUnPacker unPacker;
    boolean isRunning = true;
    long ratio, pos;
    private int threadNum = 1;  // Default thread number.
    private long lastUpdateProgress;
//    long initBytePos;

    /**
     * Constructs a new {@code BWZDeCompressor} instance.
     *
     * @param inFile     the name or path of the file to be uncompressed
     * @param windowSize the size of each block
     * @throws IOException if the file is not readable
     */
    public BWZDeCompressor(String inFile, int windowSize, long startPos) throws IOException {
        this.windowSize = windowSize;

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

//    private static long hufTotal;

    private void decode(OutputStream out, FileChannel fc) throws Exception {
        long lastCheckTime = System.currentTimeMillis();
        long currentTime;

        int huffmanBlockForEachWindow;
        if (windowSize <= huffmanBlockMaxSize) huffmanBlockForEachWindow = 1;
        else huffmanBlockForEachWindow = windowSize / huffmanBlockMaxSize;

        ArrayList<int[]> blockList = new ArrayList<>();
        ArrayList<int[]> huffmanBlockList = new ArrayList<>();

        LongHuffmanInputStream his =
                new LongHuffmanInputStream(fc, BWZCompressor.HUFFMAN_TABLE_SIZE, windowSize);
        int[] huffmanResult;
        byte[] headBytes;
        byte[] blockBytes;

        while (true) {
            if (huffmanMaps.isEmpty()) {
                headBytes = his.read(6);
                if (headBytes == null) break;  // Reach the end of the stream.

                int mapLen = Bytes.bytesToInt24(headBytes, 0);
                int origRow = Bytes.bytesToInt24(headBytes, 3);
//                System.out.format("%d %d %d\n", flagLen, mapLen, origRow);
                blockBytes = his.read(mapLen);
                if (blockBytes == null) {
                    throw new RuntimeException("Cannot read block");
                }

                fillMaps(blockBytes, mapLen, origRow);
            }
            byte[] map = huffmanMaps.removeFirst();
//            System.out.println(Arrays.toString(map));
//            long t1 = System.currentTimeMillis();
            huffmanResult = his.read(map, BWZCompressor.HUFFMAN_END_SIG);
//            hufTotal += System.currentTimeMillis() - t1;
//            System.out.println("huf :" + hufTotal);

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

                    if (unPacker != null) {
                        if (unPacker.isInterrupted) {
                            break;
                        } else {
                            currentTime = System.currentTimeMillis();
                            updateInfo(currentTime, lastCheckTime);
                            lastCheckTime = currentTime;
                        }
                    }
                }
            }
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
        Timer timer = null;
        if (unPacker != null) {
            timer = new Timer();
            timer.scheduleAtFixedRate(new DTimer(), 0, 1000);
        }

        try {
            decode(out, fc);
        } catch (InterruptedException | ClosedByInterruptException e) {
            // If the user interrupts the decompression process.
        } catch (Exception e) {
            fc.close();
            throw e;
        } finally {
            if (timer != null) timer.cancel();
        }
        fc.close();
//        fis.close();
        isRunning = false;
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
            if (unPacker != null) unPacker.progress.set(pos);
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


    /**
     * An implementation of {@code Runnable}, used to update status of a {@code BWZDeCompressor} instance to a
     * {@code UnPacker} instance every 1 second.
     *
     * @author zbh
     * @see Runnable
     * @since 0.5
     */
    class DTimer extends TimerTask {

        private int timeUsed;

        /**
         * Creates a new {@code Timer} instance.
         */
        DTimer() {
        }

        /**
         * Runs this {@code DTimer}.
         */
        @Override
        public void run() {
            unPacker.timeUsed.setValue(Util.secondToString(timeUsed));

            if (pos != 0) {
                TimerHelper.updateBwzProgress(
                        pos,
                        unPacker.getTotalOrigSize(),
                        unPacker.percentage,
                        unPacker.ratio,
                        ratio,
                        unPacker.timeExpected,
                        unPacker.passedLength);
            }
            timeUsed += 1;
        }
    }
}
