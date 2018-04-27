package WinLzz.BWZ.Util;

import WinLzz.Utility.Util;
import java.util.LinkedList;
import java.util.List;

public abstract class BWZUtil {

    /**
     * Returns the array of the {0, 1} base-2 bijective enumeration.
     *
     * @param length the number to be transformed.
     * @return the bijective enumeration.
     */
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

    /**
     * Reverses the bijective enumeration.
     *
     * @param s the array of {0, 1} base-2 bijective enumeration.
     * @return the original number.
     */
    public static int runLengthInverse(short[] s) {
        int result = 0;
        for (int i = 0; i < s.length; i++) result += (Math.pow(2, i) * (s[i] + 1));
        return result;
    }

    public static int runLengthInverse(List<Short> s) {
        int result = 0;
        for (int i = 0; i < s.size(); i++) result += (Math.pow(2, i) * (s.get(i) + 1));
        return result;
    }

    /**
     * Returns the array of the {0, 1} base-2 bijective enumeration.
     *
     * @param length the number to be transformed.
     * @return the bijective enumeration.
     */
    public static byte[] runLengthByte(int length) {
        LinkedList<Byte> list = new LinkedList<>();
        int remainder = length;
        int base = 1;
        while (remainder > 0) {
            if (remainder % (base * 2) == 0) {
                list.addLast((byte) 1);
                remainder -= base * 2;
            } else {
                list.addLast((byte) 0);
                remainder -= base;
            }
            base *= 2;
        }
        return Util.collectionToArray(list);
    }

    /**
     * Reverses the bijective enumeration.
     *
     * @param s the array of {0, 1} base-2 bijective enumeration.
     * @return the original number.
     */
    public static int runLengthInverse(byte[] s) {
        int result = 0;
        for (int i = 0; i < s.length; i++) result += (Math.pow(2, i) * (s[i] + 1));
        return result;
    }
}
