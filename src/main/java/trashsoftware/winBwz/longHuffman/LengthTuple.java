package trashsoftware.winBwz.longHuffman;

/**
 * A tuple of a code length in the huffman code, implements the interface {@code Comparable}.
 * <p>
 * This class is used for controlling the code length of a huffman table.
 *
 * @author zbh
 * @see Comparable
 * @since 0.5.2
 */
public class LengthTuple implements Comparable<LengthTuple> {

    private int b;

    int length;

    private int freq;

    /**
     * Creates a new instance of {@code LengthTuple}.
     *
     * @param b      the value
     * @param length the code length
     * @param freq   the occurrences of <code>b</code> in the original text
     */
    public LengthTuple(int b, int length, int freq) {
        this.b = b;
        this.length = length;
        this.freq = freq;
    }

    /**
     * Returns the value.
     *
     * @return the value
     */
    public int getByte() {
        return b;
    }

    /**
     * Compares this {@code LengthTuple} with another {@code LengthTuple}.
     *
     * @param o the {@code LengthTuple} to be compared with this {@code LengthTuple}
     * @return {@code 1} if this {@code LengthTuple} is greater than <code>o</code>, {@code -1} if smaller,
     * {@code 0} if equals
     */
    @Override
    public int compareTo(LengthTuple o) {
        return Integer.compare(o.freq, freq);
    }
}
