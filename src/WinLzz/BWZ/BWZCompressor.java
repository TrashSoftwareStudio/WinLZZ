/*
 * BWZCompressor
 * Implemented by Bohan Zhang @Trash Software Studio, 2018
 *
 * This algorithm is implemented by using Burrows-Wheeler Transform, Move-To-Front Transform,
 * Run-Length Coding and Huffman Coding.
 * A suffix array is presented in case to build the BWT string.
 */

package WinLzz.BWZ;

import WinLzz.BWZ.BWT.BWTEncoder;
import WinLzz.BWZ.BWT.BWTEncoderByte;
import WinLzz.Huffman.MapCompressor.MapCompressor;
import WinLzz.Interface.Compressor;
import WinLzz.LZZ2.Util.LZZ2Util;
import WinLzz.LongHuffman.LongHuffmanCompressorRam;
import WinLzz.Packer.Packer;
import WinLzz.Utility.Util;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BWZCompressor implements Compressor {

    private String mainTempName;

    int maxHuffmanSize = 16384;  // Default maximum size of a huffman-coding block.

    final static int dc3DecisionSize = 1048576;  // Decision size of suffix array building algorithm.
    // DC3 algorithm is used if the window size if greater than or equal to this size in case of increasing speed.
    // Otherwise, doubling algorithm is used (since doubling algorithm takes O(n * log n) while dc3 takes O(n),
    // But doubling algorithm has a much smaller constant).

    final static short huffmanTableSize = 259;
    final static short huffmanEndSig = 258;
    private OutputStream mainOut;
    private FileChannel fis;

    private long cmpSize;  // Total size after compression.

    long mainLen;
    private int windowSize;
    Packer parent;
    private long lastUpdateProgress;
    private LinkedList<byte[]> huffmanMaps = new LinkedList<>();
    private LinkedList<Byte> huffmanFlags = new LinkedList<>();
    private int threadNumber = 1;
    boolean isRunning = true;
    long ratio;
    long pos;

    /**
     * Creates a new instance of BWZCompressor.
     *
     * @param inFile     name/path of file to compress.
     * @param windowSize the block size.
     * @throws IOException if the input file is not readable.
     */
    public BWZCompressor(String inFile, int windowSize) throws IOException {
        this.windowSize = windowSize;
        this.fis = new FileInputStream(inFile).getChannel();
        generateTempNames(inFile);
    }

    private void generateTempNames(String inFile) {
        mainTempName = inFile + ".temp";
    }

    private void deleteTemp() {
        Util.deleteFile(mainTempName);
    }

    private void compress() throws Exception {
        long lastCheckTime = System.currentTimeMillis();
        long currentTime;

        int read;
        ByteBuffer block = ByteBuffer.allocate(windowSize * threadNumber);
        while ((read = fis.read(block)) > 0) {
            block.flip();
            byte[] validBlock;
            if (read == windowSize * threadNumber) {
                validBlock = block.array();
            } else {
                validBlock = new byte[read];
                System.arraycopy(block.array(), 0, validBlock, 0, read);
            }

            int threadsNeed;
            if (read % windowSize == 0) threadsNeed = read / windowSize;
            else threadsNeed = read / windowSize + 1;

            EncodeThread[] threads = new EncodeThread[threadsNeed];
            ExecutorService es = Executors.newCachedThreadPool();
            for (int i = 0; i < threads.length; i++) {
                byte[] buffer;
                if ((i + 1) * windowSize <= validBlock.length) {
                    buffer = new byte[windowSize];
                    System.arraycopy(validBlock, i * windowSize, buffer, 0, windowSize);
                } else {
                    int len = validBlock.length % windowSize;
                    buffer = new byte[len];
                    System.arraycopy(validBlock, i * windowSize, buffer, 0, len);
                }
                EncodeThread et = new EncodeThread(buffer, windowSize, this);
                threads[i] = et;
                es.execute(et);
            }
            es.shutdown();
            es.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);  // Wait for all threads complete.

            for (EncodeThread et : threads) {
                et.writeTo(mainOut, this);
                byte[][] maps = et.getMaps();
                byte[] flags = et.getFlags();
                for (byte[] map : maps) huffmanMaps.addLast(map);
                for (byte flag : flags) huffmanFlags.addLast(flag);
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
    public void Compress(OutputStream out) throws Exception {
        mainOut = new BufferedOutputStream(new FileOutputStream(mainTempName));

        Thread timer;
        if (parent != null) {
            timer = new Thread(new Timer(parent, this));
            timer.start();
        }
        try {
            compress();
        } catch (InterruptedException e) {
            // The process is interrupted by user.
        }
        fis.close();

        mainOut.flush();
        mainOut.close();

        if (parent != null && parent.isInterrupted) {
            isRunning = false;
            deleteTemp();
            return;
        }

        byte[] mainMap = new byte[Util.collectionOfArrayLength(huffmanMaps) + huffmanTableSize];
        // Size of all huffman maps + size of map of flags compressor.
        int i = huffmanTableSize;

        byte[] flags = Util.collectionToArray(huffmanFlags);
        BWTEncoder flagsBwt = new BWTEncoder(flags, false);
        short[] flagsMtf = new MTFTransform(flagsBwt.Transform()).Transform();
        LongHuffmanCompressorRam flagsHuf = new LongHuffmanCompressorRam(flagsMtf, huffmanTableSize, huffmanEndSig);
        System.arraycopy(flagsHuf.getMap(huffmanTableSize), 0, mainMap, 0, huffmanTableSize);

        byte[] compressedFlags = flagsHuf.Compress();
        int cmpFlagsLen = compressedFlags.length;
        out.write(compressedFlags);

        // Concatenate all canonical huffman maps into one long map.
        while (!huffmanMaps.isEmpty()) {
            byte[] map = huffmanMaps.removeFirst();
            System.arraycopy(map, 0, mainMap, i, map.length);
            i += map.length;
        }

        BWTEncoderByte beb = new BWTEncoderByte(mainMap);
        byte[] bwtMap = beb.Transform();
        byte[] mtfMap = new MTFTransformByte(bwtMap).Transform();

        MapCompressor mc = new MapCompressor(mtfMap);
        byte[] csq = mc.Compress(false);
        out.write(csq);

        mainLen = Util.fileConcatenate(out, new String[]{mainTempName}, 8192);  // Concatenate the main out
        // file with the file containing huffman compressed result of main text.

        deleteTemp();
        int csqLen = csq.length;
        long[] sizes = new long[]{csqLen, LZZ2Util.windowSizeToByte(maxHuffmanSize), beb.getOrigRowIndex(), cmpFlagsLen};
        byte[] sizeBlock = LZZ2Util.generateSizeBlock(sizes);
        out.write(sizeBlock);

        cmpSize = mainLen + csqLen + sizeBlock.length + cmpFlagsLen;
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

    @Override
    public void setCompressionLevel(int level) {
        if (level == 0) maxHuffmanSize = windowSize;
        else maxHuffmanSize = 16384;
    }

    @Override
    public void setParent(Packer parent) {
        this.parent = parent;
    }

    @Override
    public long getCompressedSize() {
        return cmpSize;
    }

    @Override
    public void setThreads(int threads) {
        this.threadNumber = threads;
    }
}


class EncodeThread implements Runnable {

    private byte[] buffer;
    private byte[][] results;
    private byte[][] maps;
    private byte[] flags;
    private int windowSize;
    private BWZCompressor parent;

    EncodeThread(byte[] partData, int windowSize, BWZCompressor parent) {
        this.buffer = partData;
        this.windowSize = windowSize;
        this.parent = parent;
    }

    private void start() {
        boolean isDc3 = buffer.length >= BWZCompressor.dc3DecisionSize;
        int huffmanBlockNumber;  // The number of huffman blocks needed for this BWT trunk.
        if (buffer.length <= parent.maxHuffmanSize) {
            huffmanBlockNumber = 1;
        } else {
            huffmanBlockNumber = buffer.length / parent.maxHuffmanSize;
            if (buffer.length < windowSize) huffmanBlockNumber += 1;
        }

        maps = new byte[huffmanBlockNumber][];
        results = new byte[huffmanBlockNumber][];
        flags = new byte[huffmanBlockNumber];

        BWTEncoder be = new BWTEncoder(buffer, isDc3);
        short[] bwtResult = be.Transform();

        parent.pos += buffer.length * 0.6;
        if (parent.parent != null) parent.parent.progress.set(parent.pos);  // Update progress

        MTFTransform mtf = new MTFTransform(bwtResult);
        short[] array = mtf.Transform();  // Also contains RLC Result.

        int eachLength = array.length / huffmanBlockNumber;
        int pos = 0;
        for (int i = 0; i < huffmanBlockNumber - 1; i++) {
            short[] part = new short[eachLength];
            System.arraycopy(array, pos, part, 0, eachLength);
            pos += eachLength;
            LongHuffmanCompressorRam hcr =
                    new LongHuffmanCompressorRam(part, BWZCompressor.huffmanTableSize, BWZCompressor.huffmanEndSig);
            byte[] map = hcr.getMap(BWZCompressor.huffmanTableSize);
            int x = findAvailableOldMap(hcr, map, i);  // Look for an old map that suites the current need.
            flags[i] = (byte) x;
            if (x > 0) {  // If found.
                maps[i] = new byte[0];
                results[i] = hcr.Compress(maps[i - x]);
            } else {  // If not found.
                maps[i] = map;
                results[i] = hcr.Compress();
            }
        }

        // The last huffman block.
        int i = huffmanBlockNumber - 1;
        short[] lastPart = new short[array.length - pos];  // Since we need to put everything remaining inside
        // the last block.
        System.arraycopy(array, pos, lastPart, 0, lastPart.length);
        LongHuffmanCompressorRam hcr =
                new LongHuffmanCompressorRam(lastPart, BWZCompressor.huffmanTableSize, BWZCompressor.huffmanEndSig);
        byte[] map = hcr.getMap(BWZCompressor.huffmanTableSize);
        int x = findAvailableOldMap(hcr, map, i);
        flags[i] = (byte) x;
        if (x > 0) {
            maps[i] = new byte[0];
            results[i] = hcr.Compress(maps[i - x]);
        } else {
            maps[i] = map;
            results[i] = hcr.Compress();
        }
        parent.pos += buffer.length * 0.4;  // Update progress again
    }

    void writeTo(OutputStream out, BWZCompressor parent) throws IOException {
        for (byte[] result : results) {
            parent.mainLen += result.length;
            out.write(result);
        }
    }

    byte[][] getMaps() {
        return maps;
    }

    byte[] getFlags() {
        return flags;
    }

    private int findAvailableOldMap(LongHuffmanCompressorRam hcr, byte[] origMap, int currentIndex) {
        long closest = 36;
        int distance = 0;
        for (int i = currentIndex - 1; i > currentIndex - 255; i--) {
            if (i < 0 || maps[i] == null) break;
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

    @Override
    public void run() {
        start();
    }
}


class Timer implements Runnable {

    private Packer packer;
    private BWZCompressor compressor;
    private int timeUsed;

    Timer(Packer parent, BWZCompressor compressor) {
        this.packer = parent;
        this.compressor = compressor;
    }

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
