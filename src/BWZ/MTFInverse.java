package BWZ;

import BWZ.Util.LinkedDictionary;
import Utility.Util;

import java.util.ArrayList;

class MTFInverse {

    private short[] text;

    MTFInverse(short[] text) {
        this.text = text;
    }

    short[] Inverse() {
        LinkedDictionary ld = new LinkedDictionary();
        ld.initialize(257);
        ArrayList<Short> temp = new ArrayList<>();
        for (short b : text) temp.add(ld.getAndMove(b & 0xffff));
        return Util.collectionToShortArray(temp);
    }
}

class MTFInverseByte {

    private byte[] text;

    MTFInverseByte(byte[] text) {
        this.text = text;
    }

    byte[] Inverse() {
        LinkedDictionary ld = new LinkedDictionary();
        ld.initialize(16);
        ArrayList<Byte> temp = new ArrayList<>();
        for (byte b : text) temp.add((byte) ld.getAndMove(b & 0xffff));
        return Util.collectionToArray(temp);
    }
}
