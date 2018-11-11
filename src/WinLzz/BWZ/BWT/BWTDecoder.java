package WinLzz.BWZ.BWT;

import WinLzz.Utility.Bytes;
import WinLzz.Utility.Util;

import java.util.ArrayDeque;

/**
 * A inverse transformer of Burrows-Wheeler Transform.
 * <p>
 * This program takes a short array as input text.
 *
 * @author zbh
 * @since 0.5
 */
public class BWTDecoder {

    private int[] cmpText;
    private int origIndex;

    /**
     * Creates a new {@code BWTDecoder} instance.
     *
     * @param cmpText the text after bwt transformation.
     */
    public BWTDecoder(int[] cmpText) {
        int[] indexBytesS = new int[3];
        System.arraycopy(cmpText, 0, indexBytesS, 0, 3);
        byte[] indexBytes = new byte[]{(byte) indexBytesS[0], (byte) indexBytesS[1], (byte) indexBytesS[2]};
        origIndex = Bytes.bytesToInt24(indexBytes);
        this.cmpText = new int[cmpText.length - 3];
        System.arraycopy(cmpText, 3, this.cmpText, 0, this.cmpText.length);
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
        int[] sorted = new int[len];
        System.arraycopy(cmpText, 0, sorted, 0, len);
        Util.countingSort(sorted, 257);

        IntegerLinkedList[] lists = new IntegerLinkedList[257];
        for (int i = 0; i < 257; i++) lists[i] = new IntegerLinkedList();
        for (int i = 0; i < len; i++) lists[cmpText[i]].addLast(i);
        for (int i = 0; i < len; i++) lShift[i] = lists[sorted[i]].removeFirst();

        byte[] result = new byte[len - 1];

        int currentRow = origIndex;
        for (int i = 0; i < len - 1; i++) {
            currentRow = lShift[currentRow];
            result[i] = (byte) (cmpText[currentRow] - 1);
        }
        return result;
    }
}


/**
 * An {@code ArrayDeque} that holds {@code Integer} as elements.
 */
class IntegerLinkedList extends ArrayDeque<Integer> {

    /**
     * Creates a new {@code IntegerLinkedList} instance.
     */
    IntegerLinkedList() {
        super();
    }
}
