package trashsoftware.winBwz.rangeCodec;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class RangeDecoder {
    private InputStream in;

    private long code = 0;
    private long low = 0;
    private long high = RangeCodingConstants.MASK_RANGE;

    public RangeDecoder(InputStream in) {
        this.in = in;
    }

    public void initialize() throws IOException {
        for (int i = 0; i < (RangeCodingConstants.RANGE_BITS + 7) / 8; i++) {
            int b = in.read();
            if (b == -1) throw new EOFException("Unexpected EOF during init");
            code = (code << 8) | (b & 0xFF);
        }
    }

    public int decodeSymbol(FrequencyTable freq) throws IOException {
        long range = high - low + 1;
        int total = freq.getTotal();

        long offset = code - low;
        int value = (int)(((offset + 1) * total - 1) / range);
//        long offset = code - low;
//        int value = (int)((offset * total) / range);

        int symbol = freq.getSymbolFromValue(value);
        int symLow = freq.getSymbolLow(symbol);
        int symHigh = freq.getSymbolHigh(symbol);

        long offsetLow = range * symLow / total;
        long offsetHigh = range * symHigh / total;

        long newLow = low + offsetLow;
        long newHigh = low + offsetHigh - 1;
        if (newHigh < newLow) {
            System.out.printf("Symbol collision in decoder! newLow: %d, newHigh: %d, range: %d, " +
                    "symLow: %d, symHigh: %d, total: %d\n", newLow, newHigh, range, symLow, symHigh, total);
            newHigh = newLow;
        }

        low = newLow & RangeCodingConstants.MASK_RANGE;
        high = newHigh & RangeCodingConstants.MASK_RANGE;

        while ((low >>> (RangeCodingConstants.RANGE_BITS - 8)) ==
                (high >>> (RangeCodingConstants.RANGE_BITS - 8))) {
            int b = in.read();
            if (b == -1) throw new EOFException("Unexpected EOF during decoding");
            code = ((code << 8) | (b & 0xFF)) & RangeCodingConstants.MASK_RANGE;
            low = (low << 8) & RangeCodingConstants.MASK_RANGE;
            high = ((high << 8) | 0xFF) & RangeCodingConstants.MASK_RANGE;
        }

        return symbol;
    }
}
