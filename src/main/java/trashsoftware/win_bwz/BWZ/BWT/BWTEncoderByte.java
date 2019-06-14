package trashsoftware.win_bwz.BWZ.BWT;

import trashsoftware.win_bwz.BWZ.SuffixArrayDoublingByte;

/**
 * A transformer of text, used to transform texts using Burrows-Wheeler Transform.
 * <p>
 * This transformer stores text as byte array.
 * This transformer is implemented using doubling algorithm.
 *
 * @author zbh
 * @since 0.6
 */
public class BWTEncoderByte {

    /**
     * Original text for encoding.
     */
    private byte[] text;

    /**
     * The index of the row0 in the text after transformation, where row0 is the first row of the original text.
     */
    private int origRowIndex;

    /**
     * Creates a new {@code BWTEncoderByte} instance.
     *
     * @param text The text being transformed.
     */
    public BWTEncoderByte(byte[] text) {
        this.text = new byte[text.length + 1];
        for (int i = 0; i < text.length; i++) this.text[i] = (byte) ((text[i] & 0xff) + 1);  // Transform every byte
        // to unsigned and plus one to make sure nothing is smaller than or equal to the EOF character.
        this.text[this.text.length - 1] = 0;  // Add the EOF character (0) at the end of the original text.
        // This is necessary for transforming suffix array into Burrows-Wheeler matrix.
    }

    /**
     * Returns the text after transformation.
     *
     * @return the text after transformation.
     */
    public byte[] Transform() {
        return transform();
    }

    /**
     * Return the {@code origRowIndex}.
     *
     * @return the {@code origRowIndex}.
     */
    public int getOrigRowIndex() {
        return origRowIndex;
    }

    private byte[] transform() {
        int[] suffixArray;
        SuffixArrayDoublingByte sa = new SuffixArrayDoublingByte(text);
        sa.build(17);
        suffixArray = sa.getSa();

        int len = suffixArray.length;
        assert len == text.length;
        byte[] result = new byte[len];
        for (int i = 0; i < len; i++) {
            int pos = (suffixArray[i] + len - 1) % len;
            result[i] = text[pos];
            if (suffixArray[i] == 0) origRowIndex = i;
        }
        return result;
    }
}
