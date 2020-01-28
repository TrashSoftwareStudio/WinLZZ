package trashsoftware.win_bwz.bwz;

import java.util.LinkedList;

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
        LinkedList<Byte> ll = new LinkedList<>();
        for (int i = 0; i < alphabetSize; i++) ll.addLast((byte) i);
        byte[] result = new byte[text.length];
        int index = 0;
        for (byte b : text) {
            int i = b & 0xff;
            Byte s;
            if (i == 0) s = ll.getFirst();
            else {
                s = ll.remove(i);
                ll.addFirst(s);
            }
            result[index] = s;
            index += 1;
        }
        return result;
    }
}