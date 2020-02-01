package trashsoftware.win_bwz.core.bwz;

/**
 * The inverse transformer of Move-To-Front transformation.
 * <p>
 * This program takes a byte array as input text.
 *
 * @author zbh
 * @since 0.6
 */
public class MTFInverseByte {

    private byte[] text;

    /**
     * Creates a new instance of {@code MTFInverseByte}.
     *
     * @param text the text to be inverse transformed.
     */
    public MTFInverseByte(byte[] text) {
        this.text = text;
    }

    /**
     * Returns the text after the inverse transform.
     *
     * @param alphabetSize the size of alphabet in <code>text</code>
     * @return the text after the inverse transform.
     */
    public byte[] Inverse(int alphabetSize) {
        byte[] dictionary = new byte[alphabetSize];
        for (int i = 0; i < alphabetSize; ++i) dictionary[i] = (byte) i;

        byte[] result = new byte[text.length];
        int index = 0;
        for (byte b : text) {
            int ib = b & 0xff;
            byte s = dictionary[ib];
            if (ib > 0) {
                System.arraycopy(dictionary, 0, dictionary, 1, ib);
                dictionary[0] = s;
            }
            result[index++] = s;
        }
        return result;
    }
}
