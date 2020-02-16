package trashsoftware.win_bwz.utility;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class ChannelBitInputStream extends BitInputStream {

    /**
     * The current reading byte.
     */
    private int bits;

    /**
     * The current operating position in {@code current}, from left to right.
     */
    private int bitPos;

    private FileChannel fc;

    private ByteBuffer buffer = ByteBuffer.allocate(1);

    private boolean streamEnds = false;

    public ChannelBitInputStream(FileChannel fileChannel) {
        this.fc = fileChannel;
    }

    @Override
    public int read() throws IOException {
        int res = readBits(1);
        return streamEnds ? 2 : res;
    }

    @Override
    public void alignByte() {
        bitPos = 0;
    }

    /**
     * Returns the combination of the next <code>length</code> bits.
     *
     * @param bitLength the length to of the reading
     * @return the combination of the next <code>length</code> bits
     * @throws IOException if the stream is not readable
     */
    public int read(int bitLength) throws IOException {
        return readBits(bitLength);
    }

    public boolean isStreamEnds() {
        return streamEnds;
    }

    public byte readByte() throws IOException {
        return (byte) readBits(8);
    }

    public void revert(int bitCount) {
        bitPos += bitCount;
    }

    private void loadBits(int leastBitsCount) throws IOException {
        while (bitPos < leastBitsCount) {
            bitPos += 8;
            bits <<= 8;
            if (fc.read(buffer) != 1) {
                buffer.flip();
                streamEnds = true;
            }
            bits |= (buffer.get(0) & 0xff);
        }
    }

    private int readBits(int bitsCount) throws IOException {
        loadBits(bitsCount);
        int res = (bits >> (bitPos - bitsCount)) & Bytes.getAndEr(bitsCount);
        bitPos -= bitsCount;
        return res;
    }

    /**
     * Closes this {@code FileBitInputStream}.
     *
     * @throws IOException if the {@code bis} cannot be closed
     */
    public void close() throws IOException {
        fc.close();
    }
}
