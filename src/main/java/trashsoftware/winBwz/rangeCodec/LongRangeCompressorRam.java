package trashsoftware.winBwz.rangeCodec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class LongRangeCompressorRam extends AbstractRangeCompressor {
    
//    ByteArrayOutputStream out;
    private final int[] fullText;
    private final int textLength;

    public LongRangeCompressorRam(int[] fullText, FrequencyTable freq, int eofSig, int textLength) {
        super(eofSig);
        this.fullText = fullText;
        this.textLength = textLength;
        
        setFreq(freq);
    }

    public byte[] compress() {
        try {
            out = new ByteArrayOutputStream();

            for (int i = 0; i < textLength; i++) {
                int b = fullText[i];
                encodeSymbol(b);
                freq.increment(b);
            }
            encodeSymbol(eofSig);
            finish();

            return ((ByteArrayOutputStream) out).toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("No IO here.");
        }
    }
}
