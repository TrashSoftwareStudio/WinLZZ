package trashsoftware.win_bwz.Utility;

import trashsoftware.win_bwz.Packer.Packer;

import java.io.*;
import java.util.Deque;
import java.util.zip.CRC32;

/**
 * An extension of {@code InputStream} that takes a queue of files and treat them as a single input stream.
 *
 * @author zbh
 * @see java.io.InputStream
 * @since 0.7.2
 */
public class MultipleInputStream extends InputStream {

    /**
     * The input stream of the current opening file.
     */
    InputStream currentInputStream;

    /**
     * The length of the part of the current opening file that has not been read.
     */
    long currentLength;

    Deque<File> files;

    private CRC32 crc32 = new CRC32();

    private Packer parent;

    boolean buffered;

    /**
     * Creates a new {@code MultipleInputStream} instance.
     *
     * @param files    a queue of input files
     * @param parent   the {@code Packer} instance which launched this {@code MultipleInputStream}.
     * @param buffered whether to use a buffer to read the input
     * @throws IOException if any of the files in <code>files</code> is not readable
     */
    public MultipleInputStream(Deque<File> files, Packer parent, boolean buffered) throws IOException {
        this.files = files;
        this.parent = parent;
        this.buffered = buffered;
        File f = this.files.removeFirst();
        if (parent != null) parent.file.setValue(String.format("%s\\\n%s", f.getParent(), f.getName()));
        currentLength = f.length();
        if (buffered) currentInputStream = new BufferedInputStream(new FileInputStream(f));
        else currentInputStream = new FileInputStream(f);
    }

    /**
     * Creates a new {@code MultipleInputStream} instance.
     * <p>
     * The empty constructor.
     */
    public MultipleInputStream() {
    }

    /**
     * This method does not do anything. Don't use.
     *
     * @return {@code 0}
     */
    @Override
    public int read() {
        return 0;
    }

    public int read(byte[] array, int src, int len) throws IOException {
        if (src != 0)
        throw new IOException("Method unavailable");
        else {
            byte[] realArray = new byte[len];
            int r = read(realArray);
            System.arraycopy(realArray, 0, array, 0, r);
            return r;
        }
    }

    /**
     * Reads bytes into <code>array</code> and returns the total number of bytes read.
     *
     * @param array the byte array to read data in
     * @return the number of bytes read
     * @throws IOException if the input stream is not readable
     */
    @Override
    public int read(byte[] array) throws IOException {
        int remain = array.length;
        while (remain > 0) {
            if (currentLength >= remain) {
                int read = currentInputStream.read(array, array.length - remain, remain);
                currentLength -= read;
                remain -= read;
            } else {
                int read = currentInputStream.read(array, array.length - remain, (int) currentLength);
                remain -= read;
                if (files.isEmpty()) break;
                File f = files.removeFirst();
                if (parent != null) parent.file.setValue(String.format("%s\\\n%s", f.getParent(), f.getName()));
                currentLength = f.length();
                currentInputStream.close();
                if (buffered) currentInputStream = new BufferedInputStream(new FileInputStream(f));
                else currentInputStream = new FileInputStream(f);
            }
        }
        if (remain == 0) crc32.update(array, 0, array.length);
        else if (array.length >= remain) {
            byte[] copy = new byte[array.length - remain];
            System.arraycopy(array, 0, copy, 0, copy.length);
            crc32.update(copy, 0, copy.length);
        }
        return array.length - remain;
    }

    /**
     * Returns the CRC32 checksum of the total input stream.
     *
     * @return the CRC32 checksum of the total input stream
     */
    public long getCrc32Checksum() {
        return crc32.getValue();
    }

    /**
     * Closes the current opening input stream.
     *
     * @throws IOException if any io error occurs
     */
    public void close() throws IOException {
        currentInputStream.close();
    }
}
