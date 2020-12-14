package trashsoftware.winBwz.core.fasterLzz;

import trashsoftware.winBwz.core.Compressor;
import trashsoftware.winBwz.packer.PzPacker;
import trashsoftware.winBwz.utility.*;

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
public class FasterLzzCompressor implements Compressor {

    /**
     * Load this size and process every time.
     */
    final static int MEMORY_BUFFER_SIZE = 16777216;  // 16 MB

    public static final int VERSION = 0;

    private InputStream sis;

    protected long totalLength;

    private long processedLength;

    private int bufferMaxSize;  // Size of LAB (Look ahead buffer).

    private int dictSize;

    public static final int MAXIMUM_DISTANCE = 65535 + FasterLzzUtil.MINIMUM_DISTANCE;

    public static final int MAXIMUM_LENGTH = 276 + FasterLzzUtil.MINIMUM_LENGTH;

    protected long cmpSize;

    protected PzPacker parent;

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
    public FasterLzzCompressor(String inFile, int windowSize, int bufferSize) throws IOException {
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
    public FasterLzzCompressor(MultipleInputStream mis, int windowSize, int bufferSize, long totalLength) {
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

        FixedIntSlider[] sliders = new FixedIntSlider[threads];
        for (int i = 0; i < threads; ++i) {
            sliders[i] = new FixedIntSlider();
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
    public void setPacker(PzPacker packer) {
        this.parent = packer;
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
        private FixedIntSlider slider;
        private byte[] outBuffer;
        private int outIndex;

        /**
         * Whether to use this thread to drive the gui timer
         */
        private boolean timerThread;

        private int dis, len;

        EncodeThread(int bufferStart, int bufferSize, FixedIntSlider slider) {
            this.bufferStart = bufferStart;
            this.bufferSize = bufferSize;
            this.slider = slider;
            outBuffer = new byte[(int) ((double) bufferSize * 1.2)];
        }

        @Override
        public void run() {
            slider.clear();
            byte[] extraLitBuf = new byte[256];
            byte[] extraLenBuf = new byte[16];
            long currentTime;
            int i = 0;
            int litCountStatic = 0;
            while (i < bufferSize - 3) {
                calculateLongestMatchSingle(slider, bufferStart + i);
                int prevI = i;

                if (len < FasterLzzUtil.MINIMUM_LENGTH) {
                    litCountStatic++;
                    i++;
                } else {
                    int litToken, lenToken;
                    int extraLitLen = 0, extraLenLen = 0;
                    int litCount = litCountStatic;

                    if (litCount < 15) {
                        litToken = litCount;
                    } else {
                        litToken = 15;
                        litCount -= 15;
                        while (litCount > 0) {
                            if (litCount > 254) {
                                extraLitBuf[extraLitLen++] = -1;
                                litCount -= 254;
                            } else {
                                extraLitBuf[extraLitLen++] = (byte) litCount;
                                litCount = 0;
                            }
                        }
                    }

                    int matchLen = len - FasterLzzUtil.MINIMUM_LENGTH;
                    if (matchLen < 15) {
                        lenToken = matchLen;
                    } else {
                        lenToken = 15;
                        matchLen -= 15;
                        while (matchLen > 0) {
                            if (matchLen > 254) {
                                extraLenBuf[extraLenLen++] = -1;
                            } else {
                                extraLenBuf[extraLenLen++] = (byte) matchLen;
                            }
                            matchLen -= 254;
                        }
                    }

                    int token = (litToken << 4) | lenToken;
                    outBuffer[outIndex++] = (byte) token;  // writes token
                    System.arraycopy(extraLitBuf, 0, outBuffer, outIndex, extraLitLen);  // writes extra literal len
                    outIndex += extraLitLen;
                    int litBegin = i - litCountStatic;
                    System.arraycopy(buffer, bufferStart + litBegin, outBuffer, outIndex, litCountStatic);  // writes literal
                    outIndex += litCountStatic;

                    Bytes.shortToBytes(dis - FasterLzzUtil.MINIMUM_DISTANCE, outBuffer, outIndex);  // write distance
                    outIndex += 2;
                    System.arraycopy(extraLenBuf, 0, outBuffer, outIndex, extraLenLen);
                    outIndex += extraLenLen;

                    litCountStatic = 0;
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
//                for (; i < bufferSize; i++) {
//                    fos.write(0);
//                    fos.writeByte(buffer[bufferStart + i]);
//                }
//                fos.flush();
//                fos.close();
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

        private void fillSlider(int from, int to, FixedIntSlider slider) {
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
            outputStream.write(outBuffer, 0, outIndex);
            return outIndex;
        }
    }
}
