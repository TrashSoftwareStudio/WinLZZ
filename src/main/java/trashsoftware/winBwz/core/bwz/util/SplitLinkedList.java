package trashsoftware.winBwz.core.bwz.util;

import java.util.LinkedList;

/**
 * A list implementation that takes O(sqrt(n)) to access an element randomly and O(sqrt(n)) to move an element.
 *
 * @author zbh
 * @since 0.6.2
 */
public class SplitLinkedList {

    private final ShortLinkedList[] lists = new ShortLinkedList[16];

    private short front;

    /**
     * Creates a new {@code SplitLinkedList} instance.
     */
    public SplitLinkedList() {
        for (int i = 0; i < 16; i++) {
            lists[i] = new ShortLinkedList();
            for (int j = 0; j < 16; j++) lists[i].addLast((short) (i * 16 + j + 1));
        }
    }

    /**
     * Returns the element at index <code>index</code> and move this element to the front of
     * this {@code SplitLinkedList}.
     *
     * @param index the random access index.
     * @return the element at <code>index</code>.
     */
    public short getAndMove(int index) {
        if (index == 0) {
            return front;
        } else {
            int trueIndex = index - 1;
            int range = trueIndex >> 4;
            int split = trueIndex & 0x0f;
            short item = lists[range].remove(split);
            for (int i = range; i > 0; i--) lists[i].addFirst(lists[i - 1].removeLast());
            lists[0].addFirst(front);
            front = item;
            return item;
        }
    }
}


/**
 * An {@code LinkedList} that holds {@code Short} as elements.
 */
class ShortLinkedList extends LinkedList<Short> {

    /**
     * Creates a new {@code IntegerLinkedList} instance.
     */
    ShortLinkedList() {
        super();
    }
}
