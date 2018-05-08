package WinLzz.BWZ.BWT;

import WinLzz.Utility.Bytes;
import WinLzz.Utility.Util;

import java.util.ArrayDeque;

public class BWTDecoder {

    private short[] cmpText;
    private int origIndex;

    public BWTDecoder(short[] cmpText) {
        short[] indexBytesS = new short[3];
        System.arraycopy(cmpText, 0, indexBytesS, 0, 3);
        byte[] indexBytes = new byte[]{(byte) indexBytesS[0], (byte) indexBytesS[1], (byte) indexBytesS[2]};
        origIndex = Bytes.bytesToInt24(indexBytes);
        this.cmpText = new short[cmpText.length - 3];
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
        short[] sorted = new short[len];
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

class IntegerLinkedList extends ArrayDeque<Integer> {
    IntegerLinkedList() {
        super();
    }
}
