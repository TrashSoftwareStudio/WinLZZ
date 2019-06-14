package trashsoftware.win_bwz.Utility;

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
     * The current operating byte.
     */
    private int current;

    /**
     * The current writing position of the byte {@code current}.
     */
    private int pointer;

    /**
     * Creates a new {@code FileBitOutputStream} instance.
     *
     * @param bos the {@code BufferedOutputStream} to write data in.
     */
    public FileBitOutputStream(BufferedOutputStream bos) {
        this.bos = bos;
    }

    /**
     * Writes the last {@code numberOfBits} bits of {@code bits} into the output stream.
     *
     * @param bits         the bits to be written
     * @param numberOfBits the number of bits to be written
     * @throws IOException if the output stream is not writable
     */
    public void write(int bits, int numberOfBits) throws IOException {
        for (int i = 0; i < numberOfBits; i++) {
            write(bits >> (numberOfBits - i - 1));
        }
    }

    /**
     * Writes the one-bit value to the output stream.
     * <p>
     * This method writes the {@code current} to the output stream if the {@code current} consists of full 8 bits,
     * or adds the <code>bit</code> to the {@code current}.
     *
     * @param bit the one-bit value to be written
     * @throws IOException if the output stream is not writable
     */
    public void write(int bit) throws IOException {
        current = current << 1;
        current = current | bit;
        if (pointer != 7) {
            pointer += 1;
        } else {
            bos.write((byte) current);
            pointer = 0;
        }
    }

    /**
     * Writes a full byte to the output stream.
     *
     * @param b the byte to be written
     * @throws IOException if the output stream is not writable
     */
    public void writeByte(byte b) throws IOException {
        for (int i = 0; i < 8; i++) {
            write(b >> (7 - i) & 1);
        }
    }

    /**
     * Writes all data remaining in buffer into the output stream.
     *
     * @throws IOException if the {@code bos} cannot be flushed
     */
    public void flush() throws IOException {
        if (pointer != 0) {
            current = current << (8 - pointer);
            bos.write((byte) current);
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
