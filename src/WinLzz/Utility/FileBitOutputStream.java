package WinLzz.Utility;

import java.io.BufferedOutputStream;
import java.io.IOException;

/**
 * An output stream which writes to a file in bit level.
 *
 * @author zbh
 * @since 0.4
 */
public class FileBitOutputStream {

    private BufferedOutputStream bos;

    /**
     * The buffer array.
     */
    private char[] currentByte = new char[8];

    /**
     * The current using position in {@code currentByte}.
     */
    private short pointer = 0;

    /**
     * Creates a new {@code FileBitOutputStream} instance.
     *
     * @param bos the {@code BufferedOutputStream} to write data in.
     */
    public FileBitOutputStream(BufferedOutputStream bos) {
        this.bos = bos;
    }

    /**
     * Writes a bit into the stream.
     *
     * @param bit the bit to be written
     * @throws IOException if the stream is not writable
     */
    public void write(char bit) throws IOException {
        currentByte[pointer] = bit;
        if (pointer != 7) {
            pointer += 1;
        } else {
            bos.write(charArrayToByte());
            pointer = 0;
        }
    }

    /**
     * Writes a sequence of bits in to the stream.
     *
     * @param s the sequence of bits
     * @throws IOException if the stream is not writable
     */
    public void write(String s) throws IOException {
        for (int i = 0; i < s.length(); i++) write(s.charAt(i));
    }

    private byte charArrayToByte() {
        return Bytes.bitStringToByte(String.valueOf(currentByte));
    }

    /**
     * Writes all data remaining in buffer into the output stream.
     *
     * @throws IOException if the {@code bos} cannot be flushed
     */
    public void flush() throws IOException {
        if (pointer != 0) {
            char[] remain = new char[pointer];
            System.arraycopy(currentByte, 0, remain, 0, pointer);
            byte b = Bytes.bitStringToByteNo8(String.valueOf(remain));
            bos.write(b);
        }
        bos.flush();
    }

    /**
     * Closes this {@code FileBitOutputStream}.
     *
     * @throws IOException if the {@code bis} cannot be closed
     */
    public void close() throws IOException {
        bos.close();
    }
}
