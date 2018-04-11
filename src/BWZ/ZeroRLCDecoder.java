package BWZ;

import BWZ.Util.BWZUtil;
import Utility.Util;

import java.util.ArrayList;

class ZeroRLCDecoder {

    private short[] text;

    ZeroRLCDecoder(short[] text) {
        this.text = text;
    }

    short[] Decode() {
        ArrayList<Short> temp = new ArrayList<>();
        ArrayList<Short> runLengths = new ArrayList<>();
        int i = 0;
        while (i < text.length) {
            short b = text[i];

            if (b < (short) 2) {
                runLengths.add(b);
            } else {
                if (!runLengths.isEmpty()) {
                    int length = BWZUtil.runLengthInverse(Util.collectionToShortArray(runLengths));
                    runLengths.clear();
                    for (int j = 0; j < length; j++) {
                        temp.add((short) 0);
                    }
                }
                temp.add((short) (b - 1));
            }
            i += 1;

        }
        if (!runLengths.isEmpty()) {  // Last few 0's
            int length = BWZUtil.runLengthInverse(Util.collectionToShortArray(runLengths));
            for (int j = 0; j < length; j++) {
                temp.add((short) 0);
            }
        }
        return Util.collectionToShortArray(temp);
    }
}
