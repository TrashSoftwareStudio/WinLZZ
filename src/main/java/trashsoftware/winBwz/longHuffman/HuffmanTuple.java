package trashsoftware.winBwz.longHuffman;

/**
 * A tuple of a huffman code, implements the interface {@code Comparable}.
 *
 * @author zbh
 * @since 0.5
 */
public class HuffmanTuple implements Comparable<HuffmanTuple> {

    private int value;

    private int codeLength;

    /**
     * Creates a new {@code HuffmanTuple} instance.
     *
     * @param value      the value of this {@code HuffmanTuple}
     * @param codeLength the length of huffman code of this {@code HuffmanTuple}
     */
    public HuffmanTuple(int value, int codeLength) {
        this.value = value;
        this.codeLength = codeLength;
    }

    /**
     * Returns the code length.
     *
     * @return the code length
     */
    public int getLength() {
        return codeLength;
    }

    /**
     * Returns the value.
     *
     * @return the value
     */
    public int getValue() {
        return value;
    }

    /**
     * Compares this {@code HuffmanTuple} to another {@code HuffmanTuple} by, primarily the code length,
     * and secondarily the lexicographical order of the <code>value</code>
     *
     * @param o the {@code HuffmanTuple} to be compared with this {@code HuffmanTuple}
     * @return {@code 1} if this {@code HuffmanTuple} is greater than <code>o</code>, {@code -1} if smaller,
     * {@code 0} if equals
     */
    @Override
    public int compareTo(HuffmanTuple o) {
        int lengthCmp = Integer.compare(codeLength, o.codeLength);
        if (lengthCmp == 0) return Integer.compare(value, o.value);
        else return lengthCmp;
    }
}
