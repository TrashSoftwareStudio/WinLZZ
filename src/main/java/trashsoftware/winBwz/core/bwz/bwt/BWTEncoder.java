package trashsoftware.winBwz.core.bwz.bwt;

import trashsoftware.winBwz.core.bwz.SuffixArrayDC3;
import trashsoftware.winBwz.core.bwz.SuffixArrayDoubling;
import trashsoftware.winBwz.utility.Bytes;

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

    private int threadId;

    /**
     * Creates a new {@code BWTEncoder} instance.
     *
     * @param fullText The total text, part being transformed.
     * @param isDc3    Whether to use dc3 or doubling algorithm.
     */
    public BWTEncoder(byte[] fullText, int begin, int size, boolean isDc3, int threadId) {
        this.threadId = threadId;
        this.isDc3 = isDc3;
        this.text = new int[size + 1];
        for (int i = 0; i < size; i++) this.text[i] = (fullText[begin + i] & 0xff) + 1;  // Transform every byte
        // to unsigned and plus one to make sure nothing is smaller than or equal to the EOF character.
        this.text[this.text.length - 1] = 0;  // Add the EOF character (0) at the end of the original text.
        // This is necessary for transforming suffix array into Burrows-Wheeler matrix.
    }

    /**
     * Creates a new {@code BWTEncoder} instance.
     *
     * @param fullText The text
     */
    public BWTEncoder(byte[] fullText) {
        this.threadId = 0;
        this.isDc3 = false;
        int size = fullText.length;
        this.text = new int[size + 1];
        for (int i = 0; i < size; i++) this.text[i] = (fullText[i] & 0xff) + 1;  // Transform every byte
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
        return transform();
    }

    public int[] pureTransform(int alphabetSize) {
        int[] suffixArray;

        if (isDc3) {
            SuffixArrayDC3 sa = new SuffixArrayDC3(text, alphabetSize);
            suffixArray = sa.getSa();
        } else {
            SuffixArrayDoubling sa = new SuffixArrayDoubling(text, threadId);
            sa.build(alphabetSize);
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

//    static long saTime, transTime;

    private int[] transform() {
        int[] suffixArray;

        if (isDc3) {
            SuffixArrayDC3 sa = new SuffixArrayDC3(text, 257);
            suffixArray = sa.getSa();
        } else {
            SuffixArrayDoubling sa = new SuffixArrayDoubling(text, threadId);
            sa.build(257);
            suffixArray = sa.getSa();
        }

        int len = suffixArray.length;
        assert len == text.length;

        int[] result = new int[len + 3];
        for (int i = 0; i < len; i++) {
            int pos = (suffixArray[i] + len - 1) % len;
            result[i + 3] = text[pos];
            if (suffixArray[i] == 0) origRowIndex = i;
        }
        Bytes.intToByte24(origRowIndex, result, 0);
//        System.out.println("sa: " + saTime + " move: " + transTime);

        return result;
    }

    public int getOrigRowIndex() {
        return origRowIndex;
    }
}
