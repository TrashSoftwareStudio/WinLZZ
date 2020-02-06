package trashsoftware.win_bwz.core.bwz;

import trashsoftware.win_bwz.core.bwz.util.BWZUtil;
import trashsoftware.win_bwz.core.bwz.util.LinkedDictionary;

/**
 * A Move-To-Front transformer, a Zero-Run-Length-Coder is integrated.
 * <p>
 * This program takes a byte array as text.
 *
 * @author zbh
 * @since 0.6
 */
public class MTFTransformByte {

    private byte[] origText;

    /**
     * Creates a new {@code MTFTransformByte} instance.
     *
     * @param text the text to be transformed.
     */
    public MTFTransformByte(byte[] text) {
        this.origText = text;
    }

    /**
     * Returns the text after mtf transformation and zero run length coding.
     *
     * @param alphabetSize the size of alphabet in <code>text</code>
     * @return the text after mtf transformation and zero run length coding..
     */
    public byte[] Transform(int alphabetSize) {
        LinkedDictionary ld = new LinkedDictionary();
        ld.initialize(alphabetSize);
        byte[] result = new byte[origText.length];
        int index = 0;
        int i = 0;
        int count = 0;
        while (i < origText.length) {
            byte s = (byte) ld.findAndMove((short) (origText[i++] & 0xff));
            if (s == 0) {
                count += 1;  // If the MTF result is 0, add one to the run-length.
            } else {
                if (count != 0) {
                    index += BWZUtil.runLengthByte(count, result, index);
//                        result[index++] = rl;  // Record the run-length of 0's and reset the counter.
                    count = 0;
                }
                result[index++] = (byte) (s + 1);
            }
        }
        if (count != 0) {  // Add last few 0's
            index += BWZUtil.runLengthByte(count, result, index);
        }
        byte[] rtn = new byte[index];
        System.arraycopy(result, 0, rtn, 0, index);
        return rtn;
    }
}
