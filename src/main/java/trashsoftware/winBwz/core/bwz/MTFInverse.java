package trashsoftware.winBwz.core.bwz;

import trashsoftware.winBwz.core.bwz.util.SplitLinkedList;

/**
 * The inverse transformer of Move-To-Front transformation.
 * <p>
 * This program takes a short array as input text.
 *
 * @author zbh
 * @since 0.5
 */
public class MTFInverse {

    private int[] text;

    /**
     * Creates a new instance of {@code MTFInverse}.
     *
     * @param text the text to be inverse transformed.
     */
    public MTFInverse(int[] text) {
        this.text = text;
    }

    /**
     * Returns the text after the inverse transform.
     * <p>
     * This method uses an array as the dictionary, which takes O(1) to access and O(n) to move.
     *
     * @param alphabetSize the alphabet size
     * @return the text after the inverse transform.
     */
    public int[] decode(int alphabetSize) {
        int[] dictionary = new int[alphabetSize];
        for (int i = 0; i < alphabetSize; i++) dictionary[i] = i;
        int[] result = new int[text.length];
        int index = 0;
        for (int b : text) {
            int s = dictionary[b];
            if (b > 0) {
                System.arraycopy(dictionary, 0, dictionary, 1, b);
                dictionary[0] = s;
            }
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
    @Deprecated
    int[] decode2() {
        SplitLinkedList sll = new SplitLinkedList();
        int[] result = new int[text.length];
        int index = 0;
        for (int i : text) result[index++] = sll.getAndMove(i);
        return result;
    }

}
