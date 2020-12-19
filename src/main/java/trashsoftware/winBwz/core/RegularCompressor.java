package trashsoftware.winBwz.core;

import trashsoftware.winBwz.packer.pz.PzPacker;
import trashsoftware.winBwz.utility.Util;

import java.util.TimerTask;

public abstract class RegularCompressor implements Compressor {

    protected PzPacker packer;
    protected long totalLength;

    /**
     * Time used by any launching process before the actual compress process starts.
     */
    protected long timeOffset;

    public RegularCompressor(long totalLength) {
        this.totalLength = totalLength;
    }

    @Override
    public void setPacker(PzPacker packer) {
        this.packer = packer;
    }

    /**
     * Returns the currently processing position of the original file.
     *
     * @return the currently processing position of the original file
     */
    protected abstract long getPosition();

    public class CompTimerTask extends TimerTask {
        private int accumulator;

        private long lastUpdateProgress;

        @Override
        public void run() {
            long position = getPosition();
            packer.progress.set(position);
            accumulator++;
            if (accumulator % Constants.GUI_UPDATES_PER_S == 0) {  // whole second
                double finished = ((double) position) / totalLength;
                double rounded = (double) Math.round(finished * 1000) / 10;
                packer.percentage.set(String.valueOf(rounded));
                int newUpdated = (int) (position - lastUpdateProgress);
                lastUpdateProgress = position;
                int ratio = newUpdated / 1024;
                packer.ratio.set(String.valueOf(ratio));

                long timeUsed = accumulator * 1000L / Constants.GUI_UPDATES_PER_S;
                packer.timeUsed.set(Util.secondToString((timeUsed + timeOffset) / 1000));
                long expectTime = (totalLength - position) / ratio / 1024;
                packer.timeExpected.set(Util.secondToString(expectTime));

                packer.passedLength.set(Util.sizeToReadable(position));
            }
        }
    }
}
