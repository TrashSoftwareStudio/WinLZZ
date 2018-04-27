package WinLzz.BWZ;

import WinLzz.BWZ.Util.BWZUtil;
import WinLzz.BWZ.Util.LinkedDictionary;

public class MTFTransformByte {
    private byte[] origText;

    public MTFTransformByte(byte[] text) {
        this.origText = text;
    }

    public byte[] Transform() {
        LinkedDictionary ld = new LinkedDictionary();
        ld.initialize(18);
        byte[] result = new byte[origText.length];
        int index = 0;
        int i = 0;
        int count = 0;
        while (i < origText.length) {
            byte s = (byte) ld.findAndMove(origText[i]);
            if (s == 0) {
                count += 1;  // If the MTF result is 0, add one to the run-length.
            } else {
                if (count != 0) {
                    byte[] runLength = BWZUtil.runLengthByte(count);
                    for (byte rl : runLength)
                        result[index++] = rl;  // Record the run-length of 0's and reset the counter.
                    count = 0;
                }
                result[index++] = (byte) (s + 1);
            }
            i += 1;
        }
        if (count != 0) {  // Add last few 0's
            byte[] runLength = BWZUtil.runLengthByte(count);
            for (byte rl : runLength) result[index++] = rl;
        }
        byte[] rtn = new byte[index];
        System.arraycopy(result, 0, rtn, 0, index);
        return rtn;
    }
}
