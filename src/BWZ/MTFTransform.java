package BWZ;

import BWZ.Util.BWZUtil;
import BWZ.Util.LinkedDictionary;

import java.util.Arrays;

class MTFTransform {

    private short[] origText;

    public static void main(String[] args) {
        short[] t = new short[]{1, 4, 4, 4, 4, 4, 4, 4, 4, 1, 2, 3, 1, 2, 3, 1, 2, 3, 4, 4, 7};
        MTFTransform mtf = new MTFTransform(t);
        System.out.println(Arrays.toString(mtf.Transform()));
    }

    MTFTransform(short[] text) {
        this.origText = text;
    }

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


