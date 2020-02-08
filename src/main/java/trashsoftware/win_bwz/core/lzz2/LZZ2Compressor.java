package trashsoftware.win_bwz.core.lzz2;

import trashsoftware.win_bwz.core.bwz.MTFTransformByte;
import trashsoftware.win_bwz.core.Compressor;
import trashsoftware.win_bwz.core.lzz2.util.LZZ2Util;
import trashsoftware.win_bwz.huffman.HuffmanCompressor;
import trashsoftware.win_bwz.huffman.HuffmanCompressorBase;
import trashsoftware.win_bwz.huffman.HuffmanCompressorTwoBytes;
import trashsoftware.win_bwz.huffman.MapCompressor.MapCompressor;
import trashsoftware.win_bwz.longHuffman.LongHuffmanUtil;
import trashsoftware.win_bwz.packer.Packer;
import trashsoftware.win_bwz.utility.FileBitOutputStream;
import trashsoftware.win_bwz.utility.FileInputBufferArray;
import trashsoftware.win_bwz.utility.MultipleInputStream;
import trashsoftware.win_bwz.utility.Util;

import java.io.*;

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

    public static final int VERSION = 1;

    public static final int MAIN_HUF_ALPHABET = 286;

    final static int MINIMUM_MATCH_LEN = 3;

    public static final int MAXIMUM_LENGTH = 286 + MINIMUM_MATCH_LEN;

    private InputStream sis;

    protected long totalLength;

    private int windowSize;  // Size of sliding window.

    private int bufferMaxSize;  // Size of LAB (Look ahead buffer).

    private int dictSize;

    protected String mainTempName, lenHeadTempName, disHeadTempName, flagTempName, dlBodyTempName;

    protected long cmpSize;

    protected int itemCount;

    protected Packer parent;

    private int timeAccumulator;

    private long lastUpdateProgress;

    private long startTime;

    private long timeOffset;

    private int compressionLevel;

    private int dis, len;

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

    private void setTempNames(String inFile) {
        this.mainTempName = inFile + ".main.temp";
        this.lenHeadTempName = inFile + ".len.temp";
        this.disHeadTempName = inFile + ".dis.temp";
        this.flagTempName = inFile + ".flag.temp";
        this.dlBodyTempName = inFile + ".dlb.temp";
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

    protected void compressText() throws IOException {
        FileInputBufferArray fba = new FileInputBufferArray(sis, totalLength, windowSize);

        if (totalLength <= bufferMaxSize) {
            dictSize = (int) totalLength - 1;
            bufferMaxSize = (int) totalLength - 1;
        }
        FixedSliderLong slider = new FixedSliderLong(sliderArraySize(dictSize, compressionLevel));

        long position = 0;  // The processing index. i.e. the border of buffer and slider.

        int[] lastDistances = new int[4];
        int lastDisIndex = 0;
        int lastLength = -1;

        BufferedOutputStream mainFos = new BufferedOutputStream(new FileOutputStream(mainTempName));
        BufferedOutputStream disFos = new BufferedOutputStream(new FileOutputStream(disHeadTempName));
        FileBitOutputStream dlbFos = new FileBitOutputStream(
                new BufferedOutputStream(new FileOutputStream(dlBodyTempName)));

        long lastCheckTime = System.currentTimeMillis();
        startTime = lastCheckTime;
        if (parent != null) timeOffset = lastCheckTime - parent.startTime;
        long currentTime;

        while (position < totalLength - 3) {

            int skip = search(fba, slider, position);
            long prevPos = position;

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

            if (position >= totalLength) break;
            fillSlider(prevPos, position, fba, slider);

            if (parent != null && parent.isInterrupted) break;
            if (parent != null && (currentTime = System.currentTimeMillis()) - lastCheckTime >= 50) {
                updateInfo(position, currentTime);
                lastCheckTime = currentTime;
            }
        }

        for (; position < totalLength; ++position) {
            mainFos.write(0);
            mainFos.write(fba.getByte(position));
        }

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

    public static void printStream(String name) throws IOException {
        BufferedInputStream bbb = new BufferedInputStream(new FileInputStream(name));
        byte[] buf = new byte[1];
        while (bbb.read(buf) > 0) {
            System.out.print((buf[0] & 0xff) + " ");
        }
        System.out.println();
        bbb.close();
    }

    private static int hash(byte b0, byte b1) {
        return (b0 & 0xff) << 8 | (b1 & 0xff);
    }

    private void fillSlider(long from, long to, FileInputBufferArray fba, FixedSliderLong slider) throws IOException {
        int lastHash = -1;
        int repeatCount = 0;
        for (long j = from; j < to; j++) {
            byte b0 = fba.getByte(j);
            byte b1 = fba.getByte(j + 1);
            int hash = hash(b0, b1);
            if (hash == lastHash) {
                repeatCount++;
            } else {
                if (repeatCount > 0) {
                    repeatCount = 0;
                    slider.addIndex(lastHash, j - 1);
                }
                lastHash = hash;
                slider.addIndex(hash, j);
            }
        }
    }

    private int search(FileInputBufferArray fba, FixedSliderLong slider, long index) throws IOException {
        if (compressionLevel < 2) {
            calculateLongestMatch(fba, slider, index);
            return 0;
        } else if (compressionLevel < 4) {
            return search1(fba, slider, index);
        } else {
            return searchMore(fba, slider, index);
        }
    }

    private int search1(FileInputBufferArray fba, FixedSliderLong slider, long index) throws IOException {
        calculateLongestMatch(fba, slider, index);
        int dis1 = dis;
        int len1 = len;
        calculateLongestMatch(fba, slider, index + 1);
        if (len > len1 + 1) {
            return 1;
        } else {
            dis = dis1;
            len = len1;
            return 0;
        }
    }

    private int searchMore(FileInputBufferArray fba, FixedSliderLong slider, long index) throws IOException {
        int skip = 1;
        calculateLongestMatch(fba, slider, index);
        int lastDis = dis;
        int lastLen = len;
        while (true) {
            calculateLongestMatch(fba, slider, index + skip);
            if (len > lastLen + skip) {
                lastDis = dis;
                lastLen = len;
                skip++;
            } else {
                dis = lastDis;
                len = lastLen;
                return skip - 1;
            }
        }
    }

    private void calculateLongestMatch(FileInputBufferArray fba, FixedSliderLong slider, long index)
            throws IOException {
        byte b0 = fba.getByte(index);
        byte b1 = fba.getByte(index + 1);
        int hash = hash(b0, b1);
        FixedArrayDeque positions = slider.get(hash);
        if (positions == null) {  // not a match
            len = 0;
            return;
        }
        long windowBegin = Math.max(index - dictSize, 0);

        int longest = 2;  // at least 2
        final int beginPos = positions.beginPos();
        int tail = positions.getTail();
        long indexOfLongest = positions.getTail();
        for (int i = tail - 1; i >= beginPos; --i) {
            long pos = positions.get(i);

            if (pos <= windowBegin) break;

            int len = 2;
            while (len < bufferMaxSize &&
                    index + len < totalLength &&
                    fba.getByte(pos + len) == fba.getByte(index + len)) {
                len++;
            }
            if (len > longest) {  // Later match is preferred
                longest = len;
                indexOfLongest = pos;
            }
        }

        dis = (int) (index - indexOfLongest);
        len = longest;
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

    protected void deleteTemp() {
        Util.deleteFile(mainTempName);
        Util.deleteFile(disHeadTempName);
        Util.deleteFile(lenHeadTempName);
        Util.deleteFile(flagTempName);
        Util.deleteFile(dlBodyTempName);
    }

    private void updateInfo(long current, long updateTime) {
        parent.progress.set(current);
        if (timeAccumulator == 19) {
            timeAccumulator = 0;
            double finished = ((double) current) / totalLength;
            double rounded = (double) Math.round(finished * 1000) / 10;
            parent.percentage.set(String.valueOf(rounded));
            int newUpdated = (int) (current - lastUpdateProgress);
            lastUpdateProgress = parent.progress.get();
            int ratio = newUpdated / 1024;
            parent.ratio.set(String.valueOf(ratio));

            long timeUsed = updateTime - startTime;
            parent.timeUsed.set(Util.secondToString((timeUsed + timeOffset) / 1000));
            long expectTime = (totalLength - current) / ratio / 1024;
            parent.timeExpected.set(Util.secondToString(expectTime));

            parent.passedLength.set(Util.sizeToReadable(current));
        } else {
            timeAccumulator += 1;
        }
    }

    protected boolean isNotCompressible(OutputStream outFile) throws IOException {
        if (itemCount == 0) {
            Util.fileConcatenate(outFile, new String[]{mainTempName}, 8192);
            outFile.write((byte) 0);
            cmpSize = (int) totalLength + 1;
            deleteTemp();
            return true;
        }

        if (parent != null && parent.isInterrupted) {
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
        byte[] dhcMap = dhc.getMap(64);

        HuffmanCompressorBase mtc = new HuffmanCompressorTwoBytes(mainTempName);
        byte[] mtcMap = mtc.getMap(MAIN_HUF_ALPHABET);

//        System.out.println(Arrays.toString(dhcMap));

        byte[] totalMap = new byte[MAIN_HUF_ALPHABET + 64];
        System.arraycopy(dhcMap, 0, totalMap, 0, 64);
        System.arraycopy(mtcMap, 0, totalMap, 64, MAIN_HUF_ALPHABET);

        byte[] rlcMain = new MTFTransformByte(totalMap).Transform(18);

        MapCompressor mc = new MapCompressor(rlcMain);
        byte[] csq = mc.Compress(false);

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
    public void setParent(Packer parent) {
        this.parent = parent;
    }

    @Override
    public void setThreads(int threads) {
    }

    @Override
    public void setCompressionLevel(int compressionLevel) {
        this.compressionLevel = compressionLevel;
    }

    public static long[] estimateMemoryUsage(int threads, int windowSize, int modeLevel) {
        long cmpMem = 2048;  // objects
        cmpMem += windowSize * 2;  // input buffer array
        long sliderArraySize = sliderArraySize(windowSize, modeLevel);
        long sliderMem = sliderArraySize * 65536 * 8;  // 8 is size of long
        cmpMem += sliderMem;
        cmpMem += 65536 * 32;  // FixedArrayDeque
        cmpMem += 16384;  // estimate mtf, huf

        long uncMem = 2048;  // objects
        uncMem += LongHuffmanUtil.hufDecompressorMem();  // huffman dec
        uncMem += 500;  // heads
        uncMem += windowSize * 2;  // output buffer
        uncMem += 16384;  // others

        return new long[]{cmpMem, uncMem};
    }
}
