package trashsoftware.win_bwz.utility;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * An output stream that provides the random access of some last written data.
 */
public class FileOutputBufferArray {

    private OutputStream fos;

    private int bufferSize;

    private byte[] array1, array2;

    /**
     * Whether the {@code array2} is the front array.
     */
    private boolean activeArray2;

    /**
     * Whether {@code array1} has been initialized.
     */
    private boolean array1Init = false;

    /**
     * Whether {@code array2} has been initialized.
     */
    private boolean array2Init = false;

    private long index, front = 0;

    /**
     * Creates a new instance of {@code FileOutputBufferArray}.
     *
     * @param fileName   the name or path of the file to be written
     * @param bufferSize maximum access range of history written data
     * @throws IOException if the file is not writable
     */
    public FileOutputBufferArray(String fileName, int bufferSize) throws IOException {
        this.fos = new FileOutputStream(fileName);
        this.bufferSize = bufferSize;
        this.array1 = new byte[bufferSize];
        this.array2 = new byte[bufferSize];
    }

    /**
     * Creates a new instance of {@code FileOutputBufferArray}.
     *
     * @param fos        the input stream
     * @param bufferSize maximum access range of history written data
     */
    public FileOutputBufferArray(OutputStream fos, int bufferSize) {
        this.fos = fos;
        this.bufferSize = bufferSize;
        this.array1 = new byte[bufferSize];
        this.array2 = new byte[bufferSize];
    }

    /**
     * Writes a {@code byte} into the stream.
     *
     * @param b the {@code byte} to be written
     * @throws IOException if the stream is not writable
     */
    public void write(byte b) throws IOException {
        if (index % bufferSize == 0) {
            activeArray2 = !activeArray2;
            if (activeArray2) {
                if (array2Init) fos.write(array2);
                else array2Init = true;
                array2[0] = b;
            } else {
                if (array1Init) fos.write(array1);
                else array1Init = true;
                array1[0] = b;
            }
            front += bufferSize;
        } else {
            if (activeArray2) array2[(int) (index % bufferSize)] = b;
            else array1[(int) (index % bufferSize)] = b;
        }
        index += 1;
    }

    /**
     * Returns a sub-sequence of history written data.
     * <p>
     * This method may throw an {@code IndexOutOfBoundException} if the <code>from</code> is out of the access range.
     *
     * @param from the starting position of the sub-sequence
     * @param to   the ending postition of the sub-sequence
     * @return the sub-sequence
     */
    public byte[] subSequence(long from, long to) {
        int len = (int) (to - from);
        byte[] rtn = new byte[len];
        if (to > index) throw new IndexOutOfBoundsException("Index Out Of Bounds: Index: " + index + ", To: " + to);
        else if (from > to) throw new NegativeArraySizeException("Negative sequence size");
        if (activeArray2) {
            if (front - from <= bufferSize) {
                System.arraycopy(array2, (int) (from % bufferSize), rtn, 0, len);
            } else if (front - from <= bufferSize * 2) {
                int remain = (int) (from % bufferSize);
                int firstLen = Math.min(bufferSize - remain, len);
                System.arraycopy(array1, remain, rtn, 0, firstLen);
                System.arraycopy(array2, 0, rtn, firstLen, len - firstLen);
            } else {
                throw new IndexOutOfBoundsException("Index Out Of Window");
            }
        } else {
            if (front - from <= bufferSize) {
                System.arraycopy(array1, (int) (from % bufferSize), rtn, 0, len);
            } else if (front - from <= bufferSize * 2) {
                int remain = (int) (from % bufferSize);
                int firstLen = Math.min(bufferSize - remain, len);
                System.arraycopy(array2, remain, rtn, 0, firstLen);
                System.arraycopy(array1, 0, rtn, firstLen, len - firstLen);
            } else {
                throw new IndexOutOfBoundsException("Index Out Of Window");
            }
        }

        return rtn;
    }

    /**
     * Returns the current writing position.
     *
     * @return the current writing position
     */
    public long getIndex() {
        return index;
    }

    /**
     * Writes all remaining data in the buffer into the out file.
     *
     * @throws IOException if the {@code fos} is not writable or cannot be flushed
     */
    public void flush() throws IOException {
        int len = (int) (index % bufferSize);
        if (activeArray2) {
            if (array1Init) fos.write(array1);
            fos.write(array2, 0, len);
        } else {
            if (array2Init) fos.write(array2);
            fos.write(array1, 0, len);
        }
        this.fos.flush();
    }

    /**
     * Closes this {@code FileOutputBufferArray}.
     *
     * @throws IOException if the {@code fos} cannot be closed
     */
    public void close() throws IOException {
        fos.close();
    }
}
