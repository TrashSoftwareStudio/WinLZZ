package trashsoftware.win_bwz.longHuffman;

/**
 * A node of a huffman tree, where the node implements the interface {@code Comparable}.
 * <p>
 * Each {@code HuffmanNode} instance records a symbol or an internal node and its frequency.
 *
 * @author zbh
 * @see Comparable
 * @since 0.5
 */
public class HuffmanNode implements Comparable<HuffmanNode> {

    private int freq;

    private int value;

    private HuffmanNode left;

    private HuffmanNode right;

    /**
     * Creates a new {@code HuffmanNode} instance.
     *
     * @param freq the occurrence times of the representation of this node
     */
    public HuffmanNode(int freq) {
        this.freq = freq;
    }

    /**
     * Sets uo the value represented by this {@code HuffmanNode}.
     *
     * @param value the value
     */
    public void setValue(int value) {
        this.value = value;
    }

    /**
     * Sets up the left child of this {@code HuffmanNode}.
     *
     * @param left the left child of this {@code HuffmanNode}
     */
    public void setLeft(HuffmanNode left) {
        this.left = left;
    }

    /**
     * Sets up the right child of this {@code HuffmanNode}.
     *
     * @param right the right child of this {@code HuffmanNode}
     */
    public void setRight(HuffmanNode right) {
        this.right = right;
    }

    /**
     * Returns the value represented by this {@code HuffmanNode}.
     *
     * @return the value represented by this {@code HuffmanNode}
     */
    public int getValue() {
        return value;
    }

    public HuffmanNode getLeft() {
        return left;
    }

    public HuffmanNode getRight() {
        return right;
    }

    public int getFreq() {
        return freq;
    }

    /**
     * Returns {@code true} if and only if this {@code HuffmanNode} does not have neither left child nor right child.
     *
     * @return {@code true} if and only if this {@code HuffmanNode} does not have any children
     */
    public boolean isLeaf() {
        return left == null && right == null;
    }

    /**
     * Compares this {@code HuffmanNode} with another {@code HuffmanNode}.
     * <p>
     * This method returns 1 if this {@code HuffmanNode} has smaller {@code freq} than <code>o</code>'s,
     * -1 if greater, 0 if equals.
     *
     * @param o the {@code HuffmanNode} to be compared with this {@code HuffmanNode}
     * @return {@code 1} if this {@code HuffmanNode} has smaller {@code freq} than <code>o</code>'s,
     * {@code -1} if greater, {@code 0} if equals.
     */
    @Override
    public int compareTo(HuffmanNode o) {
        return Integer.compare(o.freq, freq);
    }
}
