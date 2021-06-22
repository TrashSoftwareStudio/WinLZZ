package trashsoftware.winBwz.core.bwz;

import trashsoftware.winBwz.core.bwz.util.BWZUtil;

/**
 * A Move-To-Front transformer, a Zero-Run-Length-Coder is integrated.
 * <p>
 * This program takes a short array as text.
 *
 * @author zbh
 * @since 0.5
 */
public class MTFTransform {

    private final int[] origText;

    /**
     * Creates a new {@code MTFTransform} instance.
     *
     * @param text the text to be transformed.
     */
    public MTFTransform(int[] text) {
        this.origText = text;
    }

    /**
     * Returns the text after mtf transformation and zero run length coding.
     *
     * @param alphabetSize the alphabet size
     * @return the text after mtf transformation and zero run length coding..
     */
    public int[] Transform(int alphabetSize) {
        ArrayDictionary ld = new ArrayDictionary(alphabetSize);
        int[] result = new int[origText.length];
        int index = 0;
        int i = 0;
        int count = 0;
        while (i < origText.length) {
            int s = ld.findAndMove(origText[i]);
            if (s == 0) {
                count += 1;  // If the MTF result is 0, add one to the run-length.
            } else {
                if (count != 0) {
                    index += BWZUtil.runLength(count, result, index);
                    // Record the run-length of 0's and reset the counter.
                    count = 0;
                }
                result[index++] = s + 1;
            }
            i += 1;
        }
        if (count != 0) {  // Add last few 0's
            index += BWZUtil.runLength(count, result, index);
        }
        int[] rtn = new int[index];
        System.arraycopy(result, 0, rtn, 0, index);
        return rtn;
    }
}

class ArrayDictionary {

    private final int[] array;

    ArrayDictionary(int alphabetSize) {
        array = new int[alphabetSize];
        for (int i = 0; i < alphabetSize; ++i) {
            array[i] = i;
        }
    }

    int findAndMove(int value) {
        if (array[0] == value) return 0;
        int loopSize = array.length;
        int last = array[0];
        for (int i = 1; i < loopSize; ++i) {
            int v = array[i];
            array[i] = last;
            last = v;
            if (v == value) {
                array[0] = v;
                return i;
            }
        }
        throw new RuntimeException("Cannot find symbol");
    }
}
