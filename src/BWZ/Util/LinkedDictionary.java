package BWZ.Util;

public class LinkedDictionary {

    private Node head;

    public void initialize(int size) {
        head = new Node((short) (size - 1));
        for (int i = size - 2; i >= 0; i--) {
            Node node = new Node((short) i);
            node.setNext(head);
            head = node;
        }
    }

    public int findAndMove(short value) {
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

    private short value;

    private Node next;

    Node(short value) {
        this.value = value;
    }

    void setNext(Node next) {
        this.next = next;
    }

    public short getValue() {
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
