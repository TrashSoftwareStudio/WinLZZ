package trashsoftware.win_bwz.huffman;

public class HuffmanNode implements Comparable<HuffmanNode> {

    private int freq;

    private byte value;

    private HuffmanNode left;

    private HuffmanNode right;

    HuffmanNode(int freq) {
        this.freq = freq;
    }

    public void setValue(byte value) {
        this.value = value;
    }

    void setLeft(HuffmanNode left) {
        this.left = left;
    }

    void setRight(HuffmanNode right) {
        this.right = right;
    }

    public byte getValue() {
        return value;
    }

    HuffmanNode getLeft() {
        return left;
    }

    HuffmanNode getRight() {
        return right;
    }

    int getFreq() {
        return freq;
    }

    boolean isLeaf() {
        return left == null && right == null;
    }

    @Override
    public int compareTo(HuffmanNode o) {
        return -Integer.compare(freq, o.freq);
    }
}
