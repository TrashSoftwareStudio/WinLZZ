package BWZ;

import BWZ.Util.BWZUtil;
import BWZ.Util.LinkedDictionary;
import Utility.Util;

import java.util.ArrayList;

class MTFTransform {

    private short[] origText;

    MTFTransform(short[] text) {
        this.origText = text;
    }

    short[] Transform() {
        LinkedDictionary ld = new LinkedDictionary();
        ld.initialize(257);
        ArrayList<Short> temp = new ArrayList<>();
        int i = 0;
        int count = 0;
        while (i < origText.length) {
            short s = (short) ld.findAndMove(origText[i]);
            if (s == 0) {
                count += 1;  // If the MTF result is 0, add one to the run-length.
            } else {
                if (count != 0) {
                    short[] runLength = BWZUtil.runLength(count);
                    for (short rl : runLength) temp.add(rl);  // Record the run-length of 0's and reset the counter.
                    count = 0;
                }
                temp.add((short) (s + 1));
            }
            i += 1;
        }
        if (count != 0) {  // Add last few 0's
            short[] runLength = BWZUtil.runLength(count);
            for (short rl : runLength) temp.add(rl);
        }
        return Util.collectionToShortArray(temp);
    }
}


class MTFTransformByte {
    private byte[] origText;

    MTFTransformByte(byte[] text) {
        this.origText = text;
    }

    byte[] Transform() {
        LinkedDictionary ld = new LinkedDictionary();
        ld.initialize(17);
        ArrayList<Byte> temp = new ArrayList<>();
        int i = 0;
        int count = 0;
        while (i < origText.length) {
            byte s = (byte) ld.findAndMove(origText[i]);
            if (s == 0) {
                count += 1;  // If the MTF result is 0, add one to the run-length.
            } else {
                if (count != 0) {
                    byte[] runLength = BWZUtil.runLengthByte(count);
                    for (byte rl : runLength) temp.add(rl);  // Record the run-length of 0's and reset the counter.
                    count = 0;
                }
                temp.add((byte) (s + 1));
            }
            i += 1;
        }
        if (count != 0) {  // Add last few 0's
            byte[] runLength = BWZUtil.runLengthByte(count);
            for (byte rl : runLength) temp.add(rl);
        }
        return Util.collectionToArray(temp);
    }
}
