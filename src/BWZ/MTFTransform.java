package BWZ;

import BWZ.Util.BWZUtil;
import BWZ.Util.LinkedDictionary;
import Utility.Bytes;
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
                count += 1;
            } else {
                if (count != 0) {
                    short[] runLength = BWZUtil.runLength(count);
                    for (short rl : runLength) {
                        temp.add(rl);
                    }
                    count = 0;
                }
                temp.add((short) (s + 1));
            }
            i += 1;
        }
        if (count != 0) {  // Add last few 0's
            short[] runLength = BWZUtil.runLength(count);
            for (short rl : runLength) {
                temp.add(rl);
            }
        }
//        temp.add((short) 258);  // End signal for huffman compressor.

        return Util.collectionToShortArray(temp);
    }
}
