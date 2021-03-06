package trashsoftware.winBwz.encrypters;

/**
 * An {@code Exception} that will be thrown when the password used for decode does not match the password used while
 * encoding.
 *
 * @author zbh
 * @see Exception
 * @since 0.4
 */
public class WrongPasswordException extends Exception {

    /**
     * Creates a new instance of {@code WrongPasswordException}
     */
    public WrongPasswordException() {
    }
}
