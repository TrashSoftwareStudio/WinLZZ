package trashsoftware.win_bwz.core.lzz2;

import trashsoftware.win_bwz.core.bwz.MTFTransformByte;
import trashsoftware.win_bwz.core.Compressor;
import trashsoftware.win_bwz.core.fastLzz.FixedSlider;
import trashsoftware.win_bwz.core.lzz2.util.LZZ2Util;
import trashsoftware.win_bwz.huffman.HuffmanCompressor;
import trashsoftware.win_bwz.huffman.MapCompressor.MapCompressor;
import trashsoftware.win_bwz.packer.Packer;
import trashsoftware.win_bwz.utility.FileBitOutputStream;
import trashsoftware.win_bwz.utility.FileInputBufferArray;
import trashsoftware.win_bwz.utility.MultipleInputStream;
import trashsoftware.win_bwz.utility.Util;

import java.io.*;
import java.util.*;

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

    private InputStream sis;

    protected long totalLength;

    private int windowSize;  // Size of sliding window.

    private int bufferMaxSize;  // Size of LAB (Look ahead buffer).

    private int dictSize;

    final static int minimumMatchLen = 3;

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
        this.bufferMaxSize = bufferSize + minimumMatchLen + 1;
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
        this.bufferMaxSize = bufferSize + minimumMatchLen + 1;
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

    private int sliderArraySize() {
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
        FixedSliderLong slider = new FixedSliderLong(sliderArraySize());
//        SimpleHashSlider slider = new SimpleHashSlider();
//        LinkedHashSet<Long> omitSet = new LinkedHashSet<>();

        long position = 0;  // The processing index. i.e. the border of buffer and slider.
//        int sliderSize = 0;

//        fillHashSlider(fba, 0, (int) front, slider, omitSet);

        LinkedList<Integer> lastMatches = new LinkedList<>();
        int lastLength = -1;

        BufferedOutputStream mainFos = new BufferedOutputStream(new FileOutputStream(mainTempName));
        BufferedOutputStream disFos = new BufferedOutputStream(new FileOutputStream(disHeadTempName));
        BufferedOutputStream lenFos = new BufferedOutputStream(new FileOutputStream(lenHeadTempName));
        FileBitOutputStream flagFos = new FileBitOutputStream(new BufferedOutputStream(new FileOutputStream(flagTempName)));
        FileBitOutputStream dlbFos = new FileBitOutputStream(new BufferedOutputStream(new FileOutputStream(dlBodyTempName)));

        long lastCheckTime = System.currentTimeMillis();
        startTime = lastCheckTime;
        if (parent != null) timeOffset = lastCheckTime - parent.startTime;
        long currentTime;

        while (position < totalLength - 3) {


            int skip = search(fba, slider, position);
            long prevPos = position;

            if (len < minimumMatchLen) {
                // Not a match
                flagFos.write(0);
                mainFos.write(fba.getByte(position++));
            } else {
                // A match.
                for (int j = 0; j < skip; ++j) {
                    flagFos.write(0);
                    mainFos.write(fba.getByte(position++));
                }

                flagFos.write(1);
                int distanceInt = dis;
                int lengthInt = len;
                int findInLast = compressionLevel > 0 ? reverseIndexInQueue(lastMatches, distanceInt) : -1;
                if (findInLast == -1) {
                    LZZ2Util.addDistance(distanceInt, 0, disFos, dlbFos);  // Distance first.
                    LZZ2Util.addLength(lengthInt, minimumMatchLen, lenFos, dlbFos);
                } else {
                    if (findInLast == 0 && lastLength == lengthInt) {
                        disFos.write((byte) 4);
                    } else {
                        disFos.write((byte) findInLast);
                        LZZ2Util.addLength(lengthInt, minimumMatchLen, lenFos, dlbFos);
                    }
                }
                itemCount += 1;
                position += lengthInt;
                lastLength = lengthInt;
                lastMatches.addFirst(distanceInt);
                if (lastMatches.size() > 4) lastMatches.removeLast();
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
            flagFos.write(0);
            mainFos.write(fba.getByte(position));
        }

        mainFos.flush();
        mainFos.close();
        disFos.flush();
        disFos.close();
        lenFos.flush();
        lenFos.close();
        flagFos.flush();
        flagFos.close();
        dlbFos.flush();
        dlbFos.close();
        fba.close();
    }

    private static int hash(byte b0, byte b1) {
        return (b0 & 0xff) << 8 | (b1 & 0xff);
    }

    private void fillSlider(long from, long to, FileInputBufferArray fba, FixedSliderLong slider) {
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

    private int search(FileInputBufferArray fba, FixedSliderLong slider, long index) {
        if (compressionLevel < 2) {
            calculateLongestMatch(fba, slider, index);
            return 0;
        } else if (compressionLevel < 4) {
            return search1(fba, slider, index);
        } else {
            return searchMore(fba, slider, index);
        }
    }

    private int search1(FileInputBufferArray fba, FixedSliderLong slider, long index) {
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

    private int searchMore(FileInputBufferArray fba, FixedSliderLong slider, long index) {
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

    private void calculateLongestMatch(FileInputBufferArray fba, FixedSliderLong slider, long index) {
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

    private int reverseIndexInQueue(Queue<Integer> queue, int target) {
        int index = 0;
        for (int i : queue) {
            if (i == target) return index;
            index += 1;
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

        HuffmanCompressor dhc = new HuffmanCompressor(disHeadTempName);
        byte[] dhcMap = dhc.getMap(64);

        HuffmanCompressor lhc = new HuffmanCompressor(lenHeadTempName);
        byte[] lhcMap = lhc.getMap(32);

        HuffmanCompressor fc = new HuffmanCompressor(flagTempName);
        byte[] fcMap = fc.getMap(256);

        HuffmanCompressor mtc = new HuffmanCompressor(mainTempName);
        byte[] mtcMap = mtc.getMap(256);

        byte[] totalMap = new byte[608];
        System.arraycopy(dhcMap, 0, totalMap, 0, 64);
        System.arraycopy(lhcMap, 0, totalMap, 64, 32);
        System.arraycopy(fcMap, 0, totalMap, 96, 256);
        System.arraycopy(mtcMap, 0, totalMap, 352, 256);

        byte[] rlcMain = new MTFTransformByte(totalMap).Transform(18);

        MapCompressor mc = new MapCompressor(rlcMain);
        byte[] csq = mc.Compress(false);

        outFile.write(csq);

        dhc.SepCompress(outFile);
        long disHeadLen = dhc.getCompressedLength();
        lhc.SepCompress(outFile);
        long lenHeadLen = lhc.getCompressedLength();
        fc.SepCompress(outFile);
        long flagLen = fc.getCompressedLength();

        long dlbLen = Util.fileConcatenate(outFile, new String[]{dlBodyTempName}, 8192);

        mtc.SepCompress(outFile);
        long mainLen = mtc.getCompressedLength();

        deleteTemp();

        int csqLen = csq.length;
        long[] sizes = new long[]{csqLen, disHeadLen, lenHeadLen, flagLen, dlbLen};
        byte[] sizeBlock = Util.generateSizeBlock(sizes);
        outFile.write(sizeBlock);
        cmpSize = disHeadLen + lenHeadLen + flagLen + dlbLen + mainLen + csqLen + sizeBlock.length;
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

//    /**
//     * @return [lazy evaluation delay, skip repeat]
//     */
//    private int[] getCompressionParam() {
//        if (compressionLevel == 0) return new int[]{0, 0};
//        else if (compressionLevel == 1) return new int[]{1, 0};
//        else if (compressionLevel == 2) return new int[]{2, 0};
//        else if (compressionLevel == 3) return new int[]{1, 1};
//        else if (compressionLevel == 4) return new int[]{2, 1};
//        else throw new IndexOutOfBoundsException("Unknown level");
//    }
}
