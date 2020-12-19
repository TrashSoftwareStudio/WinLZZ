package trashsoftware.winBwz.core;

import trashsoftware.winBwz.packer.pz.PzUnPacker;
import trashsoftware.winBwz.utility.Util;

import java.util.TimerTask;

public abstract class RegularDeCompressor implements DeCompressor {

    protected PzUnPacker unPacker;
    protected long timeOffset;

    @Override
    public void setUnPacker(PzUnPacker unPacker) {
        this.unPacker = unPacker;
    }

    /**
     * Returns the currently processing position of the uncompressed file.
     *
     * @return the currently processing position of the uncompressed file
     */
    public abstract long getPosition();

    public class DeCompTimerTask extends TimerTask {
        private int accumulator;
        private long lastUpdateProgress;

        @Override
        public void run() {
            long position = getPosition();
            long totalProgress = unPacker.totalProgressProperty().get();
            unPacker.progress.set(position);
            accumulator++;
            if (accumulator % Constants.GUI_UPDATES_PER_S == 0) {  // whole second
                double finished = ((double) position) / totalProgress;
                double rounded = (double) Math.round(finished * 1000) / 10;
                unPacker.percentage.set(String.valueOf(rounded));
                int newUpdated = (int) (position - lastUpdateProgress);
                lastUpdateProgress = unPacker.progress.get();
                int ratio = newUpdated / 1024;
                unPacker.ratio.set(String.valueOf(ratio));

                long timeUsed = accumulator * 1000L / Constants.GUI_UPDATES_PER_S;
                unPacker.timeUsed.set(Util.secondToString((timeUsed + timeOffset) / 1000));
                long expectTime = (totalProgress - position) / ratio / 1024;
                unPacker.timeExpected.set(Util.secondToString(expectTime));

                unPacker.passedLength.set(Util.sizeToReadable(position));
            }
        }
    }
}
