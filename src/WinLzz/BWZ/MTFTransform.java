package WinLzz.BWZ;

import WinLzz.BWZ.Util.BWZUtil;
import WinLzz.BWZ.Util.LinkedDictionary;

/**
 * A Move-To-Front transformer, a Zero-Run-Length-Coder is integrated.
 * <p>
 * This program takes a short array as text.
 *
 * @author zbh
 * @since 0.5
 */
class MTFTransform {

    private short[] origText;

    /**
     * Creates a new {@code MTFTransform} instance.
     *
     * @param text the text to be transformed.
     */
    MTFTransform(short[] text) {
        this.origText = text;
    }

    /**
     * Returns the text after mtf transformation and zero run length coding.
     *
     * @return the text after mtf transformation and zero run length coding..
     */
    short[] Transform() {
        LinkedDictionary ld = new LinkedDictionary();
        ld.initialize(257);
        short[] result = new short[origText.length];
        int index = 0;
        int i = 0;
        int count = 0;
        while (i < origText.length) {
            short s = (short) ld.findAndMove(origText[i]);
            if (s == 0) {
                count += 1;  // If the MTF result is 0, add one to the run-length.
            } else {
                if (count != 0) {
                    short[] runLength = BWZUtil.runLength(count);
                    for (short rl : runLength) result[index++] = rl;
                    // Record the run-length of 0's and reset the counter.
                    count = 0;
                }
                result[index++] = (short) (s + 1);
            }
            i += 1;
        }
        if (count != 0) {  // Add last few 0's
            short[] runLength = BWZUtil.runLength(count);
            for (short rl : runLength) result[index++] = rl;
        }
        short[] rtn = new short[index];
        System.arraycopy(result, 0, rtn, 0, index);
        return rtn;
    }
}


