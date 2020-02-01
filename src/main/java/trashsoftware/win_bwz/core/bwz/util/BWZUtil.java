package trashsoftware.win_bwz.core.bwz.util;

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
     * Returns the original number that was encoded using base-2 bijective enumeration.
     */
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
    public static int runLengthByte(int length, byte[] result, int resultIndex) {
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
}
