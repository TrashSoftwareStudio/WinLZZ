package trashsoftware.win_bwz.bwz;

import trashsoftware.win_bwz.bwz.util.BWZUtil;
import trashsoftware.win_bwz.utility.Util;

import java.util.ArrayList;

/**
 * Decoder of Zero-Run-Length-Coding.
 * <p>
 * This program takes a byte array as input text, and output a byte array as result.
 *
 * @author zbh
 * @since 0.6
 */
public class ZeroRLCDecoderByte {

    private byte[] text;

    /**
     * Creates a new {@code ZeroRLCDecoderByte} instance.
     *
     * @param text the text after zero rlc coding.
     */
    public ZeroRLCDecoderByte(byte[] text) {
        this.text = text;
    }

    /**
     * Returns the decoded text.
     *
     * @return the decoded text.
     */
    public byte[] Decode() {
        ArrayList<Byte> temp = new ArrayList<>();
        ArrayList<Byte> runLengths = new ArrayList<>();
        int i = 0;
        while (i < text.length) {
            byte b = text[i];
            if (b == 0 || b == 1) {
                runLengths.add(b);
            } else {
                if (!runLengths.isEmpty()) {
                    int length = BWZUtil.runLengthInverse(Util.collectionToArray(runLengths));
                    runLengths.clear();
                    for (int j = 0; j < length; j++) {
                        temp.add((byte) 0);
                    }
                }
                temp.add((byte) (b - 1));
            }
            i += 1;

        }
        if (!runLengths.isEmpty()) {  // Last few 0's
            int length = BWZUtil.runLengthInverse(Util.collectionToArray(runLengths));
            for (int j = 0; j < length; j++) {
                temp.add((byte) 0);
            }
        }
        return Util.collectionToArray(temp);
    }
}
