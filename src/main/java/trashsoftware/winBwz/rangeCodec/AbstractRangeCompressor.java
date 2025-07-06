package trashsoftware.winBwz.rangeCodec;

import trashsoftware.winBwz.rangeCodec.freq.FrequencyTable;

import java.io.IOException;
import java.io.OutputStream;

public class AbstractRangeCompressor {

    // 32-bit range: low and high define the current coding interval [low, high)
    protected long low = 0;
    protected long high = RangeCodingConstants.MASK_RANGE;

    // Number of pending underflow bytes (used when low and high converge slowly)
    protected int pending = 0;

    protected FrequencyTable freq;
    protected final int eofSig;
    protected boolean collapse = false;

    protected OutputStream out;
    protected long compressedLength;

    public AbstractRangeCompressor(int eofSig) {
        this.eofSig = eofSig;
    }

    public void setFreq(FrequencyTable freq) {
        this.freq = freq;
    }

    protected void encodeSymbol(int symbol) throws IOException {
        long range = high - low + 1;
//        System.out.print(range + " ");
        int total = freq.getTotal();
        int symLow = freq.getSymbolLow(symbol);
        int symHigh = freq.getSymbolHigh(symbol);

        // Safe: MAX_FREQ and MAX_RANGE ensure this product won’t overflow
        long offsetLow = range * symLow / total;
        long offsetHigh = range * symHigh / total;

        long newLow = low + offsetLow;
        long newHigh = low + offsetHigh - 1;

        if (newHigh < newLow) {
            collapse = true;
            System.out.printf("\nSymbol collision in compressor! symbol: %d, newLow: %d, newHigh: %d, range: %d, " +
                    "symLow: %d, symHigh: %d, total: %d\n", symbol, newLow, newHigh, range, symLow, symHigh, total);
            System.out.printf("Adjacent symbols: {%d: (%d, %d), %d: (%d, %d), %d: (%d, %d)} \n",
                    symbol - 1, freq.getSymbolLow(symbol - 1), freq.getSymbolHigh(symbol - 1),
                    symbol, freq.getSymbolLow(symbol), freq.getSymbolHigh(symbol),
                    symbol + 1, freq.getSymbolLow(symbol + 1), freq.getSymbolHigh(symbol + 1));
            newHigh = newLow;
        }

        low = newLow & RangeCodingConstants.MASK_RANGE;
        high = newHigh & RangeCodingConstants.MASK_RANGE;

        while ((low >>> (RangeCodingConstants.RANGE_BITS - 8)) ==
                (high >>> (RangeCodingConstants.RANGE_BITS - 8))) {
            int topByte = (int) (high >>> (RangeCodingConstants.RANGE_BITS - 8));
            out.write(topByte);
            compressedLength++;

            while (pending-- > 0) {
                out.write(topByte ^ 0xFF);
                compressedLength++;
            }

            low = (low << 8) & RangeCodingConstants.MASK_RANGE;
            high = ((high << 8) | 0xFF) & RangeCodingConstants.MASK_RANGE;
        }

//        // Emergency flush if range is too small
//        if ((high - low + 1) < RangeCodingConstants.EMERGENCY_FLUSH_THRESHOLD) {
//            int topByte = (int) (high >>> (RangeCodingConstants.RANGE_BITS - 8));
//            out.write(topByte);
//            while (pending-- > 0)
//                out.write(topByte ^ 0xFF);
//            low = (low << 8) & RangeCodingConstants.MASK_RANGE;
//            high = ((high << 8) | 0xFF) & RangeCodingConstants.MASK_RANGE;
//        }
    }

    public void finish() throws IOException {
        for (int i = 0; i < (RangeCodingConstants.RANGE_BITS + 7) / 8; i++) {
            out.write((int) (low >>> (RangeCodingConstants.RANGE_BITS - 8)));
            compressedLength++;
            low = (low << 8) & RangeCodingConstants.MASK_RANGE;
        }
    }

    public boolean isCollapsed() {
        return collapse;
    }

    public long getCompressedLength() {
        return compressedLength;
    }
}
