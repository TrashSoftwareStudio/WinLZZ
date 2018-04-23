package BWZ;

import Huffman.MapCompressor.MapDeCompressor;
import Interface.DeCompressor;
import LZZ2.Util.LZZ2Util;
import LongHuffman.LongHuffmanDeCompressorRam;
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
    private int csqLen, mainLen, cmpFlagsLen, huffmanBlockMaxSize, windowSize, origRowIndex;
    private BufferedInputStream bis;
    private LinkedList<byte[]> huffmanMaps = new LinkedList<>();
    private long fileLength;
    private int threadNum = 1;  // Default thread number.
    UnPacker parent;
    private long lastUpdateProgress;
    boolean isRunning = true;
    long ratio, pos;

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

        int[] sizes = LZZ2Util.recoverSizeBlock(sizeBlock, 4);
        csqLen = sizes[0];
        huffmanBlockMaxSize = (int) Math.pow(2, sizes[1]);
        origRowIndex = sizes[2];
        cmpFlagsLen = sizes[3];
        mainLen = length - csqLen - sizeBlockSize - cmpFlagsLen - 1;

        bis = new BufferedInputStream(new FileInputStream(inFile));
    }

    private void generateNames(String inName) {
        cmpMainName = inName + ".cmp";
        mainTempName = inName + ".temp";
    }

    private void uncompressHead() throws IOException {

        byte[] cmpFlags = new byte[cmpFlagsLen];
        if (bis.read(cmpFlags) != cmpFlagsLen) throw new IOException("Error occurs while reading");

        byte[] csq = new byte[csqLen];
        if (bis.read(csq) != csqLen) throw new IOException("Error occurs while reading");

        MapDeCompressor mdc = new MapDeCompressor(csq);
        int huffmanBlockSize = Math.min(huffmanBlockMaxSize, windowSize);

        byte[] rlcMain = mdc.Uncompress((int) (260 * (fileLength / huffmanBlockSize + 1) * 8 + 259), false);
        // 259 for each map, 1 for EOF character
        byte[] zeroRlc = new ZeroRLCDecoderByte(rlcMain).Decode();
        byte[] bwtMap = new MTFInverseByte(zeroRlc).Inverse();
        byte[] fullMap = new BWTDecoderByte(bwtMap, origRowIndex).Decode();
        byte[] flagsMap = new byte[259];
        byte[] mainMap = new byte[fullMap.length - 259];
        System.arraycopy(fullMap, 0, flagsMap, 0, 259);
        System.arraycopy(fullMap, 259, mainMap, 0, mainMap.length);
        copyToTemp();

        LongHuffmanDeCompressorRam flagsDec =
                new LongHuffmanDeCompressorRam(cmpFlags, 259, cmpFlags.length * 8);
        short[] flagsDeHuf = flagsDec.uncompress(flagsMap, (short) 258);
        short[] flagsRld =
                new ZeroRLCDecoder(flagsDeHuf, (int) fileLength * 32 / huffmanBlockSize + 256).Decode();
        short[] flagsMTR = new MTFInverse(flagsRld).decode();
        byte[] flags = new BWTDecoder(flagsMTR).Decode();

        int fIndex = 0;
        int i = 0;
        while (i < mainMap.length) {
            int flag = flags[fIndex++] & 0xff;
            byte[] map = new byte[BWZCompressor.huffmanTableSize];
            if (flag == 0) {
                System.arraycopy(mainMap, i, map, 0, BWZCompressor.huffmanTableSize);
                i += BWZCompressor.huffmanTableSize;
            } else {
                int x = huffmanMaps.size() - flag;
                System.arraycopy(huffmanMaps.get(x), 0, map, 0, BWZCompressor.huffmanTableSize);
            }
            huffmanMaps.addLast(map);
        }
    }

    private void copyToTemp() throws IOException {
        Util.fileTruncate(bis, cmpMainName, readBufferSize, mainLen);
    }

    private void decode(OutputStream out, FileChannel fc) throws Exception {
        long lastCheckTime = System.currentTimeMillis();
        long currentTime;

        int huffmanBlockForEachWindow;
        if (windowSize <= huffmanBlockMaxSize) huffmanBlockForEachWindow = 1;
        else huffmanBlockForEachWindow = windowSize / huffmanBlockMaxSize;

        ArrayList<short[]> blockList = new ArrayList<>();
        ArrayList<short[]> huffmanBlockList = new ArrayList<>();

        LongHuffmanInputStream his =
                new LongHuffmanInputStream(fc, BWZCompressor.huffmanTableSize, huffmanBlockMaxSize + 256);
        short[] huffmanResult;

        while (!huffmanMaps.isEmpty()) {
            huffmanResult = his.read(huffmanMaps.removeFirst(), BWZCompressor.huffmanEndSig);

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

                blockList.add(concatenateHuffman);
                huffmanBlockList.clear();

                // Got enough blocks to start multi-threading, or reaches the end of file.
                if (blockList.size() == threadNum || huffmanMaps.isEmpty()) {
                    ExecutorService es = Executors.newCachedThreadPool();
                    DecodeThread threads[] = new DecodeThread[blockList.size()];
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
        } catch (Exception e) {
            fc.close();
            throw e;
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
    private int windowSize;
    private BWZDeCompressor parent;

    DecodeThread(short[] text, int windowSize, BWZDeCompressor parent) {
        this.text = text;
        this.windowSize = windowSize;
        this.parent = parent;
    }

    @Override
    public void run() {
        short[] rld = new ZeroRLCDecoder(text, windowSize + 4).Decode();
        parent.pos += rld.length / 2;
        if (parent.parent != null) parent.parent.progress.set(parent.pos);
        short[] mtf = new MTFInverse(rld).decode();
        result = new BWTDecoder(mtf).Decode();
        parent.pos = parent.pos - rld.length / 2 + result.length;
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
