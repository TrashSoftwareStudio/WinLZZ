package trashsoftware.win_bwz.bwz;

import trashsoftware.win_bwz.bwz.util.BWZUtil;
import trashsoftware.win_bwz.bwz.util.LinkedDictionary;

import java.util.Arrays;

/**
 * A Move-To-Front transformer, a Zero-Run-Length-Coder is integrated.
 * <p>
 * This program takes a short array as text.
 *
 * @author zbh
 * @since 0.5
 */
class MTFTransform {

    private int[] origText;

    /**
     * Creates a new {@code MTFTransform} instance.
     *
     * @param text the text to be transformed.
     */
    MTFTransform(int[] text) {
        this.origText = text;
    }

    /**
     * Returns the text after mtf transformation and zero run length coding.
     *
     * @return the text after mtf transformation and zero run length coding..
     */
    int[] Transform() {
        ArrayDictionary ld = new ArrayDictionary(257);
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

    private int[] array;

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

    public static void main(String[] args) {
        int[] eee = {3, 3, 3, 4, 7, 2, 5, 1, 2, 0, 7, 3, 2, 2, 3, 1, 2};
        ArrayDictionary ad = new ArrayDictionary(8);
//        System.out.println(Arrays.toString(ad.array));
//        System.out.println(ad.findAndMove(3));
//        System.out.println(Arrays.toString(ad.array));
        int[] rrr = new int[eee.length];
        for (int i = 0; i < eee.length; ++i) {
            int f = ad.findAndMove(eee[i]);
            rrr[i] = f;
        }
        System.out.println(Arrays.toString(rrr));
    }
}
