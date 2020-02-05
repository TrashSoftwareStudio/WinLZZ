package trashsoftware.win_bwz.utility;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * An input stream which reads a file in bit level.
 *
 * @author zbh
 * @since 0.4
 */
public class FileBitInputStream {

    private BufferedInputStream bis;

    /**
     * The current reading byte.
     */
    private int bits;

    /**
     * The current operating position in {@code current}, from left to right.
     */
    private int bitPos;

    private byte[] buffer1 = new byte[1];

    private boolean streamEnds = false;

    /**
     * Creates a new instance of {@code FileBitInputStream}.
     *
     * @param fis the {@code InputStream} which this {@code FileBitInputStream} reads data from
     */
    public FileBitInputStream(InputStream fis) {
        if (fis instanceof BufferedInputStream) this.bis = (BufferedInputStream) fis;
        else this.bis = new BufferedInputStream(fis);
    }

    /**
     * Returns the combination of the next <code>length</code> bits.
     *
     * @param length the length to of the reading
     * @return the combination of the next <code>length</code> bits
     * @throws IOException if the stream is not readable
     */
    public int read(int length) throws IOException {
        return readBits(length);
    }

    /**
     * Returns the next bit in the stream {@code bis}, or {@code 2} if the stream ends.
     *
     * @return the next bit in the stream {@code bis}, or {@code 2} if the stream ends.
     * @throws IOException if the input stream is not readable
     */
    public int read() throws IOException {
//        int i;
//        if (bitPos == 0) {
//            byte[] b = new byte[1];
//            if (bis.read(b) != 1) return 2;
//            bits = b[0];
//            i = (bits >> 7) & 1;
//        } else {
//            i = (bits >> (7 - bitPos)) & 1;
//        }
//        if (bitPos == 7) bitPos = 0;
//        else bitPos += 1;
//        return i;
        int i = readBits(1);
        return streamEnds ? 2 : i;
    }

    public byte readByte() throws IOException {
        return (byte) readBits(8);
    }

    private void loadBits(int leastBitsCount) throws IOException {
        while (bitPos < leastBitsCount) {
            bitPos += 8;
            bits <<= 8;
            if (bis.read(buffer1) != 1) streamEnds = true;
            bits |= (buffer1[0] & 0xff);
        }
    }

    private int readBits(int bitsCount) throws IOException {
        loadBits(bitsCount);
        int andEr = Bytes.getAndEr(bitsCount);
        int res = (bits >> (bitPos - bitsCount)) & andEr;
        bitPos -= bitsCount;
        return res;
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
