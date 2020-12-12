package trashsoftware.winBwz.packer;

/**
 * This class is a sub-exception of {@code RuntimeException}.
 * <p>
 * This exception will be thrown if the {@code UnPacker} has different primary version with the archive file.
 *
 * @author zbh
 * @see java.lang.RuntimeException
 * @since 0.4
 */
public class UnsupportedVersionException extends RuntimeException {

    /**
     * Constructs a new {@code UnsupportedVersionException} instance, with a given error message.
     *
     * @param message the error message
     */
    UnsupportedVersionException(String message) {
        super(message);
    }
}
