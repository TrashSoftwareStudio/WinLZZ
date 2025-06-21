package trashsoftware.winBwz.rangeCodec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

public class RangeEncoder {
    private OutputStream out;

    // 32-bit range: low and high define the current coding interval [low, high)
    private long low = 0;
    private long high = RangeCodingConstants.MASK_RANGE;

    // Number of pending underflow bytes (used when low and high converge slowly)
    private int pending = 0;

    public RangeEncoder(OutputStream out) {
        this.out = out;
    }

    public void encodeSymbol(int symbol, FrequencyTable freq) throws IOException {
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
            int topByte = (int)(high >>> (RangeCodingConstants.RANGE_BITS - 8));
            out.write(topByte);

            while (pending-- > 0) {
                out.write(topByte ^ 0xFF);
            }

            low = (low << 8) & RangeCodingConstants.MASK_RANGE;
            high = ((high << 8) | 0xFF) & RangeCodingConstants.MASK_RANGE;
        }
    }

    public void finish() throws IOException {
        for (int i = 0; i < (RangeCodingConstants.RANGE_BITS + 7) / 8; i++) {
            out.write((int)(low >>> (RangeCodingConstants.RANGE_BITS - 8)));
            low = (low << 8) & RangeCodingConstants.MASK_RANGE;
        }
    }

    public static void main(String[] args) throws IOException {
        ByteArrayOutputStream cmpOut = new ByteArrayOutputStream();

        byte[] data = "Missouri river flows into Mississippi river.".getBytes();

//        FrequencyTable freqEnc = new StaticFrequencyTable(257, data);
        FrequencyTable freqEnc = new AdaptiveFrequencyTable(257);
        RangeEncoder encoder = new RangeEncoder(cmpOut);
        for (byte b : data) {
            encoder.encodeSymbol(b & 0xFF, freqEnc);
            freqEnc.increment(b & 0xff);
        }
        encoder.encodeSymbol(256, freqEnc);  // EOF
        encoder.finish();

        byte[] compressed = cmpOut.toByteArray();

        System.out.println(freqEnc);
        System.out.println(Arrays.toString(compressed));
        
        FrequencyTable freqDec = new AdaptiveFrequencyTable(257);

        System.out.println(data.length + " " + compressed.length);

        ByteArrayInputStream bis = new ByteArrayInputStream(compressed);
        ByteArrayOutputStream decOut = new ByteArrayOutputStream();
        RangeDecoder decoder = new RangeDecoder(bis);
        decoder.initialize();
        while (true) {
            int sym = decoder.decodeSymbol(freqDec);
            freqDec.increment(sym);
            
            if (sym == 256) break;  // EOF marker
            decOut.write(sym);
        }
        String dec = decOut.toString();
        System.out.println(dec);
    }
}
