package trashsoftware.winBwz.longHuffman;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * A Independent input stream that takes an input stream and uncompress data from stream using huffman algorithm.
 *
 * @author zbh
 * @since 0.5.2
 */
public class LongHuffmanInputStream extends LongHuffmanDecoder {

    /**
     * The IO buffer size.
     */
    private static final int bufferSize = 8192;

    private final FileChannel fc;

    private final int[] result;

    private final ByteBuffer readBuffer = ByteBuffer.allocate(bufferSize);

    private int bufferIndex;

    /**
     * Creates a new {@code LongHuffmanInputStream} instance.
     *
     * @param fc           the input stream file channel.
     * @param alphabetSize the alphabet size.
     * @param maxLength    the maximum length of each part of huffman text.
     */
    public LongHuffmanInputStream(FileChannel fc, int alphabetSize, int maxLength) throws IOException {
        super(alphabetSize);
        this.fc = fc;
        this.compressedBitLength = fc.position() * 8;
        this.result = new int[maxLength];
    }

    @Override
    protected void loadBits(int leastPos) throws IOException {

        while (bitPos < leastPos) {
            bitPos += 8;
            bits <<= 8;
            bits |= (readBuffer.array()[bufferIndex++] & 0xff);
            if (bufferIndex >= bufferSize) {
                readBuffer.clear();
                if (fc.read(readBuffer) <= 0) {
                }
                readBuffer.flip();
                bufferIndex = 0;
            }
        }
    }
    
    @Override
    protected void decodeToEof() throws IOException {
        readBuffer.clear();
        if (fc.read(readBuffer) <= 0) {
            throw new RuntimeException();
        }
        readBuffer.flip();
        bufferIndex = 0;
        
        decodeTillEofCore();
    }
    
    @Override
    protected void setInputPosition(long pos) throws IOException {
        fc.position(pos);
    }

    @Override
    protected void appendResult(int code) {
        result[currentIndex++] = code;
    }

    @Override
    public int[] packResult() {
        int[] rtn = new int[currentIndex];
        System.arraycopy(result, 0, rtn, 0, currentIndex);
        return rtn;
    }

    /**
     * Sets up the current bit position.
     *
     * @param compressedBitLength the current bit position.
     */
    @Deprecated
    public void pushCompressedBitLength(long compressedBitLength) {
        this.compressedBitLength += compressedBitLength;
    }

    /**
     * Reads and returns bytes directly from the input stream.
     *
     * @param length the length of read
     * @return the byte content, null if stream ends
     * @throws IOException if the input stream is not readable
     */
    public byte[] readPlain(int length) throws IOException {
        compressedBitLength += length * 8L;
        ByteBuffer buffer = ByteBuffer.allocate(length);
        if (fc.read(buffer) != length) return null;
        return buffer.array();
    }
}
