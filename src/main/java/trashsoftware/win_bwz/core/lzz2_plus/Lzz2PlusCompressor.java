package trashsoftware.win_bwz.core.lzz2_plus;

import trashsoftware.win_bwz.core.Compressor;
import trashsoftware.win_bwz.packer.Packer;
import trashsoftware.win_bwz.utility.Bytes;
import trashsoftware.win_bwz.utility.FileBitOutputStream;
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
public class Lzz2PlusCompressor implements Compressor {

    /**
     * Load this size and process every time.
     */
    final static int MEMORY_BUFFER_SIZE = 16777216;  // 16 MB

    private InputStream sis;

    protected long totalLength;

    private long remainingLength;

    private int bufferMaxSize;  // Size of LAB (Look ahead buffer).

    private int dictSize;

    final static int minimumMatchLen = 3;

    protected long cmpSize;

    protected Packer parent;

    private int timeAccumulator;

    private long lastUpdateProgress;

    private long startTime;

    private long timeOffset;

    private int dis, len;

    private byte[] buffer = new byte[MEMORY_BUFFER_SIZE];

    /**
     * Constructor of a new {@code LZZ2Compressor} instance.
     *
     * @param inFile     name of file to compress.
     * @param windowSize total sliding window size.
     * @param bufferSize size of look ahead buffer.
     * @throws IOException if error occurs during file reading or writing.
     */
    public Lzz2PlusCompressor(String inFile, int windowSize, int bufferSize) throws IOException {
        this.bufferMaxSize = bufferSize;
        this.dictSize = windowSize - bufferMaxSize - 1;

        this.totalLength = new File(inFile).length();
        this.remainingLength = totalLength;
        this.sis = new FileInputStream(inFile);
    }

    /**
     * Constructor of a new {@code LZZ2Compressor} instance.
     *
     * @param mis         the input stream
     * @param windowSize  total sliding window size.
     * @param bufferSize  size of look ahead buffer.
     * @param totalLength the total length of the files to be compressed
     */
    public Lzz2PlusCompressor(MultipleInputStream mis, int windowSize, int bufferSize, long totalLength) {
        this.bufferMaxSize = bufferSize;
        this.dictSize = windowSize - bufferMaxSize - 1;
        this.totalLength = totalLength;
        this.remainingLength = totalLength;
        this.sis = mis;
    }

    private void compressContent(OutputStream outFile) throws IOException {
        if (totalLength <= bufferMaxSize) {
            dictSize = (int) totalLength - 1;
            bufferMaxSize = (int) totalLength - 1;
        }

        long lastCheckTime = System.currentTimeMillis();
        startTime = lastCheckTime;
        if (parent != null) timeOffset = lastCheckTime - parent.startTime;
        long currentTime;

        int read;

        FileBitOutputStream fos = new FileBitOutputStream(outFile);

        byte[] lengthBytes = Bytes.intToBytes32((int) totalLength);
        for (byte b : lengthBytes) fos.writeByte(b);

        Slider slider = new Slider();
        while ((read = sis.read(buffer)) > 0) {
            slider.clear();
            int i = 0;
            while (i < read - 3) {
                calculateLongestMatch(slider, i);
                int prevI = i;

                if (len < minimumMatchLen) {
                    fos.write(0);
                    fos.writeByte(buffer[i]);
                    i++;
                } else {
                    fos.write(1);
                    Lzz2pUtil.writeLengthToStream(len, fos);
                    Lzz2pUtil.writeDistanceToStream(dis, fos);

                    i += len;
                }

                if (i >= read) break;
                fillSlider(prevI, i, slider);

                if (parent != null && parent.isInterrupted) break;
                if (parent != null && (currentTime = System.currentTimeMillis()) - lastCheckTime >= 50) {
                    updateInfo(totalLength - remainingLength + i, currentTime);
                    lastCheckTime = currentTime;
                }
            }
            for (; i < read; i++) {
                fos.write(0);
                fos.writeByte(buffer[i]);
            }
            remainingLength -= read;
        }

        sis.close();
        fos.flush();
        cmpSize = fos.getLength();
//        fos.close();
    }

    private void fillSlider(int from, int to, Slider slider) {
        int lastHash = -1;
        int repeatCount = 0;
        for (int j = from; j < to; j++) {
            byte b0 = buffer[j];
            byte b1 = buffer[j + 1];
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

    private void calculateLongestMatch(Slider slider, int index) {
        byte b0 = buffer[index];
        byte b1 = buffer[index + 1];
        int hash = hash(b0, b1);
        FixedArrayDeque positions = slider.get(hash);
        if (positions == null) {  // not a match
            len = 0;
            return;
        }

        int maxLookAhead = (int) Math.min(remainingLength, MEMORY_BUFFER_SIZE);
        int windowBegin = Math.max(index - dictSize, 0);
//        System.out.println(windowBegin);

        int longest = 2;  // at least 2
        final int beginPos = positions.beginPos();
        int indexOfLongest = positions.tail;
        for (int i = positions.tail - 1; i >= beginPos; i--) {
            int pos = positions.array[i & FixedArrayDeque.RANGE];

            if (pos <= windowBegin) break;

            int len = 2;
            while (len < bufferMaxSize &&
                    index + len < maxLookAhead &&
                    buffer[pos + len] == buffer[index + len]) {
                len++;
            }
            if (len > longest) {  // Later match is preferred
                longest = len;
                indexOfLongest = pos;
            }
        }

        dis = index - indexOfLongest;
        len = longest;
    }

    private static int hash(byte b0, byte b1) {
        return (b0 & 0xff) << 8 | (b1 & 0xff);
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

    /**
     * compress file into output stream.
     *
     * @param outFile the target output stream.
     * @throws IOException if io error occurs during compression.
     */
    @Override
    public void compress(OutputStream outFile) throws IOException {
        compressContent(outFile);
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
    }
}
