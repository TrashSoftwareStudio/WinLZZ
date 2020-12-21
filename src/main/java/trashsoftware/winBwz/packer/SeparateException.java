package trashsoftware.winBwz.packer;

/**
 * An exception that represent the case of error that the file structure information cannot be contained in
 * a specific part of the separated archive.
 */
public class SeparateException extends Exception {

    private final long bytesRequired;

    public SeparateException(long bytesRequired) {
        this("", bytesRequired);
    }

    public SeparateException(String msg, long bytesRequired) {
        super(msg);

        this.bytesRequired = bytesRequired;
    }

    public long getBytesRequired() {
        return bytesRequired;
    }
}
