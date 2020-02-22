package trashsoftware.winBwz.utility;

import java.io.IOException;

public abstract class BitInputStream {

    /**
     * Returns the next bit, or {@code 2} if the stream ends.
     *
     * @return the next bit, or {@code 2} if the stream ends.
     * @throws IOException if the input stream is not readable
     */
    public abstract int read() throws IOException;

    /**
     * Skips the bits currently in buffer
     */
    public abstract void alignByte();
}
