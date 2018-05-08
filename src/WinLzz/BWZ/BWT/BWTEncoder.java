package WinLzz.BWZ.BWT;

import WinLzz.BWZ.SuffixArrayDC3;
import WinLzz.BWZ.SuffixArrayDoubling;
import WinLzz.Utility.Bytes;

public class BWTEncoder {

    /* Original text for encoding. */
    private short[] text;
    private int origRowIndex;
    private boolean isDc3;

    /**
     * Creates a new instance of BWTEncoder.
     *
     * @param text  The text being transformed.
     * @param isDc3 Whether to use dc3 or doubling algorithm.
     */
    public BWTEncoder(byte[] text, boolean isDc3) {
        this.isDc3 = isDc3;
        this.text = new short[text.length + 1];
        for (int i = 0; i < text.length; i++) this.text[i] = (short) ((text[i] & 0xff) + 1);  // Transform every byte
        // to unsigned and plus one to make sure nothing is smaller than or equal to the EOF character.
        this.text[this.text.length - 1] = 0;  // Add the EOF character (0) at the end of the original text.
        // This is necessary for transforming suffix array into Burrows-Wheeler matrix.
    }

    public short[] Transform() {
        short[] result = new short[text.length + 3];
        short[] trans = transform();
        byte[] indexRep = Bytes.intToBytes24(origRowIndex);
        for (int i = 0; i < 3; i++) result[i] = (short) (indexRep[i] & 0xff);
        System.arraycopy(trans, 0, result, 3, trans.length);
        return result;
    }

    private short[] transform() {
        int[] suffixArray;
        if (isDc3) {
            SuffixArrayDC3 sa = new SuffixArrayDC3(text, 257);
            suffixArray = sa.getSa();
        } else {
            SuffixArrayDoubling sa = new SuffixArrayDoubling(text);
            sa.build(257);
            suffixArray = sa.getSa();
        }

        int len = suffixArray.length;
        assert len == text.length;
        short[] result = new short[len];
        for (int i = 0; i < len; i++) {
            int pos = (suffixArray[i] + len - 1) % len;
            result[i] = text[pos];
            if (suffixArray[i] == 0) origRowIndex = i;
        }
        return result;
    }
}
