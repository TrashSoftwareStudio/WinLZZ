package WinLzz.BWZ;

import WinLzz.BWZ.Util.BWZUtil;
import WinLzz.Utility.Util;

import java.util.ArrayList;

class ZeroRLCDecoder {

    private short[] text;

    private int maxBlockLength;

    ZeroRLCDecoder(short[] text, int maxBlockLength) {
        this.text = text;
        this.maxBlockLength = maxBlockLength;
    }

    short[] Decode() {
        short[] temp = new short[maxBlockLength];
        int index = 0;
        ArrayList<Short> runLengths = new ArrayList<>();
        int i = 0;
        while (i < text.length) {
            short b = text[i];
            if (b < (short) 2) {
                runLengths.add(b);
            } else {
                if (!runLengths.isEmpty()) {
                    int length = BWZUtil.runLengthInverse(runLengths);
                    runLengths.clear();
                    for (int j = 0; j < length; j++) temp[index++] = 0;
                }
                temp[index++] = (short) (b - 1);
            }
            i += 1;
        }
        if (!runLengths.isEmpty()) {  // Last few 0's
            int length = BWZUtil.runLengthInverse(Util.collectionToShortArray(runLengths));
            for (int j = 0; j < length; j++) {
                temp[index++] = 0;
            }
        }
        if (index == maxBlockLength) return temp;
        else {
            short[] rtn = new short[index];
            System.arraycopy(temp, 0, rtn, 0, index);
            return rtn;
        }
    }
}
