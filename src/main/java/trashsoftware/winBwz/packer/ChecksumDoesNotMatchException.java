package trashsoftware.winBwz.packer;

/**
 * This class is a sub-exception of {@code Exception}.
 * <p>
 * This exception will be thrown if and only if the extracted content does not generate a same checksum (usually
 * CRC32) with the original checksum stored in the archive.
 *
 * @author zbh
 * @see java.lang.Exception
 * @since 0.7.4
 */
public class ChecksumDoesNotMatchException extends Exception {

    /**
     * Creates a new instance of {@code ChecksumDoesNotMatchException}.
     *
     * @param message the error message
     */
    public ChecksumDoesNotMatchException(String message) {
        super(message);
    }
}
