package trashsoftware.winBwz.rangeCodec;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

public class RangeInputStream extends AbstractRangeDecompressor {

    /**
     * The IO buffer size.
     */
    private static final int bufferSize = 8192;
    
    private final FileChannel fc;

    private final ByteBuffer readBuffer = ByteBuffer.allocate(bufferSize);

    private int bufferIndex = -1;  // not read yet
    
    public RangeInputStream(FileChannel fc, int blockSize) {
        super();
        this.fc = fc;
//        this.result = new int[blockSize];
    }

    /**
     * Reads and returns bytes directly from the input stream.
     *
     * @param length the length of read
     * @return the byte content, null if stream ends
     * @throws IOException if the input stream is not readable
     */
    public byte[] readPlain(int length) throws IOException {
        byte[] b = new byte[length];
        for (int i = 0; i < length; i++) {
            int nxt = loadNextByte();
            if (nxt == -1) return null;  // eof
            b[i] = (byte) nxt;
        }
        return b;
    }
    
//    private void cutTail(int encodedNBytes) throws IOException {
//        while (blockBytesRead < encodedNBytes) {
//            int loaded = loadNextByte();
//            if (loaded == -1) throw new EOFException("Stream does not contain enough bytes.");
//        }
//    }
    
    protected int loadNextByte() throws IOException {
        if (bufferIndex == -1 || bufferIndex >= readBuffer.limit()) {
            readBuffer.clear();
            int read = fc.read(readBuffer);
            if (read <= 0) {
                return -1;
            }
            readBuffer.flip();
            if (read != readBuffer.limit()) readBuffer.limit(read);
            bufferIndex = 0;
        }
        blockBytesRead++;
        return readBuffer.get(bufferIndex++) & 0xff;
    }
}
