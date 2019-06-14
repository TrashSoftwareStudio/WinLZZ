package trashsoftware.win_bwz.BWZ;

import trashsoftware.win_bwz.BWZ.Util.BWZUtil;
import trashsoftware.win_bwz.BWZ.Util.LinkedDictionary;

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
        LinkedDictionary ld = new LinkedDictionary();
        ld.initialize(257);
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
                    int[] runLength = BWZUtil.runLength(count);
                    for (int rl : runLength) result[index++] = rl;
                    // Record the run-length of 0's and reset the counter.
                    count = 0;
                }
                result[index++] = s + 1;
            }
            i += 1;
        }
        if (count != 0) {  // Add last few 0's
            int[] runLength = BWZUtil.runLength(count);
            for (int rl : runLength) result[index++] = rl;
        }
        int[] rtn = new int[index];
        System.arraycopy(result, 0, rtn, 0, index);
        return rtn;
    }
}


