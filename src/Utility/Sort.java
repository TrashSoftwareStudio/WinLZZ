package Utility;

import java.util.Arrays;
import java.util.LinkedList;

public abstract class Sort {

    public static void main(String[] args) {
        short[] s = new short[]{3, 7, 4, 8, 1, 21, 4, 3, 2, 3, 8, 15};
        bucketSort(s, 35);
        System.out.println(Arrays.toString(s));
    }

    public static void bucketSort(short[] text, int bucketNumber) {
        ShortLinkedList[] lists = new ShortLinkedList[bucketNumber];
        for (short s : text) {
            if (lists[s] == null) {
                lists[s] = new ShortLinkedList();
            }
            lists[s].addLast(s);
        }

        int i = 0;
        for (ShortLinkedList sll : lists) {
            while (sll != null && !sll.isEmpty()) {
                text[i] = sll.removeFirst();
                i += 1;
            }
        }
    }
}


class ShortLinkedList extends LinkedList<Short> {

    ShortLinkedList() {
        super();
    }
}
