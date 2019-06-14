package trashsoftware.win_bwz.Utility;

import java.nio.charset.StandardCharsets;

/**
 * A class that consists of byte and bit level manipulations.
 *
 * @since 0.4
 */
@SuppressWarnings("unused")
public abstract class Bytes {

    /**
     * Returns the String that consists of <code>times</code> character <code>c</code>.
     *
     * @param c     the {@code char} to be multiplied
     * @param times the length of the result {@code String}
     * @return the multiplication result
     */
    public static String charMultiply(char c, int times) {
        char[] array = new char[times];
        for (int i = 0; i < times; i++) array[i] = c;
        return String.valueOf(array);
    }

    static byte bitStringToByte(String bits) {
        return (byte) Integer.parseInt(bits, 2);
    }

    /**
     * Converts binary string into byte.
     * <p>
     * This method fills the binary string to length of 8 by adding 0's at the end of string.
     *
     * @param bits the bit string.
     * @return the byte value of <code>bits</code>
     */
    public static byte bitStringToByteNo8(String bits) {
        return (byte) Integer.parseInt(bits + charMultiply('0', 8 - bits.length()), 2);
    }

    /**
     * Converts byte into binary string.
     *
     * @param b the input byte.
     * @return the bit string value of <code>b</code>
     */
    public static String byteToBitString(byte b) {
        return numberToBitString((b & 0xff), 8);
    }

    /**
     * Convert a binary {@code StringBuilder} to a byte array.
     * <p>
     * The last few digit may be ignored if the length of the StringBuilder is not a multiple of 8.
     *
     * @param builder The binary StringBuilder to be converted.
     * @return A byte array.
     */
    public static byte[] stringBuilderToBytes(StringBuilder builder) {
        int length = builder.length() / 8;
        byte[] result = new byte[length];
        int i = 0;
        int j = 0;
        while (i < length) {
            result[i++] = bitStringToByte(builder.substring(j, j + 8));
            j += 8;
        }
        return result;
    }

    /**
     * Convert a binary {@code StringBuilder} to a byte array.
     * <p>
     * The last few bits will be reserved.
     *
     * @param builder The binary StringBuilder to be converted.
     * @return A byte array.
     */
    public static byte[] stringBuilderToBytesFull(StringBuilder builder) {
        if (builder.length() == 0) return new byte[0];
        int length = builder.length() / 8;
        if (builder.length() % 8 != 0) length += 1;
        byte[] res = new byte[length];
        int i = 0;
        int j = 0;
        while (i < builder.length() - 8) {
            res[j++] = bitStringToByte(builder.substring(i, i + 8));
            i += 8;
        }
        res[j] = bitStringToByteNo8(builder.substring(i));
        return res;
    }

    /**
     * Convert a binary {@code StringBuilder} to a short array.
     * <p>
     * The last few bits will be reserved.
     *
     * @param builder The binary StringBuilder to be converted.
     * @return A short array.
     */
    public static short[] stringBuilderToShortsFull(StringBuilder builder) {
        if (builder.length() == 0) return new short[0];
        int length = builder.length() / 8;
        if (builder.length() % 8 != 0) length += 1;
        short[] res = new short[length];
        int i = 0;
        int j = 0;
        while (i < builder.length() - 8) {
            res[j++] = bitStringToByte(builder.substring(i, i + 8));
            i += 8;
        }
        res[j] = bitStringToByteNo8(builder.substring(i));
        return res;
    }


    /**
     * Convert a binary {@code StringBuilder} to a byte array.
     * <p>
     * The last few bits will be reserved.
     *
     * @param builder The binary StringBuilder to be converted.
     * @return A byte array.
     */
    public static byte[] stringToBytesFull(String builder) {
        if (builder.length() == 0) return new byte[0];
        int length = builder.length() / 8;

        if (builder.length() % 8 != 0) length += 1;
        byte[] res = new byte[length];
        int i = 0;
        int j = 0;
        while (i < builder.length() - 8) {
            res[j++] = bitStringToByte(builder.substring(i, i + 8));
            i += 8;
        }
        res[j] = bitStringToByteNo8(builder.substring(i));
        return res;
    }


    /**
     * Returns the binary string representation of an integer.
     *
     * @param num    The number to be converted.
     * @param length The length of th result.
     * @return The bit string of num.
     */
    public static String numberToBitString(long num, int length) {
        String s = Long.toBinaryString(num);
        return charMultiply('0', length - s.length()) + s;
    }

    /**
     * Converts a byte array into a binary {@code StringBuilder}.
     *
     * @param bytes the byte array to be converted
     * @return the binary {@code StringBuilder} representation of <code>bytes</code>
     */
    public static StringBuilder bytesToStringBuilder(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) builder.append(byteToBitString(b));
        return builder;
    }

    /**
     * Converts a byte array into a binary {@code String}.
     *
     * @param bytes the byte array to be converted
     * @return the binary {@code String} representation of <code>bytes</code>
     */
    public static String bytesToString(byte[] bytes) {
        return bytesToStringBuilder(bytes).toString();
    }

    /**
     * Convert a 4-byte array into signed integer in big-endian.
     *
     * @param b byte array.
     * @return signed integer.
     */
    public static int bytesToInt32(byte[] b) {
        return (b[0] & 0xff) << 24 | (b[1] & 0xff) << 16 | (b[2] & 0xff) << 8 | (b[3] & 0xff);
    }

    /**
     * Convert a 4-byte array into signed integer in little-endian.
     *
     * @param b byte array.
     * @return signed integer.
     */
    public static int bytesToInt32Little(byte[] b) {
        return (b[3] & 0xff) << 24 | (b[2] & 0xff) << 16 | (b[1] & 0xff) << 8 | (b[0] & 0xff);
    }

    /**
     * Convert an integer into a 4-byte array in big-endian.
     *
     * @param i the integer.
     * @return 4-byte array.
     */
    public static byte[] intToBytes32(int i) {
        return new byte[]{(byte) ((i >> 24) & 0xff), (byte) ((i >> 16) & 0xff), (byte) ((i >> 8) & 0xff), (byte) (i & 0xff)};
    }

    /**
     * Convert an integer into a 4-byte array in little-endian.
     *
     * @param i the integer.
     * @return 4-byte array.
     */
    public static byte[] intToBytes32Little(int i) {
        return new byte[]{(byte) (i & 0xff), (byte) ((i >> 8) & 0xff), (byte) ((i >> 16) & 0xff), (byte) ((i >> 24) & 0xff)};
    }

    /**
     * Convert a 3-byte array into signed integer in big-endian.
     *
     * @param b byte array.
     * @return signed integer.
     */
    public static int bytesToInt24(byte[] b) {
        return (b[0] & 0xff) << 16 | (b[1] & 0xff) << 8 | (b[2] & 0xff);
    }

    /**
     * Convert an integer into a 3-byte array in big-endian.
     *
     * @param i the integer.
     * @return 3-byte array.
     */
    public static byte[] intToBytes24(int i) {
        return new byte[]{(byte) ((i >> 16) & 0xff), (byte) ((i >> 8) & 0xff), (byte) (i & 0xff)};
    }

    /**
     * Convert a long into a 8-byte array in big-endian.
     *
     * @param l the long.
     * @return 8-byte array.
     */
    public static byte[] longToBytes(long l) {
        byte[] result = new byte[8];
        for (int i = 0; i < 8; i++) result[i] = (byte) ((l >> ((7 - i) << 3)) & 0xff);
        return result;
    }

    /**
     * Convert a 8-byte array into signed long in big-endian.
     *
     * @param b byte array.
     * @return signed long.
     */
    public static long bytesToLong(byte[] b) {
        long result = 0;
        for (int i = 0; i < 8; i++) result = (long) (b[i] & 0xff) << ((7 - i) << 3) | result;
        return result;
    }

    /**
     * Convert a 2-byte array into signed short in big-endian.
     *
     * @param b byte array.
     * @return signed short.
     */
    public static short bytesToShort(byte[] b) {
        return (short) ((b[0] & 0xff) << 8 | (b[1] & 0xff));
    }

    /**
     * Convert a 2-byte array into signed short in little-endian.
     *
     * @param b byte array.
     * @return signed short.
     */
    public static short bytesToShortLittle(byte[] b) {
        return (short) ((b[1] & 0xff) << 8 | (b[0] & 0xff));
    }

    /**
     * Convert a short into a 2-byte array in big-endian.
     *
     * @param i the short.
     * @return 2-byte array.
     */
    public static byte[] shortToBytes(short i) {
        return new byte[]{(byte) ((i >> 8) & 0xff), (byte) (i & 0xff)};
    }

    /**
     * Convert a short into a 2-byte array in little-endian.
     *
     * @param i the short.
     * @return 2-byte array.
     */
    public static byte[] shortToBytesLittle(short i) {
        return new byte[]{(byte) (i & 0xff), (byte) ((i >> 8) & 0xff)};
    }

    /**
     * Converts the byte array representation of a {@code String} with UTF-8 encoding.
     *
     * @param name the {@code String} to be encoded
     * @return the UTF-8 encoded byte array
     */
    public static byte[] stringEncode(String name) {
        return name.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Converts the String representation of a byte array with UTF-8 encoding.
     *
     * @param nameBytes byte array representation
     * @return the {@code String} encoded by <code>nameBytes</code>
     */
    public static String stringDecode(byte[] nameBytes) {
        return new String(nameBytes, StandardCharsets.UTF_8);

    }

    /**
     * Converts a {@code long} into a readable hexadecimal {@code String}.
     *
     * @param value   the number to be converted
     * @param showAll whether to show zeros before the first valid digit
     * @return the hexadecimal {@code String} of <code>value</code>
     */
    public static String longToHex(long value, boolean showAll) {
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        byte[] array = longToBytes(value);
        char[] temp = new char[16];
        for (int i = 0; i < 8; i++) {
            int num = array[i] & 0xff;
            temp[i * 2] = hexArray[num >> 4];
            temp[i * 2 + 1] = hexArray[num & 0x0f];
        }
        if (showAll) return new String(temp);
        else {
            int i = 0;
            while (i < 16 && temp[i] == '0') i += 1;
            char[] result = new char[16 - i];
            System.arraycopy(temp, i, result, 0, 16 - i);
            return new String(result);
        }
    }
}
