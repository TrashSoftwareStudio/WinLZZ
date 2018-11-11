package WinLzz.BWZ.BWT;

import WinLzz.BWZ.SuffixArrayDC3;
import WinLzz.BWZ.SuffixArrayDoubling;
import WinLzz.Utility.Bytes;

/**
 * A transformer of text, used to transform texts using Burrows-Wheeler Transform.
 * <p>
 * This transformer stores text as short array.
 * This transformer is implemented using two optional algorithms, which are doubling algorithm and dc3 algorithm.
 *
 * @author zbh
 * @since 0.5
 */
public class BWTEncoder {

    /**
     * Original text for encoding.
     */
    private int[] text;

    /**
     * The index of the row0 in the text after transformation, where row0 is the first row of the original text.
     */
    private int origRowIndex;

    /**
     * Whether to use dc3 or doubling algorithm.
     * <p>
     * {@code true} for dc3 algorithm, {@code false} for doubling algorithm.
     */
    private boolean isDc3;

    /**
     * Creates a new {@code BWTEncoder} instance.
     *
     * @param text  The text being transformed.
     * @param isDc3 Whether to use dc3 or doubling algorithm.
     */
    public BWTEncoder(byte[] text, boolean isDc3) {
        this.isDc3 = isDc3;
        this.text = new int[text.length + 1];
        for (int i = 0; i < text.length; i++) this.text[i] = (text[i] & 0xff) + 1;  // Transform every byte
        // to unsigned and plus one to make sure nothing is smaller than or equal to the EOF character.
        this.text[this.text.length - 1] = 0;  // Add the EOF character (0) at the end of the original text.
        // This is necessary for transforming suffix array into Burrows-Wheeler matrix.
    }

    /**
     * Returns the text after transformation, including the record of {@code origRowIndex}.
     *
     * @return the text after transformation, including the record of {@code origRowIndex}.
     */
    public int[] Transform() {
        int[] result = new int[text.length + 3];
        int[] trans = transform();
        byte[] indexRep = Bytes.intToBytes24(origRowIndex);
        for (int i = 0; i < 3; i++) result[i] = indexRep[i] & 0xff;
        System.arraycopy(trans, 0, result, 3, trans.length);
        return result;
    }

    private int[] transform() {
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
        int[] result = new int[len];
        for (int i = 0; i < len; i++) {
            int pos = (suffixArray[i] + len - 1) % len;
            result[i] = text[pos];
            if (suffixArray[i] == 0) origRowIndex = i;
        }
        return result;
    }
}
