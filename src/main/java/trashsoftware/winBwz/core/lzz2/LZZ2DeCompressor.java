package trashsoftware.winBwz.core.lzz2;

import trashsoftware.winBwz.core.DeCompressor;
import trashsoftware.winBwz.core.RegularDeCompressor;
import trashsoftware.winBwz.core.bwz.MTFInverse;
import trashsoftware.winBwz.core.bwz.ZeroRLCDecoder;
import trashsoftware.winBwz.huffman.HuffmanCompressorTwoBytes;
import trashsoftware.winBwz.huffman.MapCompressor.MapDeCompressor;
import trashsoftware.winBwz.packer.pz.PzUnPacker;
import trashsoftware.winBwz.utility.FileBitInputStream;
import trashsoftware.winBwz.utility.IndexedOutputStream;
import trashsoftware.winBwz.utility.Util;

import java.io.*;

import static trashsoftware.winBwz.core.lzz2.LZZ2Compressor.*;

/**
 * LZZ2-algorithm decompressor, implements {@code DeCompressor} interface.
 *
 * @author zbh
 * @see DeCompressor
 * @since 0.4
 */
public class LZZ2DeCompressor extends RegularDeCompressor {

    private final static int readBufferSize = 8192;
    private final String inFile;
    private final BufferedInputStream bis;
    private final int disHeadLen;
    private final int mainLen;
    private final int csqLen;
    private final int windowSize;
    private int dlbLen;
    private String dlBodyTempName;

    private String cmpMainTempName, cmpDisHeadTempName;

    private int[] mainMap;
    private int[] disHeadMap;

    public LZZ2DeCompressor(String inFile, int windowSize) throws IOException {
        this.inFile = inFile;
        File f = new File(inFile);
        int length = (int) f.length();
        int sizeBlockSize;
        try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
            raf.seek(length - 1);
            this.windowSize = windowSize;

            sizeBlockSize = raf.readByte() & 0xff;
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
        }

        dlbLen = length - disHeadLen - dlbLen - csqLen - sizeBlockSize - 1;
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

        IndexedOutputStream tempResult = new IndexedOutputStream(fos, windowSize);

        int[] lastDistances = new int[4];
        int lastDisIndex = -1;
        int lastLen = -1;

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
                outPosition = tempResult.getIndex();
                if (unPacker != null && unPacker.isInterrupted) break;
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
    public void setUnPacker(PzUnPacker unPacker) {
        this.unPacker = unPacker;
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
}
