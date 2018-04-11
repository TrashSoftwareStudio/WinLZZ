package BWZ;

import Utility.Bytes;
import Utility.Sort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

public class BWTDecoder {

    private short[] cmpText;

    private int origIndex;

    BWTDecoder(short[] cmpText) {
        short[] indexBytesS = new short[3];
        System.arraycopy(cmpText, 0, indexBytesS, 0, 3);
        byte[] indexBytes = new byte[]{(byte) indexBytesS[0], (byte) indexBytesS[1], (byte) indexBytesS[2]};
        origIndex = Bytes.bytesToInt24(indexBytes);
        this.cmpText = new short[cmpText.length - 3];
        System.arraycopy(cmpText, 3, this.cmpText, 0, this.cmpText.length);
    }

    private void computeShift(LinkedList<Integer> list, int[] lShift, int index) {
        lShift[index] = list.removeFirst();
    }


    /**
     * Inverses the BWT.
     *
     * Method found in:
     * https://www.geeksforgeeks.org/inverting-burrows-wheeler-transform/
     *
     * @return The original text.
     */
    public byte[] Decode() {
        int len = cmpText.length;
        int[] lShift = new int[len];
        short[] sorted = new short[cmpText.length];

        System.arraycopy(cmpText, 0, sorted, 0, len);

        Arrays.sort(sorted);

        IntegerLinkedList[] lists = new IntegerLinkedList[257];
        for (int i = 0; i < 257; i++) {
            lists[i] = new IntegerLinkedList();
        }

        for (int i = 0; i < len; i++) {
            lists[cmpText[i]].addLast(i);
        }

        for (int i = 0; i < len; i++) {
            computeShift(lists[sorted[i]], lShift, i);
        }

        ArrayList<Short> temp = new ArrayList<>();
        int i = 0;
        while (i < len) {
            origIndex = lShift[origIndex];
            temp.add(cmpText[origIndex]);
            i++;
        }

        byte[] result = new byte[temp.size() - 1];
        for (int j = 0; j < result.length; j++) {
            result[j] = (byte) (temp.get(j) - 1);
        }

        return result;
    }
}

class IntegerLinkedList extends LinkedList<Integer> {

    IntegerLinkedList() {
        super();
    }
}
