package trashsoftware.win_bwz.core.lzz2;

import trashsoftware.win_bwz.core.bwz.MTFInverse;
import trashsoftware.win_bwz.core.bwz.ZeroRLCDecoder;
import trashsoftware.win_bwz.core.DeCompressor;
import trashsoftware.win_bwz.huffman.HuffmanCompressorTwoBytes;
import trashsoftware.win_bwz.huffman.MapCompressor.MapDeCompressor;
import trashsoftware.win_bwz.packer.UnPacker;
import trashsoftware.win_bwz.utility.FileBitInputStream;
import trashsoftware.win_bwz.utility.IndexedOutputStream;
import trashsoftware.win_bwz.utility.Util;

import java.io.*;

import static trashsoftware.win_bwz.core.lzz2.LZZ2Compressor.*;

/**
 * LZZ2-algorithm decompressor, implements {@code DeCompressor} interface.
 *
 * @author zbh
 * @see DeCompressor
 * @since 0.4
 */
public class LZZ2DeCompressor implements DeCompressor {

    private String inFile;

    private BufferedInputStream bis;

    private final static int readBufferSize = 8192;

    private int disHeadLen, dlbLen, mainLen, csqLen;

    private int windowSize;

    private String dlBodyTempName;

    private String cmpMainTempName, cmpDisHeadTempName;

    private UnPacker parent;

    private int timeAccumulator;

    private long lastUpdateProgress;

    private long startTime;

    private long timeOffset;

    private int[] mainMap;
    private int[] disHeadMap;

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
            dlbLen = 0;
            mainLen = length - 1;
            raf.close();
            this.bis = new BufferedInputStream(new FileInputStream(inFile));
            return;
        }
        byte[] block = new byte[sizeBlockSize];
        raf.seek(length - sizeBlockSize - 1);
        raf.read(block);

        long[] sizes = Util.recoverSizeBlock(block, 3);
        csqLen = (int) sizes[0];
        mainLen = (int) sizes[1];
        disHeadLen = (int) sizes[2];
        raf.close();

        dlbLen = length - disHeadLen - dlbLen - csqLen  - sizeBlockSize - 1;
        this.bis = new BufferedInputStream(new FileInputStream(inFile));
    }

    private void generateTempNames() {
        this.dlBodyTempName = inFile + ".dlb.temp";
        this.cmpMainTempName = inFile + ".main.cmp";
        this.cmpDisHeadTempName = inFile + ".dis.cmp";
    }

    private void copyToTemp() throws IOException {
        Util.fileTruncate(bis, cmpMainTempName, readBufferSize, mainLen);
        Util.fileTruncate(bis, cmpDisHeadTempName, readBufferSize, disHeadLen);
        Util.fileTruncate(bis, dlBodyTempName, readBufferSize, dlbLen);
    }

    private void uncompressHead() throws IOException {
        byte[] csq = new byte[csqLen];
        if (bis.read(csq) != csqLen) throw new IOException("Error occurs while reading");
        MapDeCompressor mdc = new MapDeCompressor(csq);
        byte[] rlcMain = mdc.Uncompress(1024, LZZ2_HUF_HEAD_ALPHABET);

        int[] rlcMainInt = new int[rlcMain.length];
        for (int i = 0; i < rlcMain.length; ++i) rlcMainInt[i] = rlcMain[i] & 0xff;

        int[] rlc = new ZeroRLCDecoder(rlcMainInt, MAIN_HUF_ALPHABET + 64).Decode();
        int[] totalMap = new MTFInverse(rlc).decode(LZZ2_HUF_HEAD_ALPHABET - 1);
//        byte[] rlc = new ZeroRLCDecoderByte(rlcMain).Decode();
//        byte[] totalMap = new MTFInverseByte(rlc).Inverse(18);

        disHeadMap = new int[64];
        mainMap = new int[MAIN_HUF_ALPHABET];

        System.arraycopy(totalMap, 0, disHeadMap, 0, 64);
        System.arraycopy(totalMap, 64, mainMap, 0, MAIN_HUF_ALPHABET);

//        System.out.println(Arrays.toString(disHeadMap));

        copyToTemp();
    }

    private void deleteTemp() {
        Util.deleteFile(dlBodyTempName);
        Util.deleteFile(cmpDisHeadTempName);
        Util.deleteFile(cmpMainTempName);
    }

    private void uncompressMain(OutputStream fos) throws IOException {

        BufferedInputStream disHeadBis = new BufferedInputStream(new FileInputStream(cmpDisHeadTempName));
        FileBitInputStream dlbBis = new FileBitInputStream(new BufferedInputStream(new FileInputStream(dlBodyTempName)));
        BufferedInputStream mainBis = new BufferedInputStream(new FileInputStream(cmpMainTempName));

        Lzz2HuffmanInputStream mainHis = new Lzz2HuffmanInputStream(mainMap, mainBis);
        Lzz2HuffmanInputStream disHeadHis = new Lzz2HuffmanInputStream(disHeadMap, disHeadBis);

//        FileOutputBufferArray tempResult = new FileOutputBufferArray(fos, windowSize);
        IndexedOutputStream tempResult = new IndexedOutputStream(fos, windowSize);

        int[] lastDistances = new int[4];
        int lastDisIndex = -1;
        int lastLen = -1;

        long lastCheckTime = System.currentTimeMillis();
        startTime = lastCheckTime;
        if (parent != null) timeOffset = startTime - parent.startTime;
        long currentTime;

        try {
            while (true) {
                int s = mainHis.readNext();
//                System.out.print(s + ", ");
                if (s == HuffmanCompressorTwoBytes.END_SIG) {
                    break;
                } else if (s < 256) {
//                    System.out.print(s + ", ");
//                    tempResult.write((byte) s);
                    tempResult.writeOne((byte) s);
                } else if (s == 285) {
                    int dis = lastDistances[lastDisIndex & 0b11];
                    recoverMatch(tempResult, dis, lastLen);
                    lastDistances[(++lastDisIndex) & 0b11] = dis;
                } else {
                    int disHead = disHeadHis.readNext();
                    int length = LZZ2Util.recoverLength(s - 257, dlbBis) + MINIMUM_MATCH_LEN;
                    int distance;
                    if (disHead < 4) {
                        distance = lastDistances[(lastDisIndex - disHead) & 0b11];
                    } else {
                        distance = LZZ2Util.recoverDistance(disHead, dlbBis);
                    }

                    lastDistances[(++lastDisIndex) & 0b11] = distance;
                    lastLen = length;

                    recoverMatch(tempResult, distance, length);
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
            disHeadHis.close();
            dlbBis.close();
            mainHis.close();
            throw e;
        }
        tempResult.flush();
        tempResult.close();
        disHeadHis.close();
        dlbBis.close();
        mainHis.close();
    }

    private void recoverMatch(IndexedOutputStream ios, int distance, int length) throws IOException {
        long index = ios.getIndex();
        long from = index - distance;
        long to = from + length;
        if (to <= index) {
            ios.copyRepeat(from, length);
        } else {
            int p = 0;
            int overlap = (int) (index - from);
            while (p < length) {
                ios.copyRepeat(from + p, Math.min(overlap, length - p));
                p += overlap;
            }
        }
    }

    @Override
    public void deleteCache() {
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
//        deleteCmpTemp();
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
