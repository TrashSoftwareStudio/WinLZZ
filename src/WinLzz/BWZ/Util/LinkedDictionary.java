package WinLzz.BWZ.Util;

public class LinkedDictionary {

    private Node head;

    /**
     * Initialize a LinkedDictionary with specified size.
     *
     * @param size the size of dictionary.
     */
    public void initialize(int size) {
        head = new Node(size - 1);
        for (int i = size - 2; i >= 0; i--) {
            Node node = new Node(i);
            node.setNext(head);
            head = node;
        }
    }

    /**
     * Returns the index of the target value inside dictionary and then move it to the front.
     *
     * @param value the value to be searched.
     * @return the index of this value in this dictionary.
     */
    public int findAndMove(int value) {
        Node prev = head;
        Node current = head;
        int count = 0;
        while (current.getValue() != value) {
            prev = current;
            current = current.getNext();
            count += 1;
        }
        if (count == 0) {
            return 0;
        } else {
            prev.setNext(current.getNext());
            current.setNext(head);
            head = current;
            return count;
        }
    }

    @Override
    public String toString() {
        Node node = head;
        StringBuilder builder = new StringBuilder();
        while (node != null) {
            builder.append(node.toString());
            node = node.getNext();
        }
        return builder.toString();
    }
}


class Node {

    private int value;
    private Node next;

    Node(int value) {
        this.value = value;
    }

    void setNext(Node next) {
        this.next = next;
    }

    public int getValue() {
        return value;
    }

    Node getNext() {
        return next;
    }

    @Override
    public String toString() {
        return (value & 0xffff) + "->";
    }
}
