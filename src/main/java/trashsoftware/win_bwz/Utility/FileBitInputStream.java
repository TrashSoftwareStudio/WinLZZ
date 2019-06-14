package trashsoftware.win_bwz.Utility;

import java.io.IOException;
import java.io.InputStream;

/**
 * An input stream which reads a file in bit level.
 *
 * @author zbh
 * @since 0.4
 */
public class FileBitInputStream {

    private InputStream bis;

    /**
     * The current reading byte.
     */
    private int current;

    /**
     * The current operating position in {@code current}, from left to right.
     */
    private int pointer;

    /**
     * Creates a new instance of {@code FileBitInputStream}.
     *
     * @param bis the {@code BufferedInputStream} which this {@code FileBitInputStream} reads data from
     */
    public FileBitInputStream(InputStream bis) {
        this.bis = bis;
    }

    /**
     * Returns the combination of the next <code>length</code> bits.
     *
     * @param length the length to of the reading
     * @return the combination of the next <code>length</code> bits
     * @throws IOException if the stream is not readable
     */
    public int read(int length) throws IOException {
        int number = 0;
        for (int i = 0; i < length; i++) {
            number = number << 1;
            int r = read();
            if (r == 2) throw new IOException();
            number = number | r;
        }
        return number;
    }

    /**
     * Returns the next bit in the stream {@code bis}, or {@code 2} if the stream ends.
     *
     * @return the next bit in the stream {@code bis}, or {@code 2} if the stream ends.
     * @throws IOException if the input stream is not readable
     */
    public int read() throws IOException {
        int i;
        if (pointer == 0) {
            byte[] b = new byte[1];
            if (bis.read(b) != 1) return 2;
            current = b[0];
            i = (current >> 7) & 1;
        } else {
            i = (current >> (7 - pointer)) & 1;
        }
        if (pointer == 7) pointer = 0;
        else pointer += 1;
        return i;
    }

    /**
     * Closes this {@code FileBitInputStream}.
     *
     * @throws IOException if the {@code bis} cannot be closed
     */
    public void close() throws IOException {
        bis.close();
    }
}
