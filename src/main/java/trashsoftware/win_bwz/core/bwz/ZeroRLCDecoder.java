package trashsoftware.win_bwz.core.bwz;

import trashsoftware.win_bwz.core.bwz.util.BWZUtil;

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
        int[] buffer = new int[100];
        int bufferIndex = 0;
        int i = 0;
        while (i < text.length) {
            int b = text[i];
            if (b < 2) {
                buffer[bufferIndex++] = b;
            } else {
                if (bufferIndex > 0) {
                    int length = BWZUtil.runLengthInverse(buffer, bufferIndex);
                    bufferIndex = 0;
                    for (int j = 0; j < length; j++) temp[index++] = 0;
                }
                temp[index++] = b - 1;
            }
            i += 1;
        }
        if (bufferIndex > 0) {
            int length = BWZUtil.runLengthInverse(buffer, bufferIndex);
//            bufferIndex = 0;
            for (int j = 0; j < length; j++) temp[index++] = 0;
        }
        if (index == maxBlockLength) return temp;
        else {
            int[] rtn = new int[index];
            System.arraycopy(temp, 0, rtn, 0, index);
            return rtn;
        }
    }
}
