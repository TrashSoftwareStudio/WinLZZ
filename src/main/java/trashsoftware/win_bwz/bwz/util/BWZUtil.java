package trashsoftware.win_bwz.bwz.util;

import trashsoftware.win_bwz.utility.Util;

import java.util.LinkedList;
import java.util.List;

/**
 * This class consists of static methods that gives utilities to mostly the BWZ package.
 *
 * @since 0.5
 */
public abstract class BWZUtil {

    /**
     * Returns the array of the {0, 1} base-2 bijective enumeration.
     */
    public static int runLength(int length, int[] result, int resultIndex) {
        int remainder = length;
        int base = 1;
        int index = 0;
        while (remainder > 0) {
            if ((remainder & ((base << 1) - 1)) == 0) {
                result[resultIndex + index++] = 1;
                remainder -= (base << 1);
            } else {
                result[resultIndex + index++] = 0;
                remainder -= base;
            }
            base <<= 1;
        }
        return index;
    }

    /**
     * Reverses the bijective enumeration.
     *
     * @param s the array of {0, 1} base-2 bijective enumeration.
     * @return the original number.
     */
    public static int runLengthInverse(int[] s) {
        int result = 0;
        for (int i = 0; i < s.length; i++) result += (Math.pow(2, i) * (s[i] + 1));
        return result;
    }

    /**
     * Returns the original number that was encoded using base-2 bijective enumeration.
     *
     * @param s the base-2 bijective enumeration list.
     * @return the original number.
     */
    public static int runLengthInverse(List<Integer> s) {
        int result = 0;
        for (int i = 0; i < s.size(); i++) result += (Math.pow(2, i) * (s.get(i) + 1));
        return result;
    }

    public static int runLengthInverse(int[] buffer, int bufferLen) {
        int result = 0;
        for (int i = 0; i < bufferLen; ++i) {
            result += buffer[i] == 0 ? (1 << i) : (1 << (i + 1));  // buffer[i] is either 0 or 1
        }
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
