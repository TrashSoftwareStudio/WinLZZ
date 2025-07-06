package trashsoftware.winBwz.core.bwz.lzp;

import java.io.ByteArrayOutputStream;
import java.util.BitSet;

public class LZPMatcher {

    static final int CONTEXT_BITS = 24; // order-3 context
    static final int CONTEXT_SIZE = 1 << CONTEXT_BITS;
    
    private final byte[] buffer;
    private final int beginIndex;
    private final int length;
    
    public LZPMatcher(byte[] buffer, int beginIndex, int length) {
        this.buffer = buffer;
        this.beginIndex = beginIndex;
        this.length = length;
    }

    /**
     * Order-3 LZP encode
     */
    public LZPEncoded encode() {
        byte[] predictor = new byte[CONTEXT_SIZE];
        boolean[] initialized = new boolean[CONTEXT_SIZE];
        BitSet flags = new BitSet(length);
        ByteArrayOutputStream mismatches = new ByteArrayOutputStream();

        int ctx = 0;
        for (int i = beginIndex; i < beginIndex + length; i++) {
            int ctxIdx = ctx & (CONTEXT_SIZE - 1);
            byte predicted = predictor[ctxIdx];
            if (initialized[ctxIdx] && predicted == buffer[i]) {
                flags.set(i);
            } else {
                flags.clear(i);
                mismatches.write(buffer[i]);
                predictor[ctxIdx] = buffer[i];
                initialized[ctxIdx] = true;
            }
            // Update context: shift in new byte
            ctx = ((ctx << 8) | (buffer[i] & 0xFF)) & 0xFFFFFF;
        }

        return new LZPEncoded(flags, mismatches, length);
    }
}
