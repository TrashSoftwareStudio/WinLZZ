/*
 * BWZCompressor
 * Implemented by Bohan Zhang @Trash Software Studio, 2018
 *
 * This algorithm is implemented by using Burrows-Wheeler Transform, Move-To-Front Transform,
 * Run-Length Coding and Huffman Coding.
 * A suffix array is presented in case to build the BWT string.
 */

package trashsoftware.win_bwz.bwz;

import javafx.beans.property.ReadOnlyStringWrapper;
import trashsoftware.win_bwz.bwz.bwt.BWTEncoder;
import trashsoftware.win_bwz.bwz.bwt.BWTEncoderByte;
import trashsoftware.win_bwz.huffman.MapCompressor.MapCompressor;
import trashsoftware.win_bwz.interfaces.Compressor;
import trashsoftware.win_bwz.longHuffman.LongHuffmanCompressorRam;
import trashsoftware.win_bwz.packer.Packer;
import trashsoftware.win_bwz.utility.Bytes;
import trashsoftware.win_bwz.utility.Util;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * BWZ-algorithm implementation of {@code Compressor} interface.
 *
 * @author zbh
 * @see Compressor
 * @since 0.5
 */
public class BWZCompressor implements Compressor {

    /**
     * Default maximum size of a huffman-coding block.
     * <p>
     * This value often changed by {@code setCompressionLevel()}.
     */
    int maxHuffmanSize = 16384;

    /**
     * Decision size of suffix array building algorithm.
     * DC3 algorithm is used if the window size if greater than or equal to this size in case of increasing speed.
     * Otherwise, doubling algorithm is used (since doubling algorithm takes O(n * log n) while dc3 takes O(n),
     * But doubling algorithm has a much smaller constant).
     */
    final static int DC_3_DECISION_SIZE = 1048576;

    /**
     * The alphabet size of the main huffman table.
     */
    final static int HUFFMAN_TABLE_SIZE = 259;

    /**
     * The signal that marks the end of a huffman stream.
     */
    public final static int HUFFMAN_END_SIG = 258;

    private OutputStream out;
    private FileChannel fis;
    private InputStream sis;

    /**
     * Total size after compression.
     */
    private long cmpSize;

    /**
     * Length of the main part.
     */
    long mainLen;

    private int windowSize;

    /**
     * The parent {@code Packer} which launched this {@code BWZCompressor}.
     */
    Packer parent;

    private long lastUpdateProgress;

    private int threadNumber = 1;

    /**
     * Whether the compression is in progress.
     */
    boolean isRunning = true;

    long ratio;
    long pos;

    /**
     * Creates a new instance of {@code BWZCompressor}.
     * <p>
     * This constructor takes a file as input.
     *
     * @param inFile     name/path of file to compress.
     * @param windowSize the block size.
     * @throws IOException if the input file is not readable.
     */
    public BWZCompressor(String inFile, int windowSize) throws IOException {
        this.windowSize = windowSize;
        this.fis = new FileInputStream(inFile).getChannel();
    }

    /**
     * Creates a new {@code BWZCompressor} instance.
     * <p>
     * This constructor takes a {@code InputStream} as input.
     *
     * @param in         the input stream
     * @param windowSize the block size
     */
    public BWZCompressor(InputStream in, int windowSize) {
        this.windowSize = windowSize;
        this.sis = in;
    }

    private void compress() throws Exception {
        long lastCheckTime = System.currentTimeMillis();
        long currentTime;

        int read;
        byte[] block = new byte[windowSize * threadNumber];
        while ((read = sis.read(block)) > 0) {

            compressOneLoop(read, block);

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

    private void compress2() throws Exception {
        long lastCheckTime = System.currentTimeMillis();
        long currentTime;

        int read;
        ByteBuffer block = ByteBuffer.allocate(windowSize * threadNumber);
        while ((read = fis.read(block)) > 0) {
            block.flip();
            byte[] buffer = block.array();

            compressOneLoop(read, buffer);
            block.clear();

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

    private void compressOneLoop(int read, byte[] buffer) throws Exception {
        int threadsNeed;
        if (read % windowSize == 0) threadsNeed = read / windowSize;
        else threadsNeed = read / windowSize + 1;

        EncodeThread[] threads = new EncodeThread[threadsNeed];
        ExecutorService es = Executors.newCachedThreadPool();
        int begin = 0;
        for (int i = 0; i < threads.length; i++) {  // sets up threads
            int end = Math.min(begin + windowSize, read);
            int partSize = end - begin;
            boolean usdDc3 = partSize >= BWZCompressor.DC_3_DECISION_SIZE;

//            if (!usdDc3) SuffixArrayDoubling.allocateArraysIfNot(windowSize, threadNumber);

            EncodeThread et = new EncodeThread(
                    buffer, begin, partSize, windowSize, this, i, usdDc3
            );
            threads[i] = et;

            begin += windowSize;
        }

        for (EncodeThread et : threads) {
            es.execute(et);
        }

        es.shutdown();
        es.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);  // Wait for all threads complete.

        for (EncodeThread et : threads) {
            et.writeCompressedMap(out);
            et.writeTo(out);
        }
    }

    /**
     * Compresses to the output stream.
     *
     * @param out the target output stream.
     * @throws Exception if the output stream is not writable.
     */
    @Override
    public void compress(OutputStream out) throws Exception {
        this.out = out;
        this.out.write(Util.windowSizeToByte(maxHuffmanSize));

        Thread timer;
        if (parent != null) {
            timer = new Thread(new Timer(parent, this));
            timer.start();
        }
        try {
            if (sis == null) compress2();
            else compress();
        } catch (InterruptedException e) {
            // The process is interrupted by user.
        }
        if (sis == null) fis.close();
        else sis.close();

        if (parent != null && parent.isInterrupted) {
            isRunning = false;
            return;
        }

        cmpSize = mainLen + 1;
        isRunning = false;
    }

    private void updateInfo(long currentTime, long lastUpdateTime) {
        if (parent != null) {
            parent.progress.set(pos);
            int newUpdated = (int) (pos - lastUpdateProgress);
            lastUpdateProgress = parent.progress.get();
            ratio = (long) ((double) newUpdated / (currentTime - lastUpdateTime) * 1.024);
        }
    }

    /**
     * Sets up the compression level.
     *
     * @param level the compression level.
     */
    @Override
    public void setCompressionLevel(int level) {
        if (level == 0) maxHuffmanSize = windowSize;
        else maxHuffmanSize = 16384;
    }

    /**
     * Sets up the parent.
     *
     * @param parent parent {@code Packer} which launched this {@code BWZCompressor}.
     */
    @Override
    public void setParent(Packer parent) {
        this.parent = parent;
    }

    /**
     * Returns the file length after bwz compression.
     *
     * @return the file length after bwz compression.
     */
    @Override
    public long getCompressedSize() {
        return cmpSize;
    }

    /**
     * Sets up the thread number.
     *
     * @param threads the thread number.
     */
    @Override
    public void setThreads(int threads) {
        this.threadNumber = threads;
    }
}


/**
 * An implementation of {@code Runnable} that compresses a single block using bwz algorithm.
 *
 * @author zbh
 * @see java.lang.Runnable
 * @since 0.5
 */
class EncodeThread implements Runnable {

    private byte[] buffer;
    private int beginIndex;
    private int partSize;
    private int threadId;
    private boolean useDc3;

    private byte[][] results;
    private byte[][] maps;
    private byte[] flags;
    private int windowSize;
    private BWZCompressor parent;

    /**
     * Creates a new {@code EncodeThread} instance.
     *
     * @param data       the whole data
     * @param windowSize the block size.
     * @param beginIndex the index in <code>data</code> to be compressed
     * @param partSize   the number of byte to be compressed
     * @param parent     the parent {@code BWZCompressor} which has launched this {@code EncodeThread}.
     * @param threadId   the id of this thread
     * @param useDc3     whether to use dc3 algorithm to construct suffix array
     */
    EncodeThread(byte[] data,
                 int beginIndex,
                 int partSize,
                 int windowSize,
                 BWZCompressor parent,
                 int threadId,
                 boolean useDc3) {

        this.buffer = data;
        this.beginIndex = beginIndex;
        this.partSize = partSize;
        this.windowSize = windowSize;
        this.parent = parent;
        this.threadId = threadId;
        this.useDc3 = useDc3;
    }

    static long bwtTime, mtfTime, mapTime, hufTime;

    private void start() {

        long t1 = System.currentTimeMillis();
        BWTEncoder be = new BWTEncoder(buffer, beginIndex, partSize, useDc3, threadId);
        int[] bwtResult = be.Transform();
        long t2 = System.currentTimeMillis();
        bwtTime += t2 - t1;

        parent.pos += partSize * 0.75;
        if (parent.parent != null) parent.parent.progress.set(parent.pos);  // Update progress

        MTFTransform mtf = new MTFTransform(bwtResult);
        int[] array = mtf.Transform();  // Also contains RLC Result.
        long t3 = System.currentTimeMillis();
        mtfTime += t3 - t2;

        int lenAfterMtf = array.length;

        int huffmanBlockNumber = lenAfterMtf % parent.maxHuffmanSize == 0 ?
                lenAfterMtf / parent.maxHuffmanSize : lenAfterMtf / parent.maxHuffmanSize + 1;
        // The number of huffman blocks needed for this BWT trunk.

        maps = new byte[huffmanBlockNumber][];
        results = new byte[huffmanBlockNumber][];
        flags = new byte[huffmanBlockNumber];

//        int eachLength = array.length / huffmanBlockNumber;
        int begin = 0;
        for (int i = 0; i < huffmanBlockNumber; i++) {
            int end = Math.min(begin + parent.maxHuffmanSize, lenAfterMtf);
            int partSize = end - begin;

            compressOneHufPart(i, array, begin, partSize);
            begin += partSize;
        }

        parent.pos += partSize * 0.25;  // Update progress again

        hufTime += System.currentTimeMillis() - t3;
//        System.out.println(String.format("bwt: %d, mtf: %d, huf: %d", bwtTime, mtfTime, hufTime));
    }

    private void compressOneHufPart(int i, int[] fullText, int textBegin, int textSize) {
//        System.out.println(textBegin + ", " + textSize);
        LongHuffmanCompressorRam hcr = new LongHuffmanCompressorRam(
                fullText,
                textBegin,
                textSize,
                BWZCompressor.HUFFMAN_TABLE_SIZE,
                BWZCompressor.HUFFMAN_END_SIG
        );
        byte[] map = hcr.getMap(BWZCompressor.HUFFMAN_TABLE_SIZE);
        int x = findAvailableOldMap(hcr, map, i);
        flags[i] = (byte) x;
        if (x > 0) {
            maps[i] = new byte[0];
            results[i] = hcr.compress(maps[i - x]);
        } else {
            maps[i] = map;
            results[i] = hcr.compress();
        }
    }

    /**
     * Writes the compressed data into the output stream <code>out</code>.
     *
     * @param out the target output stream
     * @throws IOException if the <code>out</code> is not writable.
     */
    void writeTo(OutputStream out) throws IOException {
        for (byte[] result : results) {
            parent.mainLen += result.length;
            out.write(result);
        }
    }

    /**
     * Writes the compressed huffman head into the output stream <code>out</code>.
     *
     * @param out the target output stream
     * @throws IOException if the <code>out</code> is not writable.
     */
    void writeCompressedMap(OutputStream out) throws IOException {
        long t0 = System.currentTimeMillis();
        int mapLength = 0;
        for (byte[] map : maps) mapLength += map.length;
        byte[] totalMap = new byte[mapLength];
        int i = 0, j = 0;
        while (i < maps.length) {
            System.arraycopy(maps[i], 0, totalMap, j, maps[i].length);
            j += maps[i].length;
            i += 1;
        }

        byte[] flagsMtf = new MTFTransformByte(flags).Transform(256);

        BWTEncoderByte beb = new BWTEncoderByte(totalMap);
        byte[] bebMap = beb.Transform();
        byte[] mapMtf = new MTFTransformByte(bebMap).Transform(18);
        byte[] cmpMap = new MapCompressor(mapMtf).Compress(false);

        /*
         * Block structure:
         * 0 - 1: length of compressed flags
         * 2 - 5 : length of compressed map
         * 5 - 8 : index of original row of bwt
         */
        byte[] numbers = new byte[8];
        // this value will not exceed 32768
        Bytes.shortToBytes(flagsMtf.length, numbers, 0);
        Bytes.intToBytes24(cmpMap.length, numbers, 2);
        Bytes.intToBytes24(beb.getOrigRowIndex(), numbers, 5);

        mapTime += System.currentTimeMillis() - t0;
//        System.out.println(String.format("map time %d, nl %d, flags %d, map %d",
//                mapTime, numbers.length, flagsMtf.length, cmpMap.length));

        out.write(numbers);
        out.write(flagsMtf);
        out.write(cmpMap);

        parent.mainLen += (flagsMtf.length + cmpMap.length + 8);
    }

    private int findAvailableOldMap(LongHuffmanCompressorRam hcr, byte[] origMap, int currentIndex) {
        long closest = 36;
        int distance = 0;
        for (int i = currentIndex - 1; i > currentIndex - 253; i--) {
            if (i < 0) break;
            if (maps[i].length > 0) {
                long valid = hcr.calculateExpectLength(maps[i]);
                if (valid > 0) {
                    long diff = hcr.calculateExpectLength(maps[i]) - hcr.calculateExpectLength(origMap);
                    if (diff < closest) {
                        closest = diff;
                        distance = currentIndex - i;
                    }
                }
            }
        }
        return distance;
    }

    /**
     * Starts this {@code EncodeThread}.
     */
    @Override
    public void run() {
        try {
            start();
        } catch (OutOfMemoryError e) {
            parent.parent.setError("Out of memory", 1);
        }
    }
}


/**
 * An implementation of {@code Runnable}, used to update status of a {@code BWZCompressor} instance to a
 * {@code Packer} instance every 1 second.
 *
 * @author zbh
 * @see java.lang.Runnable
 * @since 0.5
 */
class Timer implements Runnable {

    private Packer packer;
    private BWZCompressor compressor;
    private int timeUsed;

    /**
     * Creates a new {@code Timer} instance.
     *
     * @param parent     the parent {@code Packer} of <code>compressor</code>
     * @param compressor the {@code BWZCompressor} which created this {@code Timer}.
     */
    Timer(Packer parent, BWZCompressor compressor) {
        this.packer = parent;
        this.compressor = compressor;
    }

    /**
     * Runs this {@code Timer}.
     */
    @Override
    public void run() {
        while (compressor.isRunning) {
            packer.timeUsed.setValue(Util.secondToString(timeUsed));
            if (compressor.pos != 0) {
                updateBwzProgress(compressor.pos,
                        packer.getTotalOrigSize(),
                        packer.percentage,
                        packer.ratio,
                        compressor.ratio,
                        packer.timeExpected,
                        packer.passedLength);
                packer.cmpLength.set(Util.sizeToReadable(compressor.mainLen));

                double cmpRatio = (double) compressor.mainLen / compressor.pos;
                double roundedRatio = (double) Math.round(cmpRatio * 1000) / 10;
                packer.currentCmpRatio.set(roundedRatio + "%");
            }

            timeUsed += 1;

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
    }

    static void updateBwzProgress(long pos,
                                  long totalOrigSize,
                                  ReadOnlyStringWrapper percentage,
                                  ReadOnlyStringWrapper ratio,
                                  long ratio2,
                                  ReadOnlyStringWrapper timeExpected,
                                  ReadOnlyStringWrapper passedLength) {
        double finished = (double) pos / totalOrigSize;
        double rounded = (double) Math.round(finished * 1000) / 10;
        percentage.set(String.valueOf(rounded));

        ratio.set(String.valueOf(ratio2));

        if (ratio2 != 0) {
            long expectTime = (totalOrigSize - pos) / ratio2 / 1024;
            timeExpected.set(Util.secondToString(expectTime));
        }

        passedLength.set(Util.sizeToReadable(pos));
    }
}
