package trashsoftware.winBwz.core.bwz.bwt;

import trashsoftware.winBwz.utility.Bytes;
import trashsoftware.winBwz.utility.Util;

import java.util.ArrayDeque;

/**
 * A inverse transformer of Burrows-Wheeler Transform.
 * <p>
 * This program takes a short array as input text.
 *
 * @author zbh
 * @since 0.5
 */
public class BWTDecoder {

    private final int[] cmpText;
    private final int origIndex;
    
    private static long timeAcc = 0;

    /**
     * Creates a new {@code BWTDecoder} instance.
     *
     * @param cmpText the text after bwt transformation.
     * @param origIndex the index of the original flag row
     */
    private BWTDecoder(int[] cmpText, int origIndex) {
        this.cmpText = cmpText;
        this.origIndex = origIndex;
    }
    
    public static BWTDecoder createFromCmpText(int[] cmpText, int sizeBytes) {
        byte[] indexBytes = new byte[sizeBytes];
        for (int i = 0; i < sizeBytes; i++) {
            indexBytes[i] = (byte) cmpText[i];
        }
        int origIndex;
        if (sizeBytes == 3) {
            origIndex = Bytes.bytesToInt24(indexBytes);
        } else if (sizeBytes == 4) {
            origIndex = Bytes.bytesToInt32(indexBytes);
        } else {
            throw new IllegalArgumentException("Size bytes can only be 3 or 4");
        }
        int[] pureCmpText = new int[cmpText.length - sizeBytes];
        System.arraycopy(cmpText, sizeBytes, pureCmpText, 0, pureCmpText.length);
        return new BWTDecoder(pureCmpText, origIndex);
    }
    
    public static BWTDecoder createWithOrigIndex(int[] cmpText, int origIndex) {
        return new BWTDecoder(cmpText, origIndex);
    }

    /**
     * Inverses the BWT.
     * <p>
     * Algorithm found in:
     * <a href="https://www.geeksforgeeks.org/inverting-burrows-wheeler-transform/">...</a>
     *
     * @return The original text.
     */
    public byte[] decode2() {
        int len = cmpText.length;
        int[] lShift = new int[len];
        int[] sorted = new int[len];
        System.arraycopy(cmpText, 0, sorted, 0, len);
        Util.countingSort(sorted, 257);

        long begin = System.currentTimeMillis();

        IntegerLinkedList[] lists = new IntegerLinkedList[257];
        for (int i = 0; i < 257; i++) lists[i] = new IntegerLinkedList();
        for (int i = 0; i < len; i++) lists[cmpText[i]].addLast(i);
        for (int i = 0; i < len; i++) lShift[i] = lists[sorted[i]].removeFirst();

        timeAcc += System.currentTimeMillis() - begin;
        System.out.println(timeAcc);

        byte[] result = new byte[len - 1];

        int currentRow = origIndex;
        for (int i = 0; i < len - 1; ++i) {
            currentRow = lShift[currentRow];
            result[i] = (byte) (cmpText[currentRow] - 1);
        }
        return result;
    }

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
        int n = cmpText.length;
        int l = 0;
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

    private byte[] mtl() {
        int[] lf = computeLF();
        int p = origIndex;
        int l = 0;
        int n = cmpText.length;
        byte[] tr = new byte[n];
        while (l < n) {
            tr[l] = (byte) (cmpText[p] - 1);
            p = lf[p];
            l++;
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

    /**
     * An {@code ArrayDeque} that holds {@code Integer} as elements.
     */
    static class IntegerLinkedList extends ArrayDeque<Integer> {

        /**
         * Creates a new {@code IntegerLinkedList} instance.
         */
        IntegerLinkedList() {
            super();
        }
    }
}
