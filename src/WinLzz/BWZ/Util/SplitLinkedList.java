package WinLzz.BWZ.Util;

import java.util.LinkedList;

public class SplitLinkedList {

    private ShortLinkedList[] lists = new ShortLinkedList[8];

    private short front;

    public SplitLinkedList() {
        for (int i = 0; i < 8; i++) {
            lists[i] = new ShortLinkedList();
            for (int j = 0; j < 32; j++) lists[i].addLast((short) (i * 32 + j + 1));
        }
    }

    public short getAndMove(int index) {
        if (index == 0) {
            return front;
        } else {
            int trueIndex = index - 1;
            int range = (trueIndex) / 32;
            int split = (trueIndex) % 32;
            short item = lists[range].remove(split);
            for (int i = range; i > 0; i--) lists[i].addFirst(lists[i - 1].removeLast());
            lists[0].addFirst(front);
            front = item;
            return item;
        }
    }
}


class ShortLinkedList extends LinkedList<Short> {

    ShortLinkedList() {
        super();
    }
}
