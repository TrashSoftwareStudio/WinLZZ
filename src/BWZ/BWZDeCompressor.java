package BWZ;

import Huffman.MapCompressor.MapDeCompressor;
import Interface.DeCompressor;
import LZZ2.Util.LZZ2Util;
import LongHuffman.LongHuffmanInputStream;
import Packer.UnPacker;
import Utility.Util;

import java.io.*;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BWZDeCompressor implements DeCompressor {

    private final static int readBufferSize = 8192;

    private String cmpMainName, mainTempName;

    private int csqLen, mainLen;

    private int windowSize;

    private BufferedInputStream bis;

    private LinkedList<byte[]> huffmanMaps = new LinkedList<>();

    private long fileLength;

    private int threadNum = 2;  // Default thread number.

    private UnPacker parent;

    private long lastUpdateProgress;

    boolean isRunning = true;

    long ratio;

    long pos;

    public BWZDeCompressor(String inFile, int windowSize) throws IOException {
        generateNames(inFile);
        this.windowSize = windowSize;

        RandomAccessFile raf = new RandomAccessFile(inFile, "r");
        fileLength = new File(inFile).length();
        int length = (int) fileLength;
        raf.seek(length - 1);
        int sizeBlockSize = raf.readByte() & 0xff;
        byte[] sizeBlock = new byte[sizeBlockSize];
        raf.seek(length - sizeBlockSize - 1);
        raf.read(sizeBlock);
        raf.close();

        int[] sizes = LZZ2Util.recoverSizeBlock(sizeBlock, 1);
        csqLen = sizes[0];
        mainLen = length - csqLen - sizeBlockSize - 1;

        bis = new BufferedInputStream(new FileInputStream(inFile));
    }

    private void generateNames(String inName) {
        cmpMainName = inName + ".cmp";
        mainTempName = inName + ".temp";
    }

    private void uncompressHead() throws IOException {
        byte[] csq = new byte[csqLen];
        if (bis.read(csq) != csqLen) throw new IOException("Error occurs while reading");

        MapDeCompressor mdc = new MapDeCompressor(csq);
        int huffmanBlockSize;
        if (windowSize > BWZCompressor.maxHuffmanSize) huffmanBlockSize = BWZCompressor.maxHuffmanSize;
        else huffmanBlockSize = windowSize;

        byte[] rlcMain = mdc.Uncompress((int) (259 * (fileLength / huffmanBlockSize + 1)), false);
        byte[] zeroRlc = new ZeroRLCDecoderByte(rlcMain).Decode();
        byte[] mainMap = new MTFInverseByte(zeroRlc).Inverse();
        copyToTemp();

        for (int i = 0; i < mainMap.length; i += 259) {
            byte[] map = new byte[259];
            System.arraycopy(mainMap, i, map, 0, 259);
            huffmanMaps.addLast(map);
        }
    }

    private void copyToTemp() throws IOException {
        Util.fileTruncate(bis, cmpMainName, readBufferSize, mainLen);
    }

    private short[] decodeBlockToFixedLength(short[] block) {
        return new ZeroRLCDecoder(block).Decode();
    }

    private void decode(OutputStream out, FileChannel fc) throws Exception {
        long lastCheckTime = System.currentTimeMillis();
        if (parent != null) {
            if (parent.isInTest) parent.step.set("正在测试...");
            else parent.step.set("正在解压...");
        }
        long currentTime;

        int huffmanBlockForEachWindow;
        if (windowSize <= BWZCompressor.maxHuffmanSize) huffmanBlockForEachWindow = 1;
        else huffmanBlockForEachWindow = windowSize / BWZCompressor.maxHuffmanSize;

        ArrayList<short[]> blockList = new ArrayList<>();
        ArrayList<short[]> huffmanBlockList = new ArrayList<>();

        LongHuffmanInputStream his = new LongHuffmanInputStream(fc, 259, BWZCompressor.maxHuffmanSize);
        short[] huffmanResult;

        while (!huffmanMaps.isEmpty()) {
            huffmanResult = his.read(huffmanMaps.removeFirst(), (short) 258);

            huffmanBlockList.add(huffmanResult);

            // Got a complete block for rlc decoding, or reaches the end of file.
            if (huffmanBlockList.size() == huffmanBlockForEachWindow || huffmanMaps.isEmpty()) {
                int totalLen = totalLength(huffmanBlockList);
                short[] concatenateHuffman = new short[totalLen];
                int index = 0;
                for (short[] s : huffmanBlockList) {
                    System.arraycopy(s, 0, concatenateHuffman, index, s.length);
                    index += s.length;
                }

                short[] lastBlock = decodeBlockToFixedLength(concatenateHuffman);
                blockList.add(lastBlock);
                huffmanBlockList.clear();

                // Got enough blocks to start multi-threading, or reaches the end of file.
                if (blockList.size() == threadNum || lastBlock.length < windowSize + 4) {
                    ExecutorService es = Executors.newCachedThreadPool();
                    DecodeThread threads[] = new DecodeThread[blockList.size()];
                    for (int i = 0; i < threads.length; i++) {
                        threads[i] = new DecodeThread(blockList.get(i));
                        es.execute(threads[i]);
                    }
                    blockList.clear();

                    es.shutdown();
                    es.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);  // Wait for all threads complete.

                    for (DecodeThread dt : threads) {
                        byte[] result = dt.getResult();
                        out.write(result);
                        pos += result.length;
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

    @Override
    public void Uncompress(OutputStream out) throws Exception {
        if (parent != null) {
            Thread timer = new Thread(new DTimer(parent, this));
            timer.start();
        }
        uncompressHead();
        bis.close();
        FileChannel fc = new FileInputStream(cmpMainName).getChannel();
        try {
            decode(out, fc);
        } catch (InterruptedException | ClosedByInterruptException e) {
            // If the user interrupts the decompression process.
        }
        fc.close();
        deleteTemp();
        isRunning = false;
    }

    private void updateInfo(long currentTime, long lastCheckTime) {
        parent.progress.set(pos);
        int newUpdated = (int) (pos - lastUpdateProgress);
        lastUpdateProgress = parent.progress.get();
        ratio = (long) ((double) newUpdated / (currentTime - lastCheckTime) * 1.024);
    }

    private int totalLength(Collection<short[]> c) {
        int len = 0;
        for (short[] s : c) len += s.length;
        return len;
    }

    @Override
    public void setParent(UnPacker parent) {
        this.parent = parent;
    }

    @Override
    public void deleteCache() {
        deleteTemp();
    }

    @Override
    public void setThreads(int threadNum) {
        this.threadNum = threadNum;
    }

    private void deleteTemp() {
        Util.deleteFile(cmpMainName);
        Util.deleteFile(mainTempName);
    }
}


class DecodeThread implements Runnable {

    private short[] text;

    private byte[] result;

    DecodeThread(short[] text) {
        this.text = text;
    }

    @Override
    public void run() {
        short[] mtf = new MTFInverse(text).Inverse();
        result = new BWTDecoder(mtf).Decode();
    }

    byte[] getResult() {
        return result;
    }
}


class DTimer implements Runnable {

    private UnPacker unPacker;

    private BWZDeCompressor dec;

    private int timeUsed;

    DTimer(UnPacker unPacker, BWZDeCompressor dec) {
        this.unPacker = unPacker;
        this.dec = dec;
    }

    @Override
    public void run() {
        while (dec.isRunning) {
            unPacker.timeUsed.setValue(Util.secondToString(timeUsed));

            if (dec.pos != 0) {
                double finished = (double) dec.pos / unPacker.getTotalOrigSize();
                double rounded = (double) Math.round(finished * 1000) / 10;
                unPacker.percentage.set(String.valueOf(rounded));

                unPacker.ratio.set(String.valueOf(dec.ratio));

                long expectTime = (unPacker.getTotalOrigSize() - dec.pos) / dec.ratio / 1024;
                unPacker.timeExpected.set(Util.secondToString(expectTime));

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
