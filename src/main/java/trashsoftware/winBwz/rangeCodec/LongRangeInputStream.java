package trashsoftware.winBwz.rangeCodec;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

public class LongRangeInputStream {

    /**
     * The IO buffer size.
     */
    private static final int bufferSize = 8192;
    
    private final FileChannel fc;
    private FrequencyTable ft;
    private int eofSig;

    private final int[] result;

    // Out index
    private int currentIndex;

    private final ByteBuffer readBuffer = ByteBuffer.allocate(bufferSize);

    private int bufferIndex = -1;  // not read yet
    private int blockBytesRead = 0;

    private long code = 0;
    private long low = 0;
    private long high = RangeCodingConstants.MASK_RANGE;
    
    public LongRangeInputStream(FileChannel fc, int blockSize) {
        this.fc = fc;
        this.result = new int[blockSize];
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

    /**
     * Update the frequency table. In the adaptive range coding, each block's table is individual.
     */
    public void setFrequencyTable(FrequencyTable ft) {
        this.ft = ft;
    }

    /**
     * Reads and uncompress the range coded file until reaches the next endSig.
     *
     * @param encodedNBytes number of bytes that should be read in this stream
     * @param endSig        the EOF character.
     * @return The uncompressed text.
     * @throws IOException If the file is not readable.
     */
    public int[] readNextCompressedBlock(int encodedNBytes, int endSig) throws IOException {
        this.eofSig = endSig;
        
        currentIndex = 0;
        blockBytesRead = 0;
        code = 0;
        low = 0;
        high = RangeCodingConstants.MASK_RANGE;

//        long t1 = System.currentTimeMillis();

        try {
            unCompress(encodedNBytes);
//            int begin = Math.max(0, bufferIndex - 8);
//            int end = Math.min(bufferIndex + 8, readBuffer.limit());
//            byte[] sub = Arrays.copyOfRange(readBuffer.array(), begin, end);
//            System.out.println(Arrays.toString(sub) + ", " + 
//                    Arrays.toString(Arrays.copyOfRange(result, currentIndex - 8, currentIndex)));
        } catch (EOFException eof) {
            System.err.printf("Expected nBytes: %d, actual read: %d, at buffer index %d, surrounding bytes: \n", 
                    encodedNBytes,
                    blockBytesRead,
                    bufferIndex);
            int begin = Math.max(0, bufferIndex - 8);
            int end = Math.min(bufferIndex + 8, readBuffer.limit());
            byte[] sub = Arrays.copyOfRange(readBuffer.array(), begin, end);
            System.err.println(Arrays.toString(sub) + ", " +
                    Arrays.toString(Arrays.copyOfRange(result, currentIndex - 8, currentIndex)));
            throw eof;
        }

//        System.out.println("Bytes read: " + blockBytesRead);

//        timer += System.currentTimeMillis() - t1;

        int[] rtn = new int[currentIndex];
        System.arraycopy(result, 0, rtn, 0, currentIndex);
        return rtn;
    }
    
    private void unCompress(int encodedNBytes) throws IOException {
        initialize();
        
        while (blockBytesRead < encodedNBytes) {
            int sym = decodeSymbol(ft);
            ft.increment(sym);
            if (sym == eofSig) continue;  // EOF marker

            result[currentIndex++] = sym;
//            if (blockBytesRead > encodedNBytes) {
//                throw new EOFException("Range coded stream is drained before encountering EOF");
//            }
        }
        
//        cutTail(encodedNBytes);
    }
    
    private void cutTail(int encodedNBytes) throws IOException {
        while (blockBytesRead < encodedNBytes) {
            int loaded = loadNextByte();
            if (loaded == -1) throw new EOFException("Stream does not contain enough bytes.");
        }
    }

    private void initialize() throws IOException {
        int n = (RangeCodingConstants.RANGE_BITS + 7) / 8;

        for (int i = 0; i < n; i++) {
            int b = loadNextByte();
            code = (code << 8) | (b & 0xFF);
        }
    }

    public int decodeSymbol(FrequencyTable freq) throws IOException {
        long range = high - low + 1;
        int total = freq.getTotal();

        long offset = code - low;
        int value = (int)(((offset + 1) * total - 1) / range);

        int symbol = freq.getSymbolFromValue(value);
        int symLow = freq.getSymbolLow(symbol);
        int symHigh = freq.getSymbolHigh(symbol);

        long offsetLow = range * symLow / total;
        long offsetHigh = range * symHigh / total;

        long newLow = low + offsetLow;
        long newHigh = low + offsetHigh - 1;
        if (newHigh < newLow) newHigh = newLow;

        low = newLow & RangeCodingConstants.MASK_RANGE;
        high = newHigh & RangeCodingConstants.MASK_RANGE;

        while ((low >>> (RangeCodingConstants.RANGE_BITS - 8)) ==
                (high >>> (RangeCodingConstants.RANGE_BITS - 8))) {
            int b = loadNextByte();
            if (b == -1) throw new EOFException("Unexpected EOF during decoding");
            code = ((code << 8) | (b & 0xFF)) & RangeCodingConstants.MASK_RANGE;
            low = (low << 8) & RangeCodingConstants.MASK_RANGE;
            high = ((high << 8) | 0xFF) & RangeCodingConstants.MASK_RANGE;
        }

        return symbol;
    }
    
    private int loadNextByte() throws IOException {
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
