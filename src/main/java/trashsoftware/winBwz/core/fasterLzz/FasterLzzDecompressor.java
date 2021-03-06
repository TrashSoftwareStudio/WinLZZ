package trashsoftware.winBwz.core.fasterLzz;

import trashsoftware.winBwz.core.DeCompressor;
import trashsoftware.winBwz.packer.UnPacker;
import trashsoftware.winBwz.utility.Bytes;
import trashsoftware.winBwz.utility.FileBitInputStream;
import trashsoftware.winBwz.utility.Util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * LZZ2-algorithm decompressor, implements {@code DeCompressor} interface.
 *
 * @author zbh
 * @see DeCompressor
 * @since 0.4
 */
public class FasterLzzDecompressor implements DeCompressor {

    private FileBitInputStream fis;

    private UnPacker parent;

    private int timeAccumulator;

    private long lastUpdateProgress;

    private long startTime;

    private long timeOffset;

    private long umpLength;

    private byte[] outBuffer = new byte[FasterLzzCompressor.MEMORY_BUFFER_SIZE];

    public FasterLzzDecompressor(String inFile, int windowSize) throws IOException {

        fis = new FileBitInputStream(new FileInputStream(inFile));
        byte[] lengthBytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            lengthBytes[i] = fis.readByte();
        }
        umpLength = Bytes.bytesToInt32(lengthBytes);
    }

    private void uncompressMain(OutputStream fos) throws IOException {
        long lastCheckTime = System.currentTimeMillis();
        startTime = lastCheckTime;
        if (parent != null) timeOffset = lastCheckTime - parent.startTime;
        long currentTime;

        int indexInBuffer = 0;
        long totalUmpIndex = 0;

        while (totalUmpIndex + indexInBuffer < umpLength) {
            int s = fis.read();
            if (s == 0) {
                byte lit = fis.readByte();
                outBuffer[indexInBuffer++] = lit;
            } else if (s == 1) {
                int length = FasterLzzUtil.readLengthFromStream(fis);
                int distance = FasterLzzUtil.readDistanceFromStream(fis);

                int from = indexInBuffer - distance;
                int to = from + length;
                if (to <= indexInBuffer) {
                    System.arraycopy(outBuffer, from, outBuffer, indexInBuffer, length);
                } else {
                    int p = 0;
                    int overlap = indexInBuffer - from;
                    while (p < length) {
                        System.arraycopy(outBuffer,
                                from,
                                outBuffer,
                                indexInBuffer + p,
                                Math.min(overlap, length - p));
                        p += overlap;
                    }
                }
                indexInBuffer += length;
            } else {
                break;
            }
            if (indexInBuffer >= FasterLzzCompressor.MEMORY_BUFFER_SIZE) {
                if (indexInBuffer != FasterLzzCompressor.MEMORY_BUFFER_SIZE) throw new RuntimeException();
                totalUmpIndex += indexInBuffer;
                indexInBuffer = 0;
                fos.write(outBuffer);
                fis.alignByte();
            }
            if (parent != null && (currentTime = System.currentTimeMillis()) - lastCheckTime >= 50) {
                updateInfo(totalUmpIndex + indexInBuffer, currentTime);
                lastCheckTime = currentTime;
                if (parent.isInterrupted) break;
            }
        }
        if (indexInBuffer > 0) {
            fos.write(outBuffer, 0, indexInBuffer);
        }
    }

    @Override
    public void deleteCache() {
    }

    @Override
    public void setUnPacker(UnPacker unPacker) {
        this.parent = unPacker;
    }

    @Override
    public void setThreads(int threads) {
    }

    @Override
    public void uncompress(OutputStream outFile) throws IOException {
        uncompressMain(outFile);
        fis.close();
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
