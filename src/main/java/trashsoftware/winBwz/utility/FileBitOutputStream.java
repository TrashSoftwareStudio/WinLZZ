package trashsoftware.winBwz.utility;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * An output stream which writes to a file in bit level.
 *
 * @author zbh
 * @since 0.4
 */
public class FileBitOutputStream {

    private OutputStream bos;

    /**
     * The length written.
     */
    private long length;

    /**
     * The current operating byte.
     */
    private int bits;

    /**
     * The current writing position of the byte {@code current}.
     */
    private int bitPos;

    /**
     * Creates a new {@code FileBitOutputStream} instance.
     *
     * @param fos the {@code OutputStream} to write data in.
     */
    public FileBitOutputStream(OutputStream fos) {
        if (fos instanceof BufferedOutputStream ||
                fos instanceof ByteArrayOutputStream ||
                fos instanceof FixedByteArrayOutputStream) bos = fos;
        else bos = new BufferedOutputStream(fos);
    }

    /**
     * Writes the last {@code numberOfBits} bits of {@code bits} into the output stream.
     *
     * @param bits         the bits to be written
     * @param numberOfBits the number of bits to be written
     * @throws IOException if the output stream is not writable
     */
    public void write(int bits, int numberOfBits) throws IOException {
        writeBits(bits, numberOfBits);
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
        writeBits(bit, 1);
    }

    /**
     * Writes a full byte to the output stream.
     *
     * @param b the byte to be written
     * @throws IOException if the output stream is not writable
     */
    public void writeByte(byte b) throws IOException {
        writeBits(b, 8);
    }

    private void writeBits(int bits, int bitsCount) throws IOException {
        bitPos += bitsCount;
        this.bits <<= bitsCount;
        this.bits |= (bits & Bytes.getAndEr(bitsCount));
        flushBits();
    }

    private void flushBits() throws IOException {
        while (bitPos >= 8) {
            bitPos -= 8;
            byte temp = (byte) (bits >> bitPos);
            bos.write(temp);
            length++;
        }
    }

    public long getLength() {
        return length;
    }

    /**
     * Writes all data remaining in buffer into the output stream.
     *
     * @throws IOException if the {@code bos} cannot be flushed
     */
    public void flush() throws IOException {
        if (bitPos > 0) {
            bits <<= (8 - bitPos);
            bitPos = 0;
            bos.write(bits);
            length++;
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

    public OutputStream getStream() {
        return bos;
    }
}
