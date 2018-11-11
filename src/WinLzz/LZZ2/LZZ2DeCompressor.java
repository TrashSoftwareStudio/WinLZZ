package WinLzz.LZZ2;

import WinLzz.BWZ.MTFInverseByte;
import WinLzz.BWZ.ZeroRLCDecoderByte;
import WinLzz.Huffman.HuffmanDeCompressor;
import WinLzz.Huffman.MapCompressor.MapDeCompressor;
import WinLzz.Interface.DeCompressor;
import WinLzz.Utility.FileBitInputStream;
import WinLzz.Utility.FileOutputBufferArray;
import WinLzz.LZZ2.Util.LZZ2Util;
import WinLzz.Packer.UnPacker;
import WinLzz.Utility.Util;

import java.io.*;
import java.util.LinkedList;

/**
 * LZZ2-algorithm decompressor, implements {@code DeCompressor} interface.
 *
 * @author zbh
 * @see WinLzz.Interface.DeCompressor
 * @since 0.4
 */
public class LZZ2DeCompressor implements DeCompressor {

    private String inFile;

    private BufferedInputStream bis;

    private final static int readBufferSize = 8192;

    private int disHeadLen, lenHeadLen, flagLen, dlbLen, mainLen, csqLen;

    private int windowSize;

    private String mainTempName, lenHeadTempName, disHeadTempName, flagTempName, dlBodyTempName;

    private String cmpMainTempName, cmpLenHeadTempName, cmpDisHeadTempName, cmpFlagTempName;

    private UnPacker parent;

    private int timeAccumulator;

    private long lastUpdateProgress;

    private long startTime;

    private long timeOffset;

    public LZZ2DeCompressor(String inFile, int windowSize) throws IOException {
        this.inFile = inFile;
        File f = new File(inFile);
        int length = (int) f.length();
        RandomAccessFile raf = new RandomAccessFile(f, "r");
        raf.seek(length - 1);
        this.windowSize = windowSize;

        int sizeBlockSize = raf.readByte() & 0xff;
        if (sizeBlockSize == 0) {
            csqLen = 0;
            disHeadLen = 0;
            lenHeadLen = 0;
            flagLen = 0;
            dlbLen = 0;
            mainLen = length - 1;
            raf.close();
            this.bis = new BufferedInputStream(new FileInputStream(inFile));
            return;
        }
        byte[] block = new byte[sizeBlockSize];
        raf.seek(length - sizeBlockSize - 1);
        raf.read(block);

        long[] sizes = Util.recoverSizeBlock(block, 5);
        csqLen = (int) sizes[0];
        disHeadLen = (int) sizes[1];
        lenHeadLen = (int) sizes[2];
        flagLen = (int) sizes[3];
        dlbLen = (int) sizes[4];
        raf.close();

        mainLen = length - disHeadLen - lenHeadLen - flagLen - dlbLen - csqLen  - sizeBlockSize - 1;
        this.bis = new BufferedInputStream(new FileInputStream(inFile));
    }

    private void generateTempNames() {
        this.mainTempName = inFile + ".main.temp";
        this.lenHeadTempName = inFile + ".len.temp";
        this.disHeadTempName = inFile + ".dis.temp";
        this.flagTempName = inFile + ".flag.temp";
        this.dlBodyTempName = inFile + ".dlb.temp";

        this.cmpMainTempName = inFile + ".main.cmp";
        this.cmpLenHeadTempName = inFile + ".len.cmp";
        this.cmpDisHeadTempName = inFile + ".dis.cmp";
        this.cmpFlagTempName = inFile + ".flag.cmp";
    }

    private void copyToTemp() throws IOException {
        Util.fileTruncate(bis, cmpDisHeadTempName, readBufferSize, disHeadLen);
        Util.fileTruncate(bis, cmpLenHeadTempName, readBufferSize, lenHeadLen);
        Util.fileTruncate(bis, cmpFlagTempName, readBufferSize, flagLen);
        Util.fileTruncate(bis, dlBodyTempName, readBufferSize, dlbLen);
        Util.fileTruncate(bis, cmpMainTempName, readBufferSize, mainLen);
    }

    private void uncompressHead() throws IOException {
        byte[] csq = new byte[csqLen];
        if (bis.read(csq) != csqLen) throw new IOException("Error occurs while reading");
        MapDeCompressor mdc = new MapDeCompressor(csq);
        byte[] rlcMain = mdc.Uncompress(1024, false);

        byte[] rlc = new ZeroRLCDecoderByte(rlcMain).Decode();
        byte[] totalMap = new MTFInverseByte(rlc).Inverse(18);

        byte[] dhdMap = new byte[64];
        byte[] lhdMap = new byte[32];
        byte[] fdMap = new byte[256];
        byte[] mainMap = new byte[256];

        System.arraycopy(totalMap, 0, dhdMap, 0, 64);
        System.arraycopy(totalMap, 64, lhdMap, 0, 32);
        System.arraycopy(totalMap, 96, fdMap, 0, 256);
        System.arraycopy(totalMap, 352, mainMap, 0, 256);

        copyToTemp();

        new HuffmanDeCompressor(cmpDisHeadTempName).Uncompress(disHeadTempName, dhdMap);
        new HuffmanDeCompressor(cmpLenHeadTempName).Uncompress(lenHeadTempName, lhdMap);
        new HuffmanDeCompressor(cmpFlagTempName).Uncompress(flagTempName, fdMap);
        new HuffmanDeCompressor(cmpMainTempName).Uncompress(mainTempName, mainMap);
    }

    private void deleteCmpTemp() {
        Util.deleteFile(cmpDisHeadTempName);
        Util.deleteFile(cmpLenHeadTempName);
        Util.deleteFile(cmpFlagTempName);
        Util.deleteFile(cmpMainTempName);
    }

    private void deleteTemp() {
        Util.deleteFile(disHeadTempName);
        Util.deleteFile(lenHeadTempName);
        Util.deleteFile(dlBodyTempName);
        Util.deleteFile(flagTempName);
        Util.deleteFile(mainTempName);
    }

    private void uncompressMain(OutputStream fos) throws IOException {
        FileBitInputStream flagBis = new FileBitInputStream(new BufferedInputStream(new FileInputStream(flagTempName)));
        BufferedInputStream disHeadBis = new BufferedInputStream(new FileInputStream(disHeadTempName));
        BufferedInputStream lenHeadBis = new BufferedInputStream(new FileInputStream(lenHeadTempName));
        FileBitInputStream dlbBis = new FileBitInputStream(new BufferedInputStream(new FileInputStream(dlBodyTempName)));
        BufferedInputStream mainBis = new BufferedInputStream(new FileInputStream(mainTempName));

        FileOutputBufferArray tempResult = new FileOutputBufferArray(fos, windowSize);

        LinkedList<Integer> lastDistances = new LinkedList<>();
        int lastLen = -1;

        long lastCheckTime = System.currentTimeMillis();
        startTime = lastCheckTime;
        if (parent != null) timeOffset = lastCheckTime - parent.startTime;
        long currentTime;

        try {
            while (true) {
                int s;
                s = flagBis.read();
                if (s == 2) {
                    break;
                } else if (s == 0) {
                    byte[] b = new byte[1];
                    if (mainBis.read(b) != 1) break;
                    tempResult.write(b[0]);
                } else {
                    int distance;
                    int length;
                    byte[] disCodes = new byte[1];
                    if (disHeadBis.read(disCodes) != 1) break;
                    int disCodeRep = disCodes[0] & 0xff;
                    if (disCodeRep == 4) {
                        distance = lastDistances.getFirst();
                        length = lastLen;
                    } else {
                        byte[] lenCodes = new byte[1];
                        if (lenHeadBis.read(lenCodes) != 1) throw new IOException("Cannot read length head");
                        int lenCodeRep = lenCodes[0] & 0xff;
                        if (disCodeRep < 4) distance = lastDistances.get(disCodeRep);
                        else distance = LZZ2Util.recoverDistance(disCodeRep, dlbBis);
                        length = LZZ2Util.recoverLength(lenCodeRep, dlbBis) + LZZ2Compressor.minimumMatchLen;
                    }
                    lastDistances.addFirst(distance);
                    lastLen = length;
                    if (lastDistances.size() > 4) lastDistances.removeLast();

                    long index = tempResult.getIndex();
                    long from = index - distance;
                    long to = from + length;
                    if (to <= index) {
                        byte[] repeat = tempResult.subSequence(from, to);
                        for (byte b : repeat) tempResult.write(b);
                    } else {
                        byte[] overlapRepeat = new byte[length];
                        int overlap = (int) (index - from);
                        byte[] repeat = tempResult.subSequence(from, index);
                        int p = 0;
                        while (p < length) {
                            System.arraycopy(repeat, 0, overlapRepeat, p, Math.min(overlap, length - p));
                            p += overlap;
                        }
                        for (byte b : overlapRepeat) tempResult.write(b);
                    }
                }
                if (parent != null && parent.isInterrupted) break;
                if (parent != null && (currentTime = System.currentTimeMillis()) - lastCheckTime >= 50) {
                    updateInfo(tempResult.getIndex(), currentTime);
                    lastCheckTime = currentTime;
                }
            }
        } catch (Exception e) {
            tempResult.flush();
            tempResult.close();
            flagBis.close();
            disHeadBis.close();
            lenHeadBis.close();
            dlbBis.close();
            mainBis.close();
            throw e;
        }
        tempResult.flush();
        tempResult.close();
        flagBis.close();
        disHeadBis.close();
        lenHeadBis.close();
        dlbBis.close();
        mainBis.close();
    }

    @Override
    public void deleteCache() {
        deleteCmpTemp();
        deleteTemp();
    }

    @Override
    public void setParent(UnPacker parent) {
        this.parent = parent;
    }

    @Override
    public void setThreads(int threads) {
    }

    @Override
    public void uncompress(OutputStream outFile) throws IOException {
        generateTempNames();
        if (disHeadLen == 0) {
            Util.fileTruncate(bis, outFile, 8192, mainLen);
            bis.close();
            return;
        }
        uncompressHead();
        bis.close();
        deleteCmpTemp();
        uncompressMain(outFile);
        deleteTemp();
    }

    private void updateInfo(long current, long updateTime) {
        parent.progress.set(current);
        if (timeAccumulator == 19) {
            timeAccumulator = 0;
            double finished = ((double) current) / parent.getTotalOrigSize();
            double rounded = (double) Math.round(finished * 1000) / 10;
            parent.percentage.set(String.valueOf(rounded));
            int newUpdated = (int) (current - lastUpdateProgress);
            lastUpdateProgress = parent.progress.get();
            int ratio = newUpdated / 1024;
            parent.ratio.set(String.valueOf(ratio));

            long timeUsed = updateTime - startTime;
            parent.timeUsed.set(Util.secondToString((timeUsed + timeOffset) / 1000));
            long expectTime = (parent.getTotalOrigSize() - current) / ratio / 1024;
            parent.timeExpected.set(Util.secondToString(expectTime));

            parent.passedLength.set(Util.sizeToReadable(current));
        } else {
            timeAccumulator += 1;
        }
    }
}
