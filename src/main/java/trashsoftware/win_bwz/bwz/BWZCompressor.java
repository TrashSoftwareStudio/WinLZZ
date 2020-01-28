/*
 * BWZCompressor
 * Implemented by Bohan Zhang @Trash Software Studio, 2018
 *
 * This algorithm is implemented by using Burrows-Wheeler Transform, Move-To-Front Transform,
 * Run-Length Coding and Huffman Coding.
 * A suffix array is presented in case to build the BWT string.
 */

package trashsoftware.win_bwz.bwz;

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
    final static int dc3DecisionSize = 1048576;

    /**
     * The alphabet size of the main huffman table.
     */
    final static int huffmanTableSize = 259;

    /**
     * The signal that marks the end of a huffman stream.
     */
    public final static int huffmanEndSig = 258;

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
//            byte[] validBlock;
//            if (read == windowSize * threadNumber) {
//                validBlock = block;
//            } else {
//                validBlock = new byte[read];
//                System.arraycopy(block, 0, validBlock, 0, read);
//            }

            int threadsNeed;
            if (read % windowSize == 0) threadsNeed = read / windowSize;
            else threadsNeed = read / windowSize + 1;

            EncodeThread[] threads = new EncodeThread[threadsNeed];
            ExecutorService es = Executors.newCachedThreadPool();
            int begin = 0;
            for (int i = 0; i < threads.length; i++) {
//                byte[] buffer;
//                if ((i + 1) * windowSize <= validBlock.length) {
//                    buffer = new byte[windowSize];
//                    System.arraycopy(validBlock, i * windowSize, buffer, 0, windowSize);
//                } else {
//                    int len = validBlock.length % windowSize;
//                    buffer = new byte[len];
//                    System.arraycopy(validBlock, i * windowSize, buffer, 0, len);
//                }
                int end = Math.min(begin + windowSize, read);
                EncodeThread et = new EncodeThread(block, begin, end - begin, windowSize, this);
                threads[i] = et;
                es.execute(et);

                begin += windowSize;
            }
            es.shutdown();
            es.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);  // Wait for all threads complete.

            for (EncodeThread et : threads) {
                et.writeCompressedMap(out);
                et.writeTo(out);
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

    private void compress2() throws Exception {
        long lastCheckTime = System.currentTimeMillis();
        long currentTime;

        int read;
        ByteBuffer block = ByteBuffer.allocate(windowSize * threadNumber);
        while ((read = fis.read(block)) > 0) {
            block.flip();
            byte[] buffer = block.array();
//            byte[] validBlock;
//            if (read == windowSize * threadNumber) {
//                validBlock = block.array();
//            } else {
//                validBlock = new byte[read];
//                System.arraycopy(block.array(), 0, validBlock, 0, read);
//            }

            int threadsNeed;
            if (read % windowSize == 0) threadsNeed = read / windowSize;
            else threadsNeed = read / windowSize + 1;

            EncodeThread[] threads = new EncodeThread[threadsNeed];
            ExecutorService es = Executors.newCachedThreadPool();
            int begin = 0;
            for (int i = 0; i < threads.length; i++) {
//                byte[] buffer;
//                if ((i + 1) * windowSize <= validBlock.length) {
//                    buffer = new byte[windowSize];
//                    System.arraycopy(validBlock, i * windowSize, buffer, 0, windowSize);
//                } else {
//                    int len = validBlock.length % windowSize;
//                    buffer = new byte[len];
//                    System.arraycopy(validBlock, i * windowSize, buffer, 0, len);
//                }
                int end = Math.min(begin + windowSize, read);
                EncodeThread et = new EncodeThread(buffer, begin, end - begin, windowSize, this);
                threads[i] = et;
                es.execute(et);

                begin += windowSize;
            }
            es.shutdown();
            es.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);  // Wait for all threads complete.

            for (EncodeThread et : threads) {
                et.writeCompressedMap(out);
                et.writeTo(out);
            }
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

    public int getThreadNumber() {
        return threadNumber;
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
     */
    EncodeThread(byte[] data, int beginIndex, int partSize, int windowSize, BWZCompressor parent) {
        this.buffer = data;
        this.beginIndex = beginIndex;
        this.partSize = partSize;
        this.windowSize = windowSize;
        this.parent = parent;
    }

    static long bwtTime, mtfTime;

    private void start() {
        boolean isDc3 = partSize >= BWZCompressor.dc3DecisionSize;
        int huffmanBlockNumber;  // The number of huffman blocks needed for this BWT trunk.
        if (partSize <= parent.maxHuffmanSize) {
            huffmanBlockNumber = 1;
        } else {
            huffmanBlockNumber = partSize / parent.maxHuffmanSize;
            if (partSize < windowSize) huffmanBlockNumber += 1;
        }

        maps = new byte[huffmanBlockNumber][];
        results = new byte[huffmanBlockNumber][];
        flags = new byte[huffmanBlockNumber];

        long t1 = System.currentTimeMillis();
        BWTEncoder be = new BWTEncoder(buffer, beginIndex, partSize, isDc3);
        int[] bwtResult = be.Transform();
        long t2 = System.currentTimeMillis();
        bwtTime += t2 - t1;

        parent.pos += partSize * 0.75;
        if (parent.parent != null) parent.parent.progress.set(parent.pos);  // Update progress

        MTFTransform mtf = new MTFTransform(bwtResult);
        int[] array = mtf.Transform();  // Also contains RLC Result.
        mtfTime += System.currentTimeMillis() - t2;
//        System.out.println(String.format("bwt: %d, mtf: %d", bwtTime, mtfTime));

        int eachLength = array.length / huffmanBlockNumber;
        int pos = 0;
        for (int i = 0; i < huffmanBlockNumber - 1; i++) {
            int[] part = new int[eachLength];
            System.arraycopy(array, pos, part, 0, eachLength);
            pos += eachLength;
            LongHuffmanCompressorRam hcr =
                    new LongHuffmanCompressorRam(part, BWZCompressor.huffmanTableSize, BWZCompressor.huffmanEndSig);
            byte[] map = hcr.getMap(BWZCompressor.huffmanTableSize);
            int x = findAvailableOldMap(hcr, map, i);  // Look for an old map that suites the current need.
            flags[i] = (byte) x;
            if (x > 0) {  // If found.
                maps[i] = new byte[0];
                results[i] = hcr.compress(maps[i - x]);
            } else {  // If not found.
                maps[i] = map;
                results[i] = hcr.compress();
            }
        }

        // The last huffman block.
        int i = huffmanBlockNumber - 1;
        int[] lastPart = new int[array.length - pos];  // Since we need to put everything remaining inside
        // the last block.
        System.arraycopy(array, pos, lastPart, 0, lastPart.length);
        LongHuffmanCompressorRam hcr =
                new LongHuffmanCompressorRam(lastPart, BWZCompressor.huffmanTableSize, BWZCompressor.huffmanEndSig);
        byte[] map = hcr.getMap(BWZCompressor.huffmanTableSize);
        int x = findAvailableOldMap(hcr, map, i);
        flags[i] = (byte) x;
        if (x > 0) {
            maps[i] = new byte[0];
            results[i] = hcr.compress(maps[i - x]);
        } else {
            maps[i] = map;
            results[i] = hcr.compress();
        }
        parent.pos += partSize * 0.25;  // Update progress again
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
        System.arraycopy(Bytes.shortToBytes((short) flagsMtf.length), 0, numbers, 0, 2);
        System.arraycopy(Bytes.intToBytes24(cmpMap.length), 0, numbers, 2, 3);
        System.arraycopy(Bytes.intToBytes24(beb.getOrigRowIndex()), 0, numbers, 5, 3);

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
        start();
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
                double finished = (double) compressor.pos / packer.getTotalOrigSize();
                double rounded = (double) Math.round(finished * 1000) / 10;
                packer.percentage.set(String.valueOf(rounded));

                packer.ratio.set(String.valueOf(compressor.ratio));

                if (compressor.ratio != 0) {
                    long expectTime = (packer.getTotalOrigSize() - compressor.pos) / compressor.ratio / 1024;
                    packer.timeExpected.set(Util.secondToString(expectTime));
                }

                packer.passedLength.set(Util.sizeToReadable(compressor.pos));
                packer.cmpLength.set(Util.sizeToReadable(compressor.mainLen));

                double cmpRatio = (double) compressor.mainLen / compressor.pos;
                double roundedRatio = (double) Math.round(cmpRatio * 1000) / 10;
                packer.currentCmpRatio.set(String.valueOf(roundedRatio) + "%");
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
