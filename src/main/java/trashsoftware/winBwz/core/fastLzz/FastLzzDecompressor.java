package trashsoftware.winBwz.core.fastLzz;

import trashsoftware.winBwz.core.Constants;
import trashsoftware.winBwz.core.DeCompressor;
import trashsoftware.winBwz.packer.PzUnPacker;
import trashsoftware.winBwz.utility.Bytes;
import trashsoftware.winBwz.utility.FileBitInputStream;
import trashsoftware.winBwz.utility.Util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

/**
 * LZZ2-algorithm decompressor, implements {@code DeCompressor} interface.
 *
 * @author zbh
 * @see DeCompressor
 * @since 0.4
 */
public class FastLzzDecompressor implements DeCompressor {

    private final FileBitInputStream fis;
    private final long umpLength;
    private final byte[] outBuffer = new byte[FastLzzCompressor.MEMORY_BUFFER_SIZE];
    private PzUnPacker unPacker;
    private long lastUpdateProgress;
    private long timeOffset;
    private long totalOutLength;

    public FastLzzDecompressor(String inFile, int windowSize) throws IOException {

        fis = new FileBitInputStream(new FileInputStream(inFile));
        byte[] lengthBytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            lengthBytes[i] = fis.readByte();
        }
        umpLength = Bytes.bytesToInt32(lengthBytes);
    }

    private void uncompressMain(OutputStream fos) throws IOException {
        Timer timer = null;
        if (unPacker != null) {
            timeOffset = System.currentTimeMillis() - unPacker.startTime;
            timer = new Timer();
            timer.scheduleAtFixedRate(new DeFLTimerTask(), 0, 1000 / Constants.GUI_UPDATES_PER_S);
        }

        int indexInBuffer = 0;
        long totalUmpIndex = 0;

        while (totalUmpIndex + indexInBuffer < umpLength) {
            int s = fis.read();
            if (s == 0) {
                byte lit = fis.readByte();
                outBuffer[indexInBuffer++] = lit;
            } else if (s == 1) {
                int length = FastLzzUtil.readLengthFromStream(fis);
                int distance = FastLzzUtil.readDistanceFromStream(fis);

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
            if (indexInBuffer >= FastLzzCompressor.MEMORY_BUFFER_SIZE) {
                if (indexInBuffer != FastLzzCompressor.MEMORY_BUFFER_SIZE) throw new RuntimeException();
                totalUmpIndex += indexInBuffer;
                indexInBuffer = 0;
                fos.write(outBuffer);
                fis.alignByte();
            }
            totalOutLength = totalUmpIndex + indexInBuffer;
//            if (unPacker != null && (currentTime = System.currentTimeMillis()) - lastCheckTime >= 50) {
//                updateInfo(totalUmpIndex + indexInBuffer, currentTime);
//                lastCheckTime = currentTime;
//                if (unPacker.isInterrupted) break;
//            }
        }
        if (indexInBuffer > 0) {
            fos.write(outBuffer, 0, indexInBuffer);
        }
        if (timer != null) timer.cancel();
    }

    @Override
    public void deleteCache() {
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
        uncompressMain(outFile);
        fis.close();
    }

//    private void updateInfo(long current, long updateTime) {
//        unPacker.progress.set(current);
//        if (timeAccumulator == 19) {
//            timeAccumulator = 0;
//            double finished = ((double) current) / unPacker.getTotalOrigSize();
//            double rounded = (double) Math.round(finished * 1000) / 10;
//            unPacker.percentage.set(String.valueOf(rounded));
//            int newUpdated = (int) (current - lastUpdateProgress);
//            lastUpdateProgress = unPacker.progress.get();
//            int ratio = newUpdated / 1024;
//            unPacker.ratio.set(String.valueOf(ratio));
//
//            long timeUsed = updateTime - startTime;
//            unPacker.timeUsed.set(Util.secondToString((timeUsed + timeOffset) / 1000));
//            long expectTime = (unPacker.getTotalOrigSize() - current) / ratio / 1024;
//            unPacker.timeExpected.set(Util.secondToString(expectTime));
//
//            unPacker.passedLength.set(Util.sizeToReadable(current));
//        } else {
//            timeAccumulator += 1;
//        }
//    }

    class DeFLTimerTask extends TimerTask {
        private int accumulator;

        @Override
        public void run() {
            unPacker.progress.set(totalOutLength);
            accumulator++;
            if (accumulator % Constants.GUI_UPDATES_PER_S == 0) {  // whole second
                double finished = ((double) totalOutLength) / unPacker.getTotalOrigSize();
                double rounded = (double) Math.round(finished * 1000) / 10;
                unPacker.percentage.set(String.valueOf(rounded));
                int newUpdated = (int) (totalOutLength - lastUpdateProgress);
                lastUpdateProgress = unPacker.progress.get();
                int ratio = newUpdated / 1024;
                unPacker.ratio.set(String.valueOf(ratio));

                long timeUsed = accumulator * 1000L / Constants.GUI_UPDATES_PER_S;
                unPacker.timeUsed.set(Util.secondToString((timeUsed + timeOffset) / 1000));
                long expectTime = (unPacker.getTotalOrigSize() - totalOutLength) / ratio / 1024;
                unPacker.timeExpected.set(Util.secondToString(expectTime));

                unPacker.passedLength.set(Util.sizeToReadable(totalOutLength));
            }
        }
    }
}
