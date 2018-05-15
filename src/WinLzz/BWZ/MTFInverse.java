package WinLzz.BWZ;

import WinLzz.BWZ.Util.SplitLinkedList;

/**
 * The inverse transformer of Move-To-Front transformation.
 * <p>
 * This program takes a short array as input text.
 *
 * @author zbh
 * @since 0.5
 */
class MTFInverse {

    private short[] text;

    /**
     * Creates a new instance of {@code MTFInverse}.
     *
     * @param text the text to be inverse transformed.
     */
    MTFInverse(short[] text) {
        this.text = text;
    }

    /**
     * Returns the text after the inverse transform.
     * <p>
     * This method uses an array as the dictionary, which takes O(1) to access and O(n) to move.
     *
     * @return the text after the inverse transform.
     */
    @Deprecated
    short[] decode2() {
        short[] dictionary = new short[257];
        for (short i = 0; i < 257; i++) dictionary[i] = i;
        short[] result = new short[text.length];
        int index = 0;
        for (short b : text) {
            short s = dictionary[b];
            System.arraycopy(dictionary, 0, dictionary, 1, b);
            dictionary[0] = s;
            result[index++] = s;
        }
        return result;
    }

    /**
     * Returns the text after the inverse transform.
     * <p>
     * This method uses {@code SplitLinkedList} as the dictionary, which takes O(sqrt(n)) to access and
     * O(sqrt(n)) to move.
     *
     * @return the text after the inverse transform.
     */
    short[] decode() {
        SplitLinkedList sll = new SplitLinkedList();
        short[] result = new short[text.length];
        int index = 0;
        for (short i : text) result[index++] = sll.getAndMove(i);
        return result;
    }
}

