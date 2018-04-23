package BWZ;

import java.util.Arrays;

class BWTDecoderByte {

    private byte[] cmpText;

    private int origIndex;

    BWTDecoderByte(byte[] cmpText, int origRowIndex) {
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
    byte[] Decode() {
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
