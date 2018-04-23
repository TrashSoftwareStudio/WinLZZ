package Utility;

import java.io.*;

public abstract class Bytes {

    public static String charMultiply(char c, int times) {
        char[] array = new char[times];
        for (int i = 0; i < times; i++) array[i] = c;
        return String.valueOf(array);
    }

    public static byte bitStringToByte(String bits) {
        return (byte) Integer.parseInt(bits, 2);
    }

    public static byte bitStringToByteNo8(String bits) {
        return (byte) Integer.parseInt(bits + charMultiply('0', 8 - bits.length()), 2);
    }

    public static String byteToBitString(byte b) {
        return numberToBitString((b & 0xff), 8);
    }

    /**
     * Convert a binary StringBuilder to a byte array.
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
     * Convert a binary StringBuilder to a byte array.
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
     * Convert a binary StringBuilder to a short array.
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
     * Convert a binary StringBuilder to a byte array.
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
    public static String numberToBitString(int num, int length) {
        String s = Integer.toBinaryString(num);
        return charMultiply('0', length - s.length()) + s;
    }

    public static StringBuilder bytesToStringBuilder(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) builder.append(byteToBitString(b));
        return builder;
    }

    public static String bytesToString(byte[] bytes) {
        return bytesToStringBuilder(bytes).toString();
    }

    public static String booleanArrayToBitString(boolean[] array) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++)
            if (array[i]) sb.append('1');
            else sb.append('0');
        return sb.toString();
    }

    public static boolean[] bitStringToBooleanArray(String bits) {
        boolean[] array = new boolean[8];
        for (int i = 0; i < 8; i++) array[i] = bits.charAt(i) != '0';
        return array;
    }

    public static int bytesToInt32(byte[] b) {
        return (b[0] & 0xff) << 24 | (b[1] & 0xff) << 16 | (b[2] & 0xff) << 8 | (b[3] & 0xff);
    }

    public static byte[] intToBytes32(int i) {
        return new byte[]{(byte) ((i >> 24) & 0xff), (byte) ((i >> 16) & 0xff), (byte) ((i >> 8) & 0xff), (byte) (i & 0xff)};
    }

    public static int bytesToInt24(byte[] b) {
        return (b[0] & 0xff) << 16 | (b[1] & 0xff) << 8 | (b[2] & 0xff);
    }

    public static byte[] intToBytes24(int i) {
        return new byte[]{(byte) ((i >> 16) & 0xff), (byte) ((i >> 8) & 0xff), (byte) (i & 0xff)};
    }

    public static byte[] longToBytes(long l) {
        byte[] result = new byte[8];
        for (int i = 0; i < 8; i++) result[i] = (byte) ((l >> ((7 - i) * 8)) & 0xff);
        return result;
    }

    public static long bytesToLong(byte[] b) {
        long result = 0;
        for (int i = 0; i < 8; i++) result = (long) (b[i] & 0xff) << ((7 - i) * 8) | result;
        return result;
    }

    public static short bytesToShort(byte[] b) {
        return (short) ((b[0] & 0xff) << 8 | (b[1] & 0xff));
    }

    public static byte[] shortToBytes(short i) {
        return new byte[]{(byte) ((i >> 8) & 0xff), (byte) (i & 0xff)};
    }

    public static short[] intToShorts(int i) {
        return new short[]{(short) (i >> 16), (short) i};
    }

    public static int shortArrayToInt(short[] s) {
        return ((s[0] & 0xffff) << 16) | (s[1] & 0xffff);
    }

    public static boolean[] numToBool3bit(int num) {
        boolean[] array = new boolean[3];
        String s = Integer.toBinaryString(num);
        String s2 = charMultiply('0', 3 - s.length()) + s;
        for (int i = 0; i < 3; i++) array[i] = (s2.charAt(i) == '1');
        return array;
    }

    public static byte[] stringEncode(String name) throws UnsupportedEncodingException {
        return name.getBytes("utf-8");
    }

    public static short[] byteArrayToShortArray(byte[] array) {
        short[] result = new short[array.length / 2];
        for (int i = 0; i < result.length; i++) result[i] = bytesToShort(new byte[]{array[i * 2], array[i * 2 + 1]});
        return result;
    }

    public static byte[] shortArrayToByteArray(short[] array) {
        byte[] result = new byte[array.length * 2];
        for (int i = 0; i < array.length; i++) System.arraycopy(shortToBytes(array[i]), 0, result, i * 2, 2);
        return result;
    }

    public static String stringDecode(byte[] nameBytes) throws UnsupportedEncodingException {
        return new String(nameBytes, "utf-8");

    }

    public static short[] byteArrayToUnsignedShorts(byte[] array) {
        short[] result = new short[array.length];
        for (int i = 0; i < array.length; i++) result[i] = (short) (array[i] & 0xff);
        return result;
    }

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
