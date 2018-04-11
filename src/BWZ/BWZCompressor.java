/*
 * BWZCompressor
 * Implemented by Bohan Zhang @Trash Software Studio, 2018
 *
 * This algorithm is implemented by using Burrows-Wheeler Transform, Move-To-Front Transform,
 * Run-Length Coding and Huffman Coding.
 * A suffix array is presented in case to build the BWT string.
 * The suffix array is constructed using Doubling Algorithm, which takes O(n * log n) time.
 */

package BWZ;

import Huffman.MapCompressor.MapCompressor;
import Huffman.RLCCoder.RLCCoder;
import Interface.Compressor;
import LZZ2.Util.LZZ2Util;
import LongHuffman.LongHuffmanCompressorRam;
import Packer.Packer;
import Utility.Bytes;
import Utility.Util;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BWZCompressor implements Compressor {

    private String mainTempName;

    private OutputStream mainOut;

    private FileChannel fis;

    private long cmpSize;

    int mainLen;

    private int windowSize;

    private Packer parent;

    private long lastUpdateProgress;

    private LinkedList<byte[]> huffmanMaps = new LinkedList<>();

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

        ArrayList<byte[]> blocks = new ArrayList<>();

        int read;
        ByteBuffer buffer = ByteBuffer.allocate(windowSize);
        while ((read = fis.read(buffer)) > 0) {
            buffer.flip();
            byte[] validBlock;

            if (read == windowSize) {
                validBlock = buffer.array();
            } else {
                validBlock = new byte[read];
                System.arraycopy(buffer.array(), 0, validBlock, 0, read);
            }
            buffer.clear();
            blocks.add(validBlock);

            if (blocks.size() == threadNumber || read < windowSize) {
                EncodeThread[] threads = new EncodeThread[blocks.size()];
                ExecutorService es = Executors.newCachedThreadPool();
                for (int i = 0; i < threads.length; i++) {
                    EncodeThread et = new EncodeThread(blocks.get(i));
                    threads[i] = et;
                    es.execute(et);
                }
                es.shutdown();
                es.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);  // Wait for all threads complete.

                for (EncodeThread et : threads) {
                    byte[] array = et.getResult();
                    mainLen += array.length;
                    mainOut.write(array);
                    huffmanMaps.addLast(et.getMap());
                    pos += et.origLen();
                }
                blocks.clear();

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
            //
        }
        fis.close();

        mainOut.flush();
        mainOut.close();

        if (parent != null && parent.isInterrupted) {
            isRunning = false;
            deleteTemp();
            return;
        }

        byte[] mainMap = new byte[huffmanMaps.size() * 259];
        for (int i = 0; i < huffmanMaps.size(); i++) {
            System.arraycopy(huffmanMaps.get(i), 0, mainMap, i * 259, 259);
        }

        RLCCoder rlc = new RLCCoder(mainMap);
        rlc.Encode();
        byte[] rlcMain = rlc.getMainResult();
        String rlcBits = rlc.getRlcBits();

        byte[] rlcBytes = Bytes.stringToBytesFull(rlcBits);

        MapCompressor mc = new MapCompressor(rlcMain);
        byte[] csq = mc.Compress();

        out.write(csq);
        out.write(rlcBytes);

        mainLen = Util.fileConcatenate(out, new String[]{mainTempName}, 8192);

        deleteTemp();

        int csqLen = csq.length;
        int rlcByteLen = rlcBytes.length;
        int[] sizes = new int[]{csqLen, rlcByteLen};
        byte[] sizeBlock = LZZ2Util.generateSizeBlock(sizes);
        out.write(sizeBlock);

        cmpSize = mainLen + csqLen + rlcByteLen + sizeBlock.length;
        isRunning = false;

    }

    private void updateInfo(long currentTime, long lastUpdateTime) {
        parent.progress.set(pos);

        int newUpdated = (int) (pos - lastUpdateProgress);
        lastUpdateProgress = parent.progress.get();
        ratio = (long) ((double) newUpdated / (currentTime - lastUpdateTime) * 1.024);
    }

    @Override
    public void setCompressionLevel(int level) {
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

    private byte[] result;

    private byte[] map;

    EncodeThread(byte[] partData) {
        this.buffer = partData;
    }

    private void start() {
        BWTEncoder be = new BWTEncoder(buffer);
        short[] bwtResult = be.Transform2();
        MTFTransform mtf = new MTFTransform(bwtResult);
        short[] array = mtf.Transform();  // Also contains RLC Result.
        LongHuffmanCompressorRam hcr = new LongHuffmanCompressorRam(array, 259, (short) 258);
        map = hcr.getMap(259);
        result = hcr.Compress();
    }

    public byte[] getResult() {
        return result;
    }

    public byte[] getMap() {
        return map;
    }

    long origLen() {
        return buffer.length;
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

                long expectTime = (packer.getTotalOrigSize() - compressor.pos) / compressor.ratio / 1024;
                packer.timeExpected.set(Util.secondToString(expectTime));

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
