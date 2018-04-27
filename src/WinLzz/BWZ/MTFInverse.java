package WinLzz.BWZ;

import WinLzz.BWZ.Util.SplitLinkedList;

class MTFInverse {

    private short[] text;

    MTFInverse(short[] text) {
        this.text = text;
    }

    @Deprecated
    short[] decode2() {
        short[] dictionary = new short[257];
        for (short i = 0; i < 257; i++) dictionary[i] = i;
        short[] result = new short[text.length];
        int index = 0;
        for (short b : text) {
            short s = dictionary[b];
            System.arraycopy(dictionary, 0, dictionary, 1, b);
            dictionary[0] = s;
            result[index++] = s;
        }
        return result;
    }

    short[] decode() {
        SplitLinkedList sll = new SplitLinkedList();
        short[] result = new short[text.length];
        int index = 0;
        for (short i : text) result[index++] = sll.getAndMove(i);
        return result;
    }
}

