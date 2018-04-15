package BWZ;

import java.util.LinkedList;

class MTFInverse {

    private short[] text;

    MTFInverse(short[] text) {
        this.text = text;
    }

    short[] Inverse() {
        LinkedList<Short> dictionary = new LinkedList<>();
        for (short i = 0; i < 257; i++) dictionary.addLast(i);
        short[] result = new short[text.length];
        int index = 0;
        for (short b : text) {
            int i = b & 0xffff;
            Short s;
            if (i == 0) s = dictionary.getFirst();  // If it is the first the element then do not change the LinkedList.
            else {
                s = dictionary.remove(i);
                dictionary.addFirst(s);
            }
            result[index++] = s;
        }
        return result;
    }
}

