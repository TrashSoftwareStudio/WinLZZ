package WinLzz.BWZ;

import WinLzz.BWZ.Util.BWZUtil;
import WinLzz.Utility.Util;

import java.util.ArrayList;

/**
 * Decoder of Zero-Run-Length-Coding.
 * <p>
 * This program takes a short array as input text, and output a short array as result.
 *
 * @author zbh
 * @since 0.5
 */
class ZeroRLCDecoder {

    private int[] text;
    private int maxBlockLength;

    /**
     * Creates a new {@code ZeroRLCDecoder} instance.
     *
     * @param text           the text after zero rlc coding.
     * @param maxBlockLength the maximum possible length after decode.
     */
    ZeroRLCDecoder(int[] text, int maxBlockLength) {
        this.text = text;
        this.maxBlockLength = maxBlockLength;
    }

    /**
     * Returns the decoded text.
     *
     * @return the decoded text.
     */
    int[] Decode() {
        int[] temp = new int[maxBlockLength];
        int index = 0;
        ArrayList<Integer> runLengths = new ArrayList<>();
        int i = 0;
        while (i < text.length) {
            int b = text[i];
            if (b < 2) {
                runLengths.add(b);
            } else {
                if (!runLengths.isEmpty()) {
                    int length = BWZUtil.runLengthInverse(runLengths);
                    runLengths.clear();
                    for (int j = 0; j < length; j++) temp[index++] = 0;
                }
                temp[index++] = b - 1;
            }
            i += 1;
        }
        if (!runLengths.isEmpty()) {  // Last few 0's
            int length = BWZUtil.runLengthInverse(Util.collectionToIntArray(runLengths));
            for (int j = 0; j < length; j++) {
                temp[index++] = 0;
            }
        }
        if (index == maxBlockLength) return temp;
        else {
            int[] rtn = new int[index];
            System.arraycopy(temp, 0, rtn, 0, index);
            return rtn;
        }
    }
}
