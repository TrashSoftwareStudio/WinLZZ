package trashsoftware.winBwz.core.bwz.lzp;

import java.io.ByteArrayOutputStream;
import java.util.BitSet;

public class LZPEncoded {
    public final BitSet flags;   // true = predicted OK, false = mismatch
    public final ByteArrayOutputStream mismatches;
    public final int length;     // original length (for decoder)

    public LZPEncoded(BitSet flags, ByteArrayOutputStream mismatches, int length) {
        this.flags = flags;
        this.mismatches = mismatches;
        this.length = length;
    }
}
