package trashsoftware.winBwz.core.lzz2;

import trashsoftware.winBwz.core.Compressor;
import trashsoftware.winBwz.core.Constants;
import trashsoftware.winBwz.core.bwz.MTFTransform;
import trashsoftware.winBwz.huffman.HuffmanCompressor;
import trashsoftware.winBwz.huffman.HuffmanCompressorBase;
import trashsoftware.winBwz.huffman.HuffmanCompressorTwoBytes;
import trashsoftware.winBwz.huffman.MapCompressor.MapCompressor;
import trashsoftware.winBwz.longHuffman.LongHuffmanUtil;
import trashsoftware.winBwz.packer.Packer;
import trashsoftware.winBwz.utility.FileBitOutputStream;
import trashsoftware.winBwz.utility.FileInputBufferArray;
import trashsoftware.winBwz.utility.MultipleInputStream;
import trashsoftware.winBwz.utility.Util;

import java.io.*;
import java.util.Timer;
import java.util.TimerTask;

/**
 * LZZ2 (Lempel-Ziv-ZBH 2) compressor, implements {@code Compressor} interface.
 * <p>
 * An improved version of LZ77 algorithm, implemented by Bohan Zhang (zbh).
 *
 * @author zbh
 * @see Compressor
 * @since 0.4
 */
public class LZZ2Compressor implements Compressor {

    public static final int VERSION = 2;

    public static final int MAIN_HUF_ALPHABET = 286;

    final static int MINIMUM_MATCH_LEN = 4;

    public static final int MAXIMUM_LENGTH = 286 + MINIMUM_MATCH_LEN;

    static final int LZZ2_HUF_HEAD_ALPHABET = 19;

//    private static final long PRIME16 = 40499;

    private final InputStream sis;
    private final int windowSize;  // Size of sliding window.
    protected long totalLength;
    protected String mainTempName, lenHeadTempName, disHeadTempName, flagTempName, dlBodyTempName;
    protected long cmpSize;
    protected int itemCount;
    protected Packer packer;
    private int bufferMaxSize;  // Size of LAB (Look ahead buffer).
    private int dictSize;
    /**
     * Time used by any launching process before this {@code LZZ2Compressor} starts.
     */
    private long timeOffset;

    private long lastUpdateProgress;

    private int compressionLevel;

    private long position = 0;  // The processing index. i.e. the border of buffer and slider.

    /**
     * Constructor of a new {@code LZZ2Compressor} instance.
     *
     * @param inFile     name of file to compress.
     * @param windowSize total sliding window size.
     * @param bufferSize size of look ahead buffer.
     * @throws IOException if error occurs during file reading or writing.
     */
    public LZZ2Compressor(String inFile, int windowSize, int bufferSize) throws IOException {
        this.windowSize = windowSize;
        this.bufferMaxSize = bufferSize + MINIMUM_MATCH_LEN + 1;
        this.dictSize = windowSize - bufferMaxSize - 1;

        this.totalLength = new File(inFile).length();
        this.sis = new FileInputStream(inFile);
        setTempNames(inFile);
    }

    /**
     * Constructor of a new {@code LZZ2Compressor} instance.
     *
     * @param mis         the input stream
     * @param windowSize  total sliding window size.
     * @param bufferSize  size of look ahead buffer.
     * @param totalLength the total length of the files to be compressed
     */
    public LZZ2Compressor(MultipleInputStream mis, int windowSize, int bufferSize, long totalLength) {
        this.windowSize = windowSize;
        this.bufferMaxSize = bufferSize + MINIMUM_MATCH_LEN + 1;
        this.dictSize = windowSize - bufferMaxSize - 1;
        this.totalLength = totalLength;
        this.sis = mis;
        setTempNames("lzz2");
    }

    private static int sliderArraySize(int dictSize, int compressionLevel) {
        int base;
        if (dictSize <= 8192) base = 16;
        else if (dictSize <= 16384) base = 32;
        else if (dictSize <= 32768) base = 64;
        else if (dictSize <= 65536) base = 128;
        else if (dictSize <= 131072) base = 256;
        else if (dictSize <= 524288) base = 512;
        else base = 1024;

        if (compressionLevel == 0) {
            return 8;
        } else if (compressionLevel < 3) {
            return base;
        } else {
            return base * 2;
        }
    }

    public static long[] estimateMemoryUsage(int threads, int windowSize, int modeLevel) {
        long cmpMem = 2048;  // objects
        cmpMem += windowSize * 2L;  // input buffer array
        long sliderArraySize = sliderArraySize(windowSize, modeLevel);
        cmpMem += sliderArraySize * 8;  // temp array
        long sliderMem = sliderArraySize * 65536 * 12;  // 12 is size of long + int
        cmpMem += sliderMem;
        cmpMem += 65536 * 32;  // FixedArrayDeque
        cmpMem += 16384;  // estimate mtf, huf

        long uncMem = 2048;  // objects
        uncMem += LongHuffmanUtil.hufDecompressorMem();  // huffman dec
        uncMem += 500;  // heads
        uncMem += windowSize * 2L;  // output buffer
        uncMem += 16384;  // others

        return new long[]{cmpMem, uncMem};
    }

    private void setTempNames(String inFile) {
        this.mainTempName = inFile + ".main.temp";
        this.lenHeadTempName = inFile + ".len.temp";
        this.disHeadTempName = inFile + ".dis.temp";
        this.flagTempName = inFile + ".flag.temp";
        this.dlBodyTempName = inFile + ".dlb.temp";
    }

    private void setUpWindow() {
        if (totalLength <= bufferMaxSize) {
            dictSize = (int) totalLength - 1;
            bufferMaxSize = (int) totalLength - 1;
        }
    }

//    public static void printStream(String name) throws IOException {
//        BufferedInputStream bbb = new BufferedInputStream(new FileInputStream(name));
//        byte[] buf = new byte[1];
//        while (bbb.read(buf) > 0) {
//            System.out.print((buf[0] & 0xff) + " ");
//        }
//        System.out.println();
//        bbb.close();
//    }

//    private static int hash(byte b0, byte b1) {
//        return (b0 & 0xff) << 8 | (b1 & 0xff);
//    }

//    private static int hash4bytesToInt16(byte b0, byte b1, byte b2, byte b3) {
//        long first = (long) (b0 & 0xff) << 24 | (b1 & 0xff) << 16 | (b2 & 0xff) << 8 | (b3 & 0xff);
//        long hash = (first >> 16) ^ ((first & 0xffff) * PRIME16);
//        return (int) (hash & 0xffff);
//    }

    protected void compressText() throws IOException {
        FileInputBufferArray fba = new FileInputBufferArray(sis, totalLength, windowSize);

        setUpWindow();

        int sliderArraySize = sliderArraySize(dictSize, compressionLevel);

        Lzz2Matcher matcher;
        if (compressionLevel < 2) {
            matcher = new GreedyMatcher(sliderArraySize, dictSize, bufferMaxSize, totalLength);
        } else if (compressionLevel < 4) {
            matcher = new NonGreedyMatcherOneStep(sliderArraySize, dictSize, bufferMaxSize, totalLength);
        } else {
            matcher = new NonGreedyMatcherMultiStep(sliderArraySize, dictSize, bufferMaxSize, totalLength);
        }

        int[] lastDistances = new int[4];
        int lastDisIndex = 0;
        int lastLength = -1;

        BufferedOutputStream mainFos = new BufferedOutputStream(new FileOutputStream(mainTempName));
        BufferedOutputStream disFos = new BufferedOutputStream(new FileOutputStream(disHeadTempName));
        FileBitOutputStream dlbFos = new FileBitOutputStream(
                new BufferedOutputStream(new FileOutputStream(dlBodyTempName)));

//        long lastCheckTime = System.currentTimeMillis();
//        startTime = lastCheckTime;
        if (packer != null) timeOffset = System.currentTimeMillis() - packer.startTime;
//        long currentTime;

        Timer timer = null;
        if (packer != null) {
            timer = new Timer();
            timer.scheduleAtFixedRate(new CompTimerTask(), 0, 1000 / Constants.LZZ_GUI_UPDATES_PER_S);
        }

        while (true) {

            int skip = matcher.search(fba, position);
            long prevPos = position;

            int len = matcher.getLength();
            int dis = matcher.getDistance();

            if (len < MINIMUM_MATCH_LEN) {
                // Not a match
//                System.out.print(fba.getByte(position) + ", ");
                mainFos.write(0);
                mainFos.write(fba.getByte(position++));  // a literal
            } else {
                // A match.
                for (int j = 0; j < skip; ++j) {
//                    System.out.print(fba.getByte(position) + ", ");
                    mainFos.write(0);
                    mainFos.write(fba.getByte(position++));
                }
//                System.out.print(dis + " " + len + "; ");

                int findInLast =
                        compressionLevel > 0 ? reverseIndexInQueue(lastDistances, lastDisIndex, dis) : -1;
                if (findInLast == -1) {
                    LZZ2Util.addLength(len, MINIMUM_MATCH_LEN, mainFos, dlbFos);  // Length first.
                    LZZ2Util.addDistance(dis, 0, disFos, dlbFos);
                } else {
                    if (findInLast == 0 && lastLength == len) {
                        mainFos.write(1);
                        mainFos.write(29);  // 28 is the last length head
                    } else {
                        LZZ2Util.addLength(len, MINIMUM_MATCH_LEN, mainFos, dlbFos);
                        disFos.write((byte) findInLast);
                    }
                }
                itemCount += 1;
                position += len;
                lastLength = len;
                lastDistances[(lastDisIndex++) & 0b11] = dis;
            }

            if (position >= totalLength - MINIMUM_MATCH_LEN) break;
            matcher.fillSlider(prevPos, position, fba);

            if (packer != null && packer.isInterrupted) break;
        }

        if (packer == null || !packer.isInterrupted) {
            for (; position < totalLength; ++position) {
                mainFos.write(0);
                mainFos.write(fba.getByte(position));
            }
        }

        if (timer != null) timer.cancel();

//        System.out.println();

        mainFos.flush();
        mainFos.close();
        disFos.flush();
        disFos.close();
        dlbFos.flush();
        dlbFos.close();
        fba.close();

//        printStream(flagTempName);
//        printStream(mainTempName);
//        printStream(disHeadTempName);
//        printStream(lenHeadTempName);
    }

    private int reverseIndexInQueue(int[] lastDistances, int index, int target) {
        int beginIndex = index < 4 ? 0 : index - 4;
        for (int i = beginIndex; i < index; ++i) {
            if (lastDistances[i & 0b11] == target) {
                return index - i - 1;
            }
        }
        return -1;
    }

//    private void updateInfo(long current, long updateTime) {
//        packer.progress.set(current);
//        if (timeAccumulator == 19) {
//            timeAccumulator = 0;
//            double finished = ((double) current) / totalLength;
//            double rounded = (double) Math.round(finished * 1000) / 10;
//            packer.percentage.set(String.valueOf(rounded));
//            int newUpdated = (int) (current - lastUpdateProgress);
//            lastUpdateProgress = packer.progress.get();
//            int ratio = newUpdated / 1024;
//            packer.ratio.set(String.valueOf(ratio));
//
//            long timeUsed = updateTime - startTime;
//            packer.timeUsed.set(Util.secondToString((timeUsed + timeOffset) / 1000));
//            long expectTime = (totalLength - current) / ratio / 1024;
//            packer.timeExpected.set(Util.secondToString(expectTime));
//
//            packer.passedLength.set(Util.sizeToReadable(current));
//        } else {
//            timeAccumulator += 1;
//        }
//    }

    protected void deleteTemp() {
        Util.deleteFile(mainTempName);
        Util.deleteFile(disHeadTempName);
        Util.deleteFile(lenHeadTempName);
        Util.deleteFile(flagTempName);
        Util.deleteFile(dlBodyTempName);
    }

    protected boolean isNotCompressible(OutputStream outFile) throws IOException {
        if (itemCount == 0) {
            Util.fileConcatenate(outFile, new String[]{mainTempName}, 8192);
            outFile.write((byte) 0);
            cmpSize = (int) totalLength + 1;
            deleteTemp();
            return true;
        }

        if (packer != null && packer.isInterrupted) {
            deleteTemp();
            return true;
        }

        return false;
    }

    /**
     * compress file into output stream.
     *
     * @param outFile the target output stream.
     * @throws IOException if io error occurs during compression.
     */
    @Override
    public void compress(OutputStream outFile) throws IOException {
        compressText();

        if (isNotCompressible(outFile)) return;

        HuffmanCompressorBase dhc = new HuffmanCompressor(disHeadTempName);
        int[] dhcMap = dhc.getMap(64);

        HuffmanCompressorBase mtc = new HuffmanCompressorTwoBytes(mainTempName);
        int[] mtcMap = mtc.getMap(MAIN_HUF_ALPHABET);

//        System.out.println(Arrays.toString(dhcMap));

        int[] totalMap = new int[MAIN_HUF_ALPHABET + 64];
        System.arraycopy(dhcMap, 0, totalMap, 0, 64);
        System.arraycopy(mtcMap, 0, totalMap, 64, MAIN_HUF_ALPHABET);

//        byte[] rlcMain = new MTFTransformByte(totalMap).Transform(18);
        int[] rlcMain = new MTFTransform(totalMap).Transform(LZZ2_HUF_HEAD_ALPHABET - 1);

        MapCompressor mc = new MapCompressor(rlcMain);
        byte[] csq = mc.Compress(LZZ2_HUF_HEAD_ALPHABET);

        outFile.write(csq);

        mtc.SepCompress(outFile);
        long mainLen = mtc.getCompressedLength();

        dhc.SepCompress(outFile);
        long disHeadLen = dhc.getCompressedLength();

        long dlbLen = Util.fileConcatenate(outFile, new String[]{dlBodyTempName}, 8192);

        deleteTemp();

        int csqLen = csq.length;
        long[] sizes = new long[]{csqLen, mainLen, disHeadLen};
        byte[] sizeBlock = Util.generateSizeBlock(sizes);
        outFile.write(sizeBlock);
        cmpSize = disHeadLen + dlbLen + mainLen + csqLen + sizeBlock.length;
    }

    /**
     * Returns the total output size after compressing.
     *
     * @return size after compressed.
     */
    @Override
    public long getCompressedSize() {
        return cmpSize;
    }

    public long getSizeBeforeCompression() {
        return totalLength;
    }

    @Override
    public void setPacker(Packer packer) {
        this.packer = packer;
    }

    @Override
    public void setThreads(int threads) {
    }

    @Override
    public void setCompressionLevel(int compressionLevel) {
        this.compressionLevel = compressionLevel;
    }

    class CompTimerTask extends TimerTask {
        private int accumulator;

        @Override
        public void run() {
            packer.progress.set(position);
            accumulator++;
            if (accumulator % Constants.LZZ_GUI_UPDATES_PER_S == 0) {  // whole second
                double finished = ((double) position) / totalLength;
                double rounded = (double) Math.round(finished * 1000) / 10;
                packer.percentage.set(String.valueOf(rounded));
                int newUpdated = (int) (position - lastUpdateProgress);
                lastUpdateProgress = packer.progress.get();
                int ratio = newUpdated / 1024;
                packer.ratio.set(String.valueOf(ratio));

                long timeUsed = accumulator * 1000L / Constants.LZZ_GUI_UPDATES_PER_S;
                packer.timeUsed.set(Util.secondToString((timeUsed + timeOffset) / 1000));
                long expectTime = (totalLength - position) / ratio / 1024;
                packer.timeExpected.set(Util.secondToString(expectTime));

                packer.passedLength.set(Util.sizeToReadable(position));
            }
        }
    }

//    public static void main(String[] args) {
//        int[] resCount = new int[65536];
//        for (int i = 0; i < 16777216; ++i) {
//            int h = hash4bytesToInt16(
//                    (byte) (Math.random() * 256),
//                    (byte) (Math.random() * 256),
//                    (byte) (Math.random() * 256),
//                    (byte) (Math.random() * 256)
//            );
//            resCount[h] += 1;
//        }
//        System.out.println(Arrays.toString(resCount));
//        int max = 0, min = 16777216;
//        for (int c : resCount) {
//            if (c > max) max = c;
//            if (c < min) min = c;
//        }
//        System.out.println(max + " " + min);
//    }
}
