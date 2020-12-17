package trashsoftware.winBwz.utility;

import java.io.IOException;
import java.io.OutputStream;

/**
 * An output stream that keeps track of the number of bytes written.
 */
public class LengthOutputStream extends OutputStream {

    private static final int BUFFER_SIZE = 8192;
    private final OutputStream base;
    private final byte[] buffer;
    private long writtenLength;
    private int bufferIndex;
    private boolean closeOut = true;

    public LengthOutputStream(OutputStream base) {
        this(base, BUFFER_SIZE);
    }

    public LengthOutputStream(OutputStream base, int bufferSize) {
        this.base = base;
        buffer = new byte[bufferSize];
    }

    /**
     * Do not close the base stream while closing this stream.
     */
    public void notCloseOut() {
        closeOut = false;
    }

    /**
     * Returns the length that has been already written to this stream.
     * <p>
     * Note that this length does not necessarily equal to the actual length
     * written to the disk until this stream finishes.
     *
     * @return the length that has been already written to this stream
     */
    public long getWrittenLength() {
        return writtenLength;
    }

    @Override
    public void write(int b) throws IOException {
        buffer[bufferIndex++] = (byte) b;
        writtenLength += 1;
        if (bufferIndex < buffer.length) {
            flushBuffer();
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (off < 0 || len < 0)
            throw new IndexOutOfBoundsException("Negative index or offset");
        if (off > b.length || off + len > b.length)
            throw new IndexOutOfBoundsException("Offset or length outside array.");
        while (len > 0) {
            int writeLen = Math.min(len, buffer.length - bufferIndex);
            System.arraycopy(b, off, buffer, bufferIndex, writeLen);
            len -= writeLen;
            bufferIndex += writeLen;
            off += writeLen;
            writtenLength += writeLen;
            flushBuffer();
        }
    }

    private void flushBuffer() throws IOException {
        base.write(buffer, 0, bufferIndex);
        bufferIndex = 0;
    }

    @Override
    public void flush() throws IOException {
        flushBuffer();
        super.flush();
    }

    @Override
    public void close() throws IOException {
        if (closeOut) {
            base.close();
            super.close();
        }
    }
}
