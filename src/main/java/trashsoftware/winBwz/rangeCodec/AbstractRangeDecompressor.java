package trashsoftware.winBwz.rangeCodec;

import trashsoftware.winBwz.rangeCodec.freq.FrequencyTable;
import trashsoftware.winBwz.utility.IntArrayOutputStream;

import java.io.EOFException;
import java.io.IOException;

public abstract class AbstractRangeDecompressor {

    protected long code = 0;
    protected long low = 0;
    protected long high = RangeCodingConstants.MASK_RANGE;

    protected FrequencyTable ft;
    protected int eofSig;

    // Out index
//    protected int currentIndex;
    protected int blockBytesRead = 0;
    protected IntArrayOutputStream result = new IntArrayOutputStream();

    public AbstractRangeDecompressor() {
    }
    
    protected int headFixedCodeLen() {
        return (RangeCodingConstants.RANGE_BITS + 7) / 8;
    }

    protected void initialize() throws IOException {
        int fcl = headFixedCodeLen();
        for (int i = 0; i < fcl; i++) {
            int b = loadNextByte();
            if (b == -1) throw new EOFException("Unexpected EOF during init");
            code = (code << 8) | (b & 0xFF);
        }
    }

    /**
     * Update the frequency table. In the adaptive range coding, each block's table is individual.
     */
    public void setFrequencyTable(FrequencyTable ft) {
        this.ft = ft;
    }

    /**
     * Reads the next byte in range (0, 255), or -1 if reaches the end of input.
     * 
     * @return the next byte in range (0, 255), or -1 if reaches the end of input
     * @throws IOException if IO error occurs
     */
    protected abstract int loadNextByte() throws IOException;

    protected int decodeSymbol(FrequencyTable freq) throws IOException {
        long range = high - low + 1;
        int total = freq.getTotal();

        long offset = code - low;
        int value = (int) (((offset + 1) * total - 1) / range);

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

    protected void unCompress(int encodedNBytes) throws IOException {
        initialize();

        while (blockBytesRead < encodedNBytes) {
            int sym = decodeSymbol(ft);
            ft.increment(sym);
            if (sym == eofSig) continue;  // EOF marker

            result.write(sym);
//            currentIndex++;
        }
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

//        currentIndex = 0;
        result.reset();
        blockBytesRead = 0;
        code = 0;
        low = 0;
        high = RangeCodingConstants.MASK_RANGE;

//        long t1 = System.currentTimeMillis();

        try {
            unCompress(encodedNBytes);
        } catch (EOFException eof) {
            System.err.printf("Expected nBytes: %d, actual read: %d",
                    encodedNBytes,
                    blockBytesRead);
            throw eof;
        }

//        System.out.println("Bytes read: " + blockBytesRead);

//        timer += System.currentTimeMillis() - t1;

//        int[] rtn = new int[currentIndex];
//        System.arraycopy(result, 0, rtn, 0, currentIndex);
        return result.toIntArray();
    }
}
