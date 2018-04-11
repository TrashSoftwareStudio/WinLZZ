package LongHuffman;

public abstract class Util {

    static short[] byteArrayToShorts(byte[] array) {
        short[] result = new short[array.length / 2];
        for (int i = 0; i < result.length; i++) {
            result[i] = (short) (((array[i * 2] & 0xff) << 8) | (array[i * 2 + 1] & 0xff));
        }
        return result;
    }
}
