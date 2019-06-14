package trashsoftware.win_bwz.BWZ.BWT;

import java.util.Arrays;

/**
 * A inverse transformer of Burrows-Wheeler Transform.
 * <p>
 * This program takes a byte array as input text.
 *
 * @author zbh
 * @since 0.6
 */
public class BWTDecoderByte {

    private byte[] cmpText;
    private int origIndex;

    /**
     * Creates a new {@code BWTDecoderByte} instance.
     *
     * @param cmpText      the text after bwt transformation.
     * @param origRowIndex the index of the row0 in the text after transformation,
     *                     where row0 is the first row of the original text.
     */
    public BWTDecoderByte(byte[] cmpText, int origRowIndex) {
        origIndex = origRowIndex;
        this.cmpText = cmpText;
    }

    /**
     * Inverses the BWT.
     * <p>
     * Algorithm found in:
     * https://www.geeksforgeeks.org/inverting-burrows-wheeler-transform/
     *
     * @return The original text.
     */
    public byte[] Decode() {
        int len = cmpText.length;
        int[] lShift = new int[len];
        byte[] sorted = new byte[cmpText.length];
        System.arraycopy(cmpText, 0, sorted, 0, len);
        Arrays.sort(sorted);

        IntegerLinkedList[] lists = new IntegerLinkedList[257];
        for (int i = 0; i < 257; i++) lists[i] = new IntegerLinkedList();
        for (int i = 0; i < len; i++) lists[cmpText[i]].addLast(i);
        for (int i = 0; i < len; i++) lShift[i] = lists[sorted[i]].removeFirst();

        byte[] result = new byte[cmpText.length - 1];
        for (int i = 0; i < result.length; i++) {
            origIndex = lShift[origIndex];
            result[i] = (byte) (cmpText[origIndex] - 1);
        }
        return result;
    }
}
