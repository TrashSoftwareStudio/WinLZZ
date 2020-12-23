package trashsoftware.winBwz.packer;

import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.ReadOnlyLongWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import trashsoftware.winBwz.core.Constants;
import trashsoftware.winBwz.core.DeCompressor;
import trashsoftware.winBwz.utility.Util;

import java.io.IOException;
import java.util.ResourceBundle;
import java.util.TimerTask;

public abstract class UnPacker {

    public final ReadOnlyLongWrapper totalProgress = new ReadOnlyLongWrapper();
    public final ReadOnlyLongWrapper progress = new ReadOnlyLongWrapper();
    public final ReadOnlyStringWrapper percentage = new ReadOnlyStringWrapper();
    public final ReadOnlyStringWrapper ratio = new ReadOnlyStringWrapper();
    public final ReadOnlyStringWrapper timeUsed = new ReadOnlyStringWrapper();
    public final ReadOnlyStringWrapper timeExpected = new ReadOnlyStringWrapper();
    public final ReadOnlyStringWrapper passedLength = new ReadOnlyStringWrapper();
    public final ReadOnlyStringWrapper currentFile = new ReadOnlyStringWrapper();
    protected final String packName;
    protected final ReadOnlyStringWrapper step = new ReadOnlyStringWrapper();
    protected final ResourceBundle bundle;
    protected String failInfo;
    protected long timeOffset;

    public UnPacker(String packName, ResourceBundle resourceBundle) {
        this.packName = packName;
        this.bundle = resourceBundle;
    }

    /**
     * Reads the archive information from the archive file to this {@code UnPacker} instance.
     *
     * @throws IOException if the archive file is not readable,
     *                     or any error occurs during reading.
     *                     //     * @throws NotAPzFileException if the archive file is not a pz archive,
     *                     //     *                             or the primary version of this archive file is older than 20.
     */
    public abstract void readInfo() throws IOException;

    public abstract boolean testPack();

    public abstract void unCompressAll(String targetDir) throws Exception;

    public abstract void unCompressFrom(String targetDir, CatalogNode node) throws Exception;

    public abstract String getAlg();

    public abstract String getAnnotation();

    public abstract void readFileStructure() throws Exception;

    public abstract CatalogNode getRootNode();

    public abstract void interrupt();

    public abstract long getTotalOrigSize();

    public abstract long getDisplayArchiveLength();

    public abstract boolean isSeparated();

    public abstract long getCreationTime();

    public abstract int getFileCount();

    public abstract int getDirCount();

    public abstract void setThreads(int threads);

    public abstract void setPassword(String password) throws Exception;

    public abstract boolean isPasswordSet();

    public abstract int getEncryptLevel();

    public abstract void close() throws IOException;

    public ReadOnlyLongProperty totalProgressProperty() {
        return totalProgress;
    }

    public ReadOnlyLongProperty progressProperty() {
        return progress;
    }

    public ReadOnlyStringProperty percentageProperty() {
        return percentage;
    }

    public ReadOnlyStringProperty ratioProperty() {
        return ratio;
    }

    public ReadOnlyStringProperty timeUsedProperty() {
        return timeUsed;
    }

    public ReadOnlyStringProperty timeExpectedProperty() {
        return timeExpected;
    }

    public ReadOnlyStringProperty passedLengthProperty() {
        return passedLength;
    }

    public ReadOnlyStringProperty stepProperty() {
        return step;
    }

    public ReadOnlyStringProperty fileProperty() {
        return currentFile;
    }

    public boolean hasSecondaryProgress() {
        return false;
    }

    public ReadOnlyLongProperty secondaryProgressProperty() {
        return null;
    }

    public ReadOnlyLongProperty secondaryTotalProgressProperty() {
        return null;
    }

    public String getFailInfo() {
        return failInfo;
    }

    public void setFailInfo(String failInfo) {
        this.failInfo = failInfo;
    }

    public abstract class UnpTimerTask extends TimerTask {
        protected DeCompressor deCompressor;
        protected long lastUpdateProgress;
        protected int accumulator;

        public synchronized void setDeCompressor(DeCompressor deCompressor) {
            this.deCompressor = deCompressor;
        }

        protected synchronized void updateTimer(long position) {
            long totalLength = totalProgress.get();
            double finished = ((double) position) / totalLength;
            double rounded = (double) Math.round(finished * 1000) / 10;
            percentage.set(String.valueOf(rounded));
            int newUpdated = (int) (position - lastUpdateProgress);
            lastUpdateProgress = position;
            int ratioV = newUpdated / 1024;
            passedLength.set(Util.sizeToReadable(position));

            long timeUsedV = accumulator * 1000L / Constants.GUI_UPDATES_PER_S;
            timeUsed.set(Util.secondToString((timeUsedV + timeOffset) / 1000));

            if (ratioV > 0) {
                ratio.set(String.valueOf(ratioV));
                long expectTime = (totalLength - position) / ratioV / 1024;
                timeExpected.set(Util.secondToString(expectTime));
            }
        }
    }
}
