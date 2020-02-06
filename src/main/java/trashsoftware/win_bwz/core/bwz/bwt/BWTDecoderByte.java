package trashsoftware.win_bwz.core.bwz.bwt;

/**
 * A inverse transformer of Burrows-Wheeler Transform.
 * <p>
 * This program takes a byte array as input text.
 *
 * @author zbh
 * @since 0.6
 */
public class BWTDecoderByte {

    private byte[] cmpText;
    private int origIndex;

    /**
     * Creates a new {@code BWTDecoderByte} instance.
     *
     * @param cmpText      the text after bwt transformation.
     * @param origRowIndex the index of the row0 in the text after transformation,
     *                     where row0 is the first row of the original text.
     */
    public BWTDecoderByte(byte[] cmpText, int origRowIndex) {
        origIndex = origRowIndex;
        this.cmpText = cmpText;
    }

    /**
     * Inverses the BWT.
     *
     * @return The original text.
     */
    public byte[] Decode() {
        byte[] inv = mtl_sa();
        byte[] rev = new byte[inv.length - 1];
        for (int i = 1; i < inv.length; i++) {
            rev[inv.length - i - 1] = inv[i];
        }
        return rev;
    }

    private byte[] mtl_sa() {
        int[] lf = computeLF();
        int[] ll = computeLL(lf);
        int[] lf2 = computeLF2(lf);
        int p = origIndex;
        int l = 0;
        int n = cmpText.length;
        byte[] tr = new byte[n];
        while (l < n - 1) {
            tr[l] = (byte) (ll[p << 1] - 1);
            tr[l + 1] = (byte) (ll[(p << 1) + 1] - 1);
            p = lf2[p];
            l += 2;
        }
        if (l == n - 1) {
            tr[l] = (byte) (ll[p << 1] - 1);
        }
        return tr;
    }

    private int[] computeCounts() {
        int[] counts = new int[258];
        for (int value : cmpText) {
            counts[value + 1] += 1;
        }
        for (int j = 1; j < 257; j++) {
            counts[j] = counts[j] + counts[j - 1];
        }
        return counts;
    }

    private int[] computeLL(int[] lf) {
        int[] ll = new int[cmpText.length * 2];
        for (int i = 0; i < cmpText.length; ++i) {
            ll[i << 1] = cmpText[i];
            ll[(i << 1) + 1] = cmpText[lf[i]];
        }
        return ll;
    }

    private int[] computeLF2(int[] lf) {
        int[] lf2 = new int[cmpText.length];
        for (int i = 0; i < cmpText.length; ++i) {
            lf2[i] = lf[lf[i]];
        }
        return lf2;
    }

    private int[] computeLF() {
        int[] counts = computeCounts();
        int[] lf = new int[cmpText.length];
        for (int i = 0; i < cmpText.length; i++) {
            lf[i] = counts[cmpText[i]];
            counts[cmpText[i]] += 1;
        }
        return lf;
    }
}
