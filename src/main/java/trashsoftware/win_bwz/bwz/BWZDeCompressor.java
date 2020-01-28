package trashsoftware.win_bwz.bwz;

import trashsoftware.win_bwz.bwz.bwt.BWTDecoder;
import trashsoftware.win_bwz.bwz.bwt.BWTDecoderByte;
import trashsoftware.win_bwz.huffman.MapCompressor.MapDeCompressor;
import trashsoftware.win_bwz.interfaces.DeCompressor;
import trashsoftware.win_bwz.longHuffman.LongHuffmanInputStream;
//import trashsoftware.win_bwz.LongHuffman.LongHuffmanInputStreamBit;
import trashsoftware.win_bwz.packer.UnPacker;
import trashsoftware.win_bwz.utility.Bytes;
import trashsoftware.win_bwz.utility.Util;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
//import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * BWZ-algorithm decompressor, implements the {@code DeCompressor} interface.
 *
 * @author zbh
 * @see trashsoftware.win_bwz.interfaces.DeCompressor
 * @since 0.5
 */
public class BWZDeCompressor implements DeCompressor {

    private int huffmanBlockMaxSize, windowSize;

    private FileChannel fc;
    //    private InputStream fis;
    private LinkedList<byte[]> huffmanMaps = new LinkedList<>();
    private int threadNum = 1;  // Default thread number.
    UnPacker parent;
    private long lastUpdateProgress;
    boolean isRunning = true;
    long ratio, pos;
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

//        initBytePos = startPos;
        fc = new FileInputStream(inFile).getChannel();
//        fis = new FileInputStream(inFile);
//        if (fis.skip(startPos) != startPos) throw new IOException("Error occurs while reading");
        fc.position(startPos);
//        byte[] buffer1 = new byte[1];
        ByteBuffer buffer = ByteBuffer.allocate(1);

        int r;
        r = fc.read(buffer);
//        r = fis.read(buffer1);

        if (r != 1) throw new IOException("Error occurs during reading");
        huffmanBlockMaxSize = (int) Math.pow(2, buffer.get(0));
//        System.out.println(huffmanBlockMaxSize);
    }

    private void fillMaps(byte[] block, int flagLen, int mapLen, int origRow) {
        byte[] cmpFlags = new byte[flagLen];
        byte[] cmpMap = new byte[mapLen];
        System.arraycopy(block, 0, cmpFlags, 0, flagLen);
        System.arraycopy(block, flagLen, cmpMap, 0, mapLen);

        ZeroRLCDecoderByte rld = new ZeroRLCDecoderByte(cmpFlags);
        byte[] rldFlags = rld.Decode();
        byte[] flags = new MTFInverseByte(rldFlags).Inverse(256);

        byte[] uncMap = new MapDeCompressor(cmpMap).
                Uncompress((windowSize / huffmanBlockMaxSize + 1) * 259, false);
        byte[] rldMap = new ZeroRLCDecoderByte(uncMap).Decode();
        byte[] mtfMap = new MTFInverseByte(rldMap).Inverse(18);
        byte[] maps = new BWTDecoderByte(mtfMap, origRow).Decode();

        int fIndex = 0;
        int i = 0;
        while (fIndex < flags.length) {
            int flag = flags[fIndex++] & 0xff;
            byte[] map = new byte[BWZCompressor.huffmanTableSize];
            if (flag == 0) {
                System.arraycopy(maps, i, map, 0, BWZCompressor.huffmanTableSize);
                i += BWZCompressor.huffmanTableSize;
            } else {
                int x = huffmanMaps.size() - flag;
                System.arraycopy(huffmanMaps.get(x), 0, map, 0, BWZCompressor.huffmanTableSize);
            }
            huffmanMaps.addLast(map);
        }
    }

    private static long hufTotal;

    private void decode(OutputStream out, FileChannel fc) throws Exception {
        long lastCheckTime = System.currentTimeMillis();
        long currentTime;

        int huffmanBlockForEachWindow;
        if (windowSize <= huffmanBlockMaxSize) huffmanBlockForEachWindow = 1;
        else huffmanBlockForEachWindow = windowSize / huffmanBlockMaxSize;

        ArrayList<int[]> blockList = new ArrayList<>();
        ArrayList<int[]> huffmanBlockList = new ArrayList<>();

        LongHuffmanInputStream his =
                new LongHuffmanInputStream(fc, BWZCompressor.huffmanTableSize, huffmanBlockMaxSize + 256);
//        LongHuffmanInputStreamBit his2 =
//                new LongHuffmanInputStreamBit(fis, initBytePos + 1, BWZCompressor.huffmanTableSize, huffmanBlockMaxSize + 256);
//        his.pushCompressedBitLength(8);  // Skip the first byte
        int[] huffmanResult;
        byte[] headBytes;
        byte[] blockBytes;
        byte[] flagLenBytes = new byte[2];
        byte[] mapLenBytes = new byte[3];
        byte[] origRowBytes = new byte[3];

        while (true) {
            if (huffmanMaps.isEmpty()) {
                headBytes = his.read(8);
//                if (!Arrays.equals(headBytes, his.read(8))) System.out.println(111);
                if (headBytes == null) break;  // Reach the end of the stream.

                System.arraycopy(headBytes, 0, flagLenBytes, 0, 2);
                System.arraycopy(headBytes, 2, mapLenBytes, 0, 3);
                System.arraycopy(headBytes, 5, origRowBytes, 0, 3);
                int flagLen = Bytes.bytesToShort(flagLenBytes);
                int mapLen = Bytes.bytesToInt24(mapLenBytes);
                int origRow = Bytes.bytesToInt24(origRowBytes);
//                System.out.format("%d %d %d\n", flagLen, mapLen, origRow);
                blockBytes = his.read(mapLen + flagLen);
//                byte[] bb2;
//                if (!Arrays.equals(blockBytes, bb2 = his.read(mapLen + flagLen))) {
//                    System.out.println(222);
//                }
//                System.out.println(Arrays.toString(Util.byteArrayToStringArray(blockBytes)) +
//                        "\n" + Arrays.toString(Util.byteArrayToStringArray(bb2)));

                fillMaps(blockBytes, flagLen, mapLen, origRow);
            }
            byte[] map = huffmanMaps.removeFirst();
            long t1 = System.currentTimeMillis();
            huffmanResult = his.read(map, BWZCompressor.huffmanEndSig);
            hufTotal += System.currentTimeMillis() - t1;
            System.out.println("huf :" + hufTotal);
//            System.out.println(Arrays.toString(huffmanResult));
//            short[] hr2 = his.read(map, BWZCompressor.huffmanEndSig);
////            System.out.println(Arrays.toString(hr2));
//            if (!Arrays.equals(huffmanResult, hr2)) {
//                System.out.println(Arrays.toString(huffmanResult));
//                System.out.println(Arrays.toString(hr2));
//            }

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
                        threads[i] = new DecodeThread(blockList.get(i), windowSize, this);
                        es.execute(threads[i]);
                    }
                    blockList.clear();

                    es.shutdown();
                    es.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);  // Wait for all threads terminate.

                    for (DecodeThread dt : threads) {
                        byte[] result = dt.getResult();
                        out.write(result);
                    }

                    if (parent != null) {
                        if (parent.isInterrupted) {
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
     * @param out the target output stream.
     * @throws Exception if the stream is not writable.
     */
    @Override
    public void uncompress(OutputStream out) throws Exception {
        if (parent != null) {
            Thread timer = new Thread(new DTimer(parent, this));
            timer.start();
        }

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

    private void updateInfo(long currentTime, long lastCheckTime) {
        parent.progress.set(pos);
        int newUpdated = (int) (pos - lastUpdateProgress);
        lastUpdateProgress = parent.progress.get();
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
     * @param parent parent {@code UnPacker} which launched this {@code BWZDeCompressor}.
     */
    @Override
    public void setParent(UnPacker parent) {
        this.parent = parent;
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
}


/**
 * An implementation of {@code Runnable} that uncompress a single block using bwz algorithm.
 *
 * @author zbh
 * @see java.lang.Runnable
 * @since 0.5
 */
class DecodeThread implements Runnable {

    private int[] text;
    private byte[] result;
    private int windowSize;
    private BWZDeCompressor parent;

    /**
     * Creates a new {@code DecodeThread} instance.
     *
     * @param text       the text to be decode
     * @param windowSize the maximum size of the block
     * @param parent     the parent {@code BWZDeCompressor} which has launched this {@code DecodeThread}.
     */
    DecodeThread(int[] text, int windowSize, BWZDeCompressor parent) {
        this.text = text;
        this.windowSize = windowSize;
        this.parent = parent;
    }

    private static long rldTotal, mtfTotal, bwtTotal;

    /**
     * Starts this {@code DecodeThread}.
     */
    @Override
    public void run() {
        long t1 = System.currentTimeMillis();
        int[] rld = new ZeroRLCDecoder(text, windowSize + 4).Decode();
        long t2 = System.currentTimeMillis();
        parent.pos += rld.length / 2;
        if (parent.parent != null) parent.parent.progress.set(parent.pos);
        int[] mtf = new MTFInverse(rld).decode();
        long t3 = System.currentTimeMillis();
        result = new BWTDecoder(mtf).Decode();
        long t4 = System.currentTimeMillis();
        rldTotal += t2 - t1;
        mtfTotal += t3 - t2;
        bwtTotal += t4 - t3;
        System.out.println(String.format("rld: %d, mtf: %d, bwt: %d", rldTotal, mtfTotal, bwtTotal));
        parent.pos = parent.pos - rld.length / 2 + result.length;
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
 * @see java.lang.Runnable
 * @since 0.5
 */
class DTimer implements Runnable {

    private UnPacker unPacker;
    private BWZDeCompressor dec;
    private int timeUsed;

    /**
     * Creates a new {@code Timer} instance.
     *
     * @param unPacker the parent {@code UnPacker} of <code>dec</code>
     * @param dec      the {@code BWZDeCompressor} which created this {@code DTimer}.
     */
    DTimer(UnPacker unPacker, BWZDeCompressor dec) {
        this.unPacker = unPacker;
        this.dec = dec;
    }

    /**
     * Runs this {@code DTimer}.
     */
    @Override
    public void run() {
        while (dec.isRunning) {
            unPacker.timeUsed.setValue(Util.secondToString(timeUsed));

            if (dec.pos != 0) {
                double finished = (double) dec.pos / unPacker.getTotalOrigSize();
                double rounded = (double) Math.round(finished * 1000) / 10;
                unPacker.percentage.set(String.valueOf(rounded));

                unPacker.ratio.set(String.valueOf(dec.ratio));

                if (dec.ratio != 0) {
                    long expectTime = (unPacker.getTotalOrigSize() - dec.pos) / dec.ratio / 1024;
                    unPacker.timeExpected.set(Util.secondToString(expectTime));
                }

                unPacker.passedLength.set(Util.sizeToReadable(dec.pos));
            }
            timeUsed += 1;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
    }
}
