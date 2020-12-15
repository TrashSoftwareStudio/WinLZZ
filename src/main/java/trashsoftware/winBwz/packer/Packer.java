package trashsoftware.winBwz.packer;

import javafx.beans.property.*;
import trashsoftware.winBwz.gui.graphicUtil.AnnotationNode;

import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;

public abstract class Packer {

    public final ReadOnlyLongWrapper progress = new ReadOnlyLongWrapper();
    public final ReadOnlyStringWrapper percentage = new ReadOnlyStringWrapper();
    public final ReadOnlyStringWrapper ratio = new ReadOnlyStringWrapper();
    public final ReadOnlyStringWrapper file = new ReadOnlyStringWrapper();
    public final ReadOnlyStringWrapper timeUsed = new ReadOnlyStringWrapper();
    public final ReadOnlyStringWrapper timeExpected = new ReadOnlyStringWrapper();
    public final ReadOnlyStringWrapper passedLength = new ReadOnlyStringWrapper();
    public final ReadOnlyStringWrapper cmpLength = new ReadOnlyStringWrapper();
    public final ReadOnlyStringWrapper currentCmpRatio = new ReadOnlyStringWrapper();
    protected final ReadOnlyStringWrapper step = new ReadOnlyStringWrapper();
    protected final ReadOnlyLongWrapper totalOrigLengthWrapper = new ReadOnlyLongWrapper();
    protected final ReadOnlyIntegerWrapper exitStatus = new ReadOnlyIntegerWrapper();
    protected final File[] inFiles;
    protected ResourceBundle bundle;
    /**
     * Total length before compression.
     */
    protected long totalLength;
    /**
     * Archive length after compression.
     */
    protected long compressedLength;
    private String errorMsg;

    public Packer(File[] inFiles) {
        this.inFiles = inFiles;
    }

    public long getTotalOrigSize() {
        return totalLength;
    }

    public long getCompressedLength() {
        return compressedLength;
    }

    public abstract void interrupt();

    public abstract void setCmpLevel(int level);

    public abstract void setAlgorithm(String algCode);

    public abstract void setThreads(int threads);

    public abstract void setPartSize(long partSize);

    public abstract void setEncrypt(String password, int encryptLevel, String encAlg, String passAlg);

    public abstract void setAnnotation(AnnotationNode annotation) throws IOException;

    /**
     * Builds the file structure.
     * <p>
     * After this method being called, total original size should be produced.
     */
    public abstract void build();

    public abstract void pack(String outFileName, int windowSize, int bufferSize) throws Exception;

    public ReadOnlyLongProperty progressProperty() {
        return progress;
    }

    public ReadOnlyStringProperty stepProperty() {
        return step;
    }

    public ReadOnlyStringProperty fileProperty() {
        return file;
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

    public ReadOnlyStringProperty compressedSizeProperty() {
        return cmpLength;
    }

    public ReadOnlyLongProperty totalOrigLengthProperty() {
        return totalOrigLengthWrapper;
    }

    public ReadOnlyStringProperty currentCmpRatioProperty() {
        return currentCmpRatio;
    }

    public ReadOnlyIntegerWrapper exitStatusProperty() {
        return exitStatus;
    }

    public void setError(String msg, int status) {
        exitStatus.set(status);
        errorMsg = msg;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    /**
     * Sets the {@code ResourceBundle} language loader.
     * <p>
     * language loader is used for displaying text in different languages on the GUI.
     *
     * @param bundle the language loader.
     */
    public void setResourceBundle(ResourceBundle bundle) {
        this.bundle = bundle;
    }
}
