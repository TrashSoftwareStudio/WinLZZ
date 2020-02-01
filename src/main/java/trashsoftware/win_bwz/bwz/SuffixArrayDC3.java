package trashsoftware.win_bwz.bwz;

/**
 * A suffix array constructor that build a suffix array using bc3 algorithm.
 *
 * @since 0.5.2
 */
public class SuffixArrayDC3 {

    private int[] wa;
    private int[] wb;
    private int[] ws;
    private int[] wv;
    private int[] sa;
    private int n;

    /**
     * Creates a new instance of {@code SuffixArrayDC3}.
     * <p>
     * This constructor also builds the suffix array, using dc3 algorithm.
     *
     * @param text         the original text.
     * @param alphabetSize the size of alphabet.
     */
    public SuffixArrayDC3(int[] text, int alphabetSize) {
        int max_n = text.length + 256;  // For safe
        wa = new int[max_n];
        wb = new int[max_n];
        ws = new int[max_n];
        wv = new int[max_n];

        int n = text.length;
        this.n = n;
        int[] r = new int[n * 3];
        System.arraycopy(text, 0, r, 0, n);
        sa = new int[n * 3];
        dc3(new ArrayWrapper(r, 0), new ArrayWrapper(sa, 0), n, alphabetSize);
    }

    /**
     * Returns the suffix array.
     *
     * @return the suffix array.
     */
    public int[] getSa() {
        int[] result = new int[n];
        System.arraycopy(sa, 0, result, 0, n);
        return result;
    }

    private int F(int x, int tb) {
        return ((x) / 3 + ((x) % 3 == 1 ? 0 : tb));
    }

    private int G(int x, int tb) {
        return ((x) < tb ? (x) * 3 + 1 : ((x) - tb) * 3 + 2);
    }

    private boolean c0(ArrayWrapper r, int a, int b) {
        return r.get(a) == r.get(b) && r.get(a + 1) == r.get(b + 1) && r.get(a + 2) == r.get(b + 2);
    }

    private boolean c12(int k, ArrayWrapper r, int a, int b) {
        if (k == 2) return r.get(a) < r.get(b) || r.get(a) == r.get(b) && c12(1, r, a + 1, b + 1);
        else return r.get(a) < r.get(b) || r.get(a) == r.get(b) && wv[a + 1] < wv[b + 1];
    }

    private void sort(ArrayWrapper r, int[] a, int[] b, int n, int m) {
        int i;
        for (i = 0; i < n; i++) wv[i] = r.get(a[i]);
        for (i = 0; i < m; i++) ws[i] = 0;
        for (i = 0; i < n; i++) ws[wv[i]]++;
        for (i = 1; i < m; i++) ws[i] += ws[i - 1];
        for (i = n - 1; i >= 0; i--) b[--ws[wv[i]]] = a[i];
    }

    private void dc3(ArrayWrapper r, ArrayWrapper sa, int n, int m) {
        int i, j;
        ArrayWrapper rn = r.createNew(n);
        ArrayWrapper san = sa.createNew(n);
        int ta = 0;
        int tb = (n + 1) / 3, tbc = 0, p;
        r.set(n, 0);
        r.set(n + 1, 0);
        for (i = 0; i < n; i++) if (i % 3 != 0) wa[tbc++] = i;
        ArrayWrapper rPlusTwo = r.createNew(2);
        ArrayWrapper rPlusOne = r.createNew(1);
        sort(rPlusTwo, wa, wb, tbc, m);
        sort(rPlusOne, wb, wa, tbc, m);
        sort(r, wa, wb, tbc, m);
        for (p = 1, rn.set(F(wb[0], tb), 0), i = 1; i < tbc; i++)
            rn.set(F(wb[i], tb), c0(r, wb[i - 1], wb[i]) ? p - 1 : p++);
        if (p < tbc) dc3(rn, san, tbc, p);
        else for (i = 0; i < tbc; i++) san.set(rn.get(i), i);
        for (i = 0; i < tbc; i++) if (san.get(i) < tb) wb[ta++] = san.get(i) * 3;
        if (n % 3 == 1) wb[ta++] = n - 1;
        sort(r, wb, wa, ta, m);

        for (i = 0; i < tbc; i++) wv[wb[i] = G(san.get(i), tb)] = i;
        for (i = 0, j = 0, p = 0; i < ta && j < tbc; p++)
            sa.set(p, c12(wb[j] % 3, r, wa[i], wb[j]) ? wa[i++] : wb[j++]);
        for (; i < ta; p++) sa.set(p, wa[i++]);
        for (; j < tbc; p++) sa.set(p, wb[j++]);
    }
}


/**
 * Wrapper for an integer array, used to simulate the array pointer operations in C/C++.
 *
 * @author zbh
 * @since 0.7.3
 */
class ArrayWrapper {

    /**
     * The common integer array.
     */
    private int[] array;

    /**
     * The position where the indexing starts in this {@code ArrayWrapper}.
     */
    private int offset;

    /**
     * Creates a new {@code ArrayWrapper} instance.
     *
     * @param array  the array
     * @param offset the index where to count as {@code 0}
     */
    ArrayWrapper(int[] array, int offset) {
        this.array = array;
        this.offset = offset;
    }

    /**
     * Returns the value stored in <code>index</code> of this {@code ArrayWrapper}.
     *
     * @param index the index
     * @return the value at <code>index</code> of this {@code ArrayWrapper}
     */
    int get(int index) {
        return array[offset + index];
    }

    /**
     * Sets the value at position <code>index</code> to <code>value</code>.
     *
     * @param index the position in this {@code ArrayWrapper}
     * @param value the value to be set
     */
    void set(int index, int value) {
        array[offset + index] = value;
    }

    /**
     * Creates a new instance of {@code ArrayWrapper}, which shares a same integer array with
     * this {@code ArrayWrapper}, but indices starts at <code>offset</code> plus the offset of
     * this {@code ArrayWrapper}.
     *
     * @param offset the further offset of the new instance
     * @return a new new instance of {@code ArrayWrapper}
     */
    ArrayWrapper createNew(int offset) {
        return new ArrayWrapper(array, offset + this.offset);
    }
}
