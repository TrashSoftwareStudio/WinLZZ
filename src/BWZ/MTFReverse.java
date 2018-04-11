package BWZ;

import BWZ.Util.LinkedDictionary;
import Utility.Util;

import java.util.ArrayList;

class MTFReverse {

    private short[] text;

    MTFReverse(short[] text) {
        this.text = text;
    }

    short[] Reverse() {
        LinkedDictionary ld = new LinkedDictionary();
        ld.initialize(257);
        ArrayList<Short> temp = new ArrayList<>();

        for (short b : text) {
            temp.add(ld.getAndMove(b & 0xffff));
        }

        return Util.collectionToShortArray(temp);
    }
}
