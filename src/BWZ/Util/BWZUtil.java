package BWZ.Util;

import Utility.Util;

import java.util.LinkedList;

public abstract class BWZUtil {

    public static short[] runLength(int length) {
        LinkedList<Short> list = new LinkedList<>();
        int remainder = length;
        int base = 1;
        while (remainder > 0) {
            if (remainder % (base * 2) == 0) {
                list.addLast((short) 1);
                remainder -= base * 2;
            } else {
                list.addLast((short) 0);
                remainder -= base;
            }
            base *= 2;
        }
        return Util.collectionToShortArray(list);
    }

    public static int runLengthInverse(short[] s) {
        int result = 0;
        for (int i = 0; i < s.length; i ++) {
            result += (Math.pow(2, i) * (s[i] + 1));
        }
        return result;
    }
}
