package WinLzz.Utility;

import java.io.BufferedInputStream;
import java.io.IOException;

/**
 * An input stream which reads a file in bit level.
 *
 * @author zbh
 * @since 0.4
 */
public class FileBitInputStream {

    private BufferedInputStream bis;

    private int index = 0;

    private String currentByte;

    /**
     * Creates a new instance of {@code FileBitInputStream}.
     *
     * @param bis the {@code BufferedInputStream} which this {@code FileBitInputStream} reads data from
     */
    public FileBitInputStream(BufferedInputStream bis) {
        this.bis = bis;
    }

    /**
     * Returns the next bit in the stream {@code bis}, or {@code 2} if the stream ends.
     *
     * @return the next bit in the stream {@code bis}, or {@code 2} if the stream ends.
     * @throws IOException if the input stream is not readable
     */
    public char read() throws IOException {
        char c;
        if (index % 8 == 0) {
            byte[] b = new byte[1];
            if (bis.read(b) != 1) return '2';
            currentByte = Bytes.byteToBitString(b[0]);
            c = currentByte.charAt(0);
        } else {
            c = currentByte.charAt(index % 8);
        }
        index += 1;
        return c;
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
