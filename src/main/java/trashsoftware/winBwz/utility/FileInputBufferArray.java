package trashsoftware.winBwz.utility;

import java.io.*;

/**
 * An input stream that provides the random access of some last read data.
 *
 * @author zbh
 * @since 0.4
 */
public class FileInputBufferArray {

    private InputStream bis;

    private int bufferSize, remainSize;

    private byte[] array1, array2;

    /**
     * Whether the {@code array2} is the front array.
     */
    private boolean activeArray2;

    private long length, index;

    /**
     * Creates a new instance of {@code FileInputBufferArray}.
     *
     * @param fis        the primary input stream
     * @param length     the total length of the input stream
     * @param bufferSize the minimum random access range of last read data
     */
    public FileInputBufferArray(InputStream fis, long length, int bufferSize) {
        this.bufferSize = bufferSize;
        this.length = length;
        this.bis = fis;
        array1 = new byte[bufferSize];
        array2 = new byte[bufferSize];
    }

    /**
     * Reads and returns a byte which at index <code>index</code>.
     * <p>
     * This method will throw an {@code IndexOutOfBoundsException} if the <code>index</code> is
     * out of the access range.
     *
     * @param index the access index
     * @return the byte at the position <code>index</code>
     */
    public byte getByte(long index) throws IOException {
        if (index >= this.index) {
            activeArray2 = !activeArray2;
            if (activeArray2) {
                int read = bis.read(array2);
                if (read > 0) {
                    this.index += read;
                    remainSize = read;
                } else if (read < 0) {
                    throw new IndexOutOfBoundsException("Index out of file's length");
                }
            } else {
                int read = bis.read(array1);
                if (read > 0) {
                    this.index += read;
                    remainSize = read;
                } else if (read < 0) {
                    throw new IndexOutOfBoundsException("Index out of file's length");
                }
            }
            return getByte(index);
        } else if (index >= this.index - remainSize) {
            if (activeArray2) return array2[(int) (index - this.index + remainSize)];
            else return array1[(int) (index - this.index + remainSize)];
        } else if (index < this.index - remainSize) {
            if (activeArray2) return array1[(int) (index - this.index + bufferSize + remainSize)];
            else return array2[(int) (index - this.index + bufferSize + remainSize)];
        } else {
            throw new IndexOutOfBoundsException("Index out of buffer range");
        }
    }

    /**
     * Closes this {@code FileInputBufferArray}.
     *
     * @throws IOException if the {@code bis} cannot be closed
     */
    public void close() throws IOException {
        bis.close();
    }

    /**
     * Returns the length of the stream.
     *
     * @return the length of the stream
     */
    public long length() {
        return length;
    }
}
