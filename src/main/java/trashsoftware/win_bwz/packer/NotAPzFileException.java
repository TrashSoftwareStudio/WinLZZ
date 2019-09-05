package trashsoftware.win_bwz.packer;

/**
 * This class is a sub-exception of {@code Exception}.
 * <p>
 * This exception will be thrown if the archive file does not starts with the .pz file's header.
 *
 * @author zbh
 * @see java.lang.Exception
 * @since 0.6
 */
public class NotAPzFileException extends Exception {

    /**
     * Constructs a new {@code NotAPzFileException} instance, with <code>message</code> as error message.
     *
     * @param message the error message
     */
    NotAPzFileException(String message) {
        super(message);
    }
}
