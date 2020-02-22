package trashsoftware.winBwz.utility;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * An output stream that output contents to multiple files, separated by size.
 *
 * @author zbh
 * @see java.io.OutputStream
 * @since 0.7.4
 */
public class SeparateOutputStream extends OutputStream {

    /**
     * The count of generated files.
     */
    private int count;

    /**
     * The fixed prefix file name.
     */
    private String prefixName;

    /**
     * The fixed suffix file name (extension).
     */
    private String ext;

    private long blockSize;

    /**
     * The remaining available length of the current writing file.
     */
    private long currentLength;

    private long cumulativeLength;

    /**
     * The output stream of the current writing file.
     */
    private OutputStream currentOutputStream;

    private boolean buffered;

    private int signature;

    /**
     * Creates a new instance of {@code SeparateOutputStream}.
     *
     * @param fileName  the arranged name of the output file
     * @param blockSize the size of each block
     * @param buffered  whether to use a buffered output stream
     * @param signature the digital signature of each section, except the first section
     * @throws IOException if the output files are not writable
     */
    public SeparateOutputStream(String fileName, long blockSize, boolean buffered, int signature) throws IOException {
        this.prefixName = fileName.substring(0, fileName.lastIndexOf("."));
        this.ext = fileName.substring(fileName.lastIndexOf("."));
        this.blockSize = blockSize;
        this.currentLength = blockSize;
        this.buffered = buffered;
        this.signature = signature;
        String name = getCurrentName();
        if (buffered) currentOutputStream = new BufferedOutputStream(new FileOutputStream(name));
        else currentOutputStream = new FileOutputStream(name);
    }

    /**
     * Creates a new instance of {@code SeparateOutputStream}.
     *
     * @param fileName  the arranged name of the output file
     * @param blockSize the size of each block
     * @param buffered  whether to use a buffered output stream
     * @throws IOException if the output files are not writable
     */
    public SeparateOutputStream(String fileName, long blockSize, boolean buffered) throws IOException {
        this.prefixName = fileName.substring(0, fileName.lastIndexOf("."));
        this.ext = fileName.substring(fileName.lastIndexOf("."));
        this.blockSize = blockSize;
        this.currentLength = blockSize;
        this.buffered = buffered;
        String name = getCurrentName();
        if (buffered) currentOutputStream = new BufferedOutputStream(new FileOutputStream(name));
        else currentOutputStream = new FileOutputStream(name);
    }

    /**
     * Writes a byte  into the output stream.
     *
     * @param b the byte
     * @throws IOException if any of the files is not writable
     */
    @Override
    public void write(int b) throws IOException {
        write(new byte[]{(byte) b});
    }

    /**
     * Writes a byte array to the output files, with offset and length editable.
     *
     * @param b   the byte array
     * @param off the offset
     * @param len the count of byte to be written
     * @throws IOException if the output stream is not writable
     */
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        byte[] real = new byte[len];
        System.arraycopy(b, off, real, 0, len);
        write(real);
    }

    /**
     * Writes a byte array into the output stream.
     *
     * @param b the byte array
     * @throws IOException if any of the files is not writable
     */
    @Override
    public void write(byte[] b) throws IOException {
        int remain = b.length;
        while (remain > 0) {
            if (remain <= currentLength) {
                currentOutputStream.write(b, b.length - remain, remain);
                currentLength -= remain;
                remain = 0;
            } else {
                int partLen = (int) currentLength;
                currentOutputStream.write(b, b.length - remain, partLen);
                remain -= partLen;
                currentLength = blockSize;
                String name = getCurrentName();
                currentOutputStream.flush();
                currentOutputStream.close();
                if (buffered) currentOutputStream = new BufferedOutputStream(new FileOutputStream(name));
                else currentOutputStream = new FileOutputStream(name);
                currentOutputStream.write(Bytes.intToBytes32(signature));
//                cumulativeLength += 4;
                currentLength -= 4;
            }
        }
        cumulativeLength += b.length;
    }

    private String getCurrentName() {
        return String.format("%s.%d%s", prefixName, ++count, ext);
    }

    /**
     * Returns the name of the first part of the archive.
     *
     * @return the name of the first part of the archive.
     */
    public String getFirstName() {
        return String.format("%s.%d%s", prefixName, 1, ext);
    }

    /**
     * Returns the current file count.
     *
     * @return the current file count.
     */
    public int getCount() {
        return count;
    }

    /**
     * Flushes this stream.
     *
     * @throws IOException if the last stream cannot be flushed
     */
    @Override
    public void flush() throws IOException {
        currentOutputStream.flush();
    }

    public long getCumulativeLength() {
        return cumulativeLength;
    }

    /**
     * Closes this stream.
     *
     * @throws IOException if the last output stream cannot be closed
     */
    @Override
    public void close() throws IOException {
        currentOutputStream.close();
    }
}
