package trashsoftware.winBwz.core;

/**
 * A stream that drives a {@code PzPacker} and provides its real-time progress.
 */
public interface ProgressedStream {

    /**
     * Returns the number of input bytes that already been processed.
     * <p>
     * When the process finished, this should be equal to the total original size.
     *
     * @return the number of input bytes that already been processed
     */
    long getInputSize();

    /**
     * Returns the real-time length of output.
     *
     * @return the real-time length of output.
     */
    long getOutputSize();
}
