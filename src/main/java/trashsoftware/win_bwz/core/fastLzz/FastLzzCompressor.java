package trashsoftware.win_bwz.core.fastLzz;

import trashsoftware.win_bwz.core.Compressor;
import trashsoftware.win_bwz.packer.Packer;
import trashsoftware.win_bwz.utility.*;

import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * LZZ2 (Lempel-Ziv-ZBH 2) compressor, implements {@code Compressor} interface.
 * <p>
 * An improved version of LZ77 algorithm, implemented by Bohan Zhang (zbh).
 *
 * @author zbh
 * @see Compressor
 * @since 0.4
 */
public class FastLzzCompressor implements Compressor {

    /**
     * Load this size and process every time.
     */
    final static int MEMORY_BUFFER_SIZE = 16777216;  // 16 MB

    public static final int VERSION = 1;

    private InputStream sis;

    protected long totalLength;

    private long processedLength;

    private int bufferMaxSize;  // Size of LAB (Look ahead buffer).

    private int dictSize;

    public static final int MAXIMUM_DISTANCE = 71168 + FastLzzUtil.MINIMUM_DISTANCE;

    public static final int MAXIMUM_LENGTH = 276 + FastLzzUtil.MINIMUM_LENGTH;

    protected long cmpSize;

    protected Packer parent;

    private int timeAccumulator;

    private long lastUpdateProgress;

    private long startTime;
    private long timeOffset;
    long lastCheckTime;

    private int compressionLevel;

    private byte[] buffer;

    private int threads = 1;

    boolean notInterrupted = true;

    /**
     * Constructor of a new {@code LZZ2Compressor} instance.
     *
     * @param inFile     name of file to compress.
     * @param windowSize total sliding window size.
     * @param bufferSize size of look ahead buffer.
     * @throws IOException if error occurs during file reading or writing.
     */
    public FastLzzCompressor(String inFile, int windowSize, int bufferSize) throws IOException {
        this.bufferMaxSize = bufferSize;
        this.dictSize = windowSize - bufferMaxSize - 1;

        this.totalLength = new File(inFile).length();
//        this.remainingLength = totalLength;
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
    public FastLzzCompressor(MultipleInputStream mis, int windowSize, int bufferSize, long totalLength) {
        this.bufferMaxSize = bufferSize;
        this.dictSize = windowSize - bufferMaxSize - 1;
        this.totalLength = totalLength;
        this.sis = mis;
    }

    private void validateWindowSize() {
        if (totalLength <= bufferMaxSize) {
            dictSize = (int) totalLength - 1;
            bufferMaxSize = (int) totalLength - 1;
        }
    }

    private void compressContent(OutputStream outFile) throws IOException, InterruptedException {
        validateWindowSize();
        buffer = new byte[MEMORY_BUFFER_SIZE * threads];

        lastCheckTime = System.currentTimeMillis();
        startTime = lastCheckTime;
        if (parent != null) timeOffset = lastCheckTime - parent.startTime;

        int read;

        byte[] lengthBytes = Bytes.intToBytes32((int) totalLength);
        for (byte b : lengthBytes) outFile.write(b);
        cmpSize = 4;

        FixedSlider[] sliders = new FixedSlider[threads];
        for (int i = 0; i < threads; ++i) {
            if (compressionLevel > 0) {
                sliders[i] = new FixedArraySlider(16);
            } else {
                sliders[i] = new FixedIntSlider();
            }
        }

        while ((read = sis.read(buffer)) > 0 && notInterrupted) {
            EncodeThread[] threadArray = new EncodeThread[threads];
            ExecutorService es = Executors.newCachedThreadPool();
            int i = 0;
            int start = 0;
            while (start < read) {
                int end = Math.min(start + MEMORY_BUFFER_SIZE, read);
                threadArray[i] = new EncodeThread(start, end - start, sliders[i]);
                if (i == 0) threadArray[i].timerThread = true;
                i++;
                start = end;
            }
            for (EncodeThread et : threadArray) {
                if (et != null)
                    es.execute(et);
            }
            es.shutdown();
            es.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);  // Wait for all threads complete.

            for (EncodeThread et : threadArray) {
                if (et != null) {
                    cmpSize += et.writeToStream(outFile);
                    processedLength += et.bufferSize;
                }
            }
        }

        sis.close();
        outFile.flush();
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
    public void compress(OutputStream outFile) throws IOException, InterruptedException {
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
        this.threads = threads;
    }

    @Override
    public void setCompressionLevel(int compressionLevel) {
        this.compressionLevel = compressionLevel;
    }

    /**
     * returns the estimated memory usage during compression and decompression
     *
     * @param threads    the thread number
     * @param windowSize the window size
     * @param modeLevel  the strong level
     * @return {@code long[2]{memory when compress, memory when uncompress}}
     */
    public static long[] estimateMemoryUsage(int threads, int windowSize, int modeLevel) {
        long cmpMemOneThread = 1024;  // other objects
        cmpMemOneThread += MEMORY_BUFFER_SIZE;  // memory buffer
        if (modeLevel == 0) {  // FixedIntSlider
            cmpMemOneThread += 65536 * 4;
        } else {  // FixedArraySlider
            int fadSize = 24 + 8 + 16 * 4;
            cmpMemOneThread += 65536 * fadSize;
        }
        long cmpTotal = cmpMemOneThread * threads;

        long uncMem = 1024; // other objects
        uncMem += MEMORY_BUFFER_SIZE;  // memory buffer
        return new long[]{cmpTotal, uncMem};
    }

    private class EncodeThread implements Runnable {

        private int bufferStart;
        private int bufferSize;
        private FixedSlider slider;
        private FileBitOutputStream fos;

        /**
         * Whether to use this thread to drive the gui timer
         */
        private boolean timerThread;

        private int dis, len;

        EncodeThread(int bufferStart, int bufferSize, FixedSlider slider) {
            this.bufferStart = bufferStart;
            this.bufferSize = bufferSize;
            this.slider = slider;
            fos = new FileBitOutputStream(new FixedByteArrayOutputStream((int) ((double) bufferSize * 1.2)));
        }

        @Override
        public void run() {
            slider.clear();
            try {
                long currentTime;
                int i = 0;
                while (i < bufferSize - 3) {
                    if (compressionLevel > 0) {
                        calculateLongestMatch((FixedArraySlider) slider, bufferStart + i);
                    } else {
                        calculateLongestMatchSingle((FixedIntSlider) slider, bufferStart + i);
                    }
                    int prevI = i;

                    if (len < FastLzzUtil.MINIMUM_LENGTH) {
                        fos.write(0);
                        fos.writeByte(buffer[bufferStart + i]);
                        i++;
                    } else {
                        fos.write(1);
                        FastLzzUtil.writeLengthToStream(len, fos);
                        FastLzzUtil.writeDistanceToStream(dis, fos);

                        i += len;
                    }

                    if (i >= bufferSize) break;
                    fillSlider(bufferStart + prevI, bufferStart + i, this.slider);

                    if (parent != null && parent.isInterrupted) {
                        notInterrupted = false;
                        break;
                    }
                    if (timerThread &&
                            parent != null &&
                            (currentTime = System.currentTimeMillis()) - lastCheckTime >= 50) {
                        updateInfo(processedLength + i, currentTime);
                        lastCheckTime = currentTime;
                    }
                }
                for (; i < bufferSize; i++) {
                    fos.write(0);
                    fos.writeByte(buffer[bufferStart + i]);
                }
                fos.flush();
                fos.close();
            } catch (IOException e) {
                notInterrupted = false;
                e.printStackTrace();
            }
        }

        private void calculateLongestMatchSingle(FixedIntSlider slider, int index) {
            byte b0 = buffer[index];
            byte b1 = buffer[index + 1];
            int hash = hash(b0, b1);
            int position = slider.get(hash);
            if (position == -1) {  // not a match
                len = 0;
                return;
            }

            int windowBegin = Math.max(index - dictSize, bufferStart);

            if (position < windowBegin) {
                len = 0;
                return;
            }

            int bufferFrontLimit = bufferStart + bufferSize;
            int length = 2;
            while (length < bufferMaxSize &&
                    index + length < bufferFrontLimit &&
                    buffer[position + length] == buffer[index + length]) {
                length++;
            }

            dis = index - position;
            len = length;
        }

        private void calculateLongestMatch(FixedArraySlider slider, int index) {
            byte b0 = buffer[index];
            byte b1 = buffer[index + 1];
            int hash = hash(b0, b1);
            FixedArraySlider.FixedArrayDeque positions = slider.get(hash);
            if (positions == null) {  // not a match
                len = 0;
                return;
            }

            int windowBegin = Math.max(index - dictSize, bufferStart);

            int longest = 2;  // at least 2
            final int beginPos = positions.beginPos();
            int indexOfLongest = positions.tail;
            int andEr = slider.getAndEr();
            int bufferFrontLimit = bufferStart + bufferSize;
            for (int i = positions.tail - 1; i >= beginPos; i--) {
                int pos = positions.array[i & andEr];

                if (pos <= windowBegin) break;

                int len = 2;
                while (len < bufferMaxSize &&
                        index + len < bufferFrontLimit &&
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

        private void fillSlider(int from, int to, FixedSlider slider) {
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

        int writeToStream(OutputStream outputStream) throws IOException {
            FixedByteArrayOutputStream arrayOutputStream = (FixedByteArrayOutputStream) fos.getStream();
            arrayOutputStream.writeToStream(outputStream);
            return arrayOutputStream.getLength();
        }
    }
}
