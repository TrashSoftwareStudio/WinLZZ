package trashsoftware.winBwz.rangeCodec;

import java.io.ByteArrayOutputStream;

public class LongRangeCompressorRam {

    // 32-bit range: low and high define the current coding interval [low, high)
    private long low = 0;
    private long high = RangeCodingConstants.MASK_RANGE;

    // Number of pending underflow bytes (used when low and high converge slowly)
    private int pending = 0;

    FrequencyTable freq;
    private final int eofSig;
    ByteArrayOutputStream out;
    private final int[] fullText;

    public LongRangeCompressorRam(int[] fullText, FrequencyTable freq, int eofSig) {
        this.freq = freq;
        this.eofSig = eofSig;
        this.fullText = fullText;
    }

    public byte[] compress() {
        out = new ByteArrayOutputStream();

        for (int b : fullText) {
            encodeSymbol(b);
            freq.increment(b);
        }
        encodeSymbol(eofSig);
        finish();

        return out.toByteArray();
    }

    private void encodeSymbol(int symbol) {
        long range = high - low + 1;
        int total = freq.getTotal();
        int symLow = freq.getSymbolLow(symbol);
        int symHigh = freq.getSymbolHigh(symbol);

        // Safe: MAX_FREQ and MAX_RANGE ensure this product won’t overflow
        long offsetLow = range * symLow / total;
        long offsetHigh = range * symHigh / total;

        long newLow = low + offsetLow;
        long newHigh = low + offsetHigh - 1;

        if (newHigh < newLow) newHigh = newLow;

        low = newLow & RangeCodingConstants.MASK_RANGE;
        high = newHigh & RangeCodingConstants.MASK_RANGE;

        // Flush stable top byte (39-bit range → top 8 bits in bits 31–38)
        while ((low >>> (RangeCodingConstants.RANGE_BITS - 8)) ==
                (high >>> (RangeCodingConstants.RANGE_BITS - 8))) {
            int topByte = (int) (high >>> (RangeCodingConstants.RANGE_BITS - 8));
            out.write(topByte);

            while (pending-- > 0) {
                out.write(topByte ^ 0xFF);
            }

            low = (low << 8) & RangeCodingConstants.MASK_RANGE;
            high = ((high << 8) | 0xFF) & RangeCodingConstants.MASK_RANGE;
        }
    }

    public void finish() {
        for (int i = 0; i < (RangeCodingConstants.RANGE_BITS + 7) / 8; i++) {
            out.write((int) (low >>> (RangeCodingConstants.RANGE_BITS - 8)));
            low = (low << 8) & RangeCodingConstants.MASK_RANGE;
        }
    }
}
