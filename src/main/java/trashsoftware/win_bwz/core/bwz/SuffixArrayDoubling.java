package trashsoftware.win_bwz.core.bwz;

/**
 * A suffix array constructor that build a suffix array using doubling algorithm.
 * <p>
 * This class takes a short array as input text.
 *
 * @since 0.5
 */
public class SuffixArrayDoubling {

    private int[] text;
    private int n;
    private int[] sa, t, t2, c;

//    private static int[][] saArray, tArray, t2Array, cArray;

    /**
     * Creates a new instance of {@code SuffixArrayDoubling}.
     *
     * @param text     the original text.
     * @param threadId the thread count of this thread
     */
    public SuffixArrayDoubling(int[] text, int threadId) {
        n = text.length;
        int max_n = n + 65536;
        this.text = text;
        sa = new int[n];
        t = new int[max_n];
        t2 = new int[max_n];
        c = new int[max_n];
    }

//    public static void allocateArrays(int maxTextLen, int maxThreads) {
//        saArray = new int[maxThreads][];
//        tArray = new int[maxThreads][];
//        t2Array = new int[maxThreads][];
//        cArray = new int[maxThreads][];
//
////        System.out.println("allocated!");
//
//        int n = getN(maxTextLen);
//        int maxN = n + 65536;
//
//        for (int i = 0; i < maxThreads; ++i) {
//            saArray[i] = new int[n];
//            tArray[i] = new int[maxN];
//            t2Array[i] = new int[maxN];
//            cArray[i] = new int[maxN];
//        }
//    }
//
//    public static void allocateArraysIfNot(int maxTextLen, int maxThreads) {
//        if (saArray == null     // not allocated
//                || saArray.length != maxThreads      // previous threads usage not equal to this
//                || saArray[0].length != getN(maxTextLen))     // previous max block length not equal to this
//            allocateArrays(maxTextLen, maxThreads);
//    }
//
//    private static int getN(int maxTextLen) {
//        return maxTextLen + 1;
//    }
//
//    private void initArrays(int threadId) {
//        sa = saArray[threadId];
//        t = tArray[threadId];
//        t2 = t2Array[threadId];
//        c = cArray[threadId];
//
//        Arrays.fill(sa, 0);
//        Arrays.fill(t, 0);
//        Arrays.fill(t2, 0);
//        Arrays.fill(c, 0);
//    }

    /**
     * Build suffix array using doubling algorithm.
     * <p>
     * Algorithm from:
     * https://wenku.baidu.com/view/228caa45b307e87101f696a8.html
     *
     * @param m initial alphabet size.
     */
    @SuppressWarnings("all")
    public void build(int m) {
        int k, i;
        int[] x, y;
        x = t;
        y = t2;
        for (i = 0; i < n; i++) c[x[i] = text[i]]++;
        for (i = 1; i < m; i++) c[i] += c[i - 1];
        for (i = n - 1; i >= 0; i--) sa[--c[x[i]]] = i;
        for (k = 1; k <= n; k <<= 1) {
            int p = 0;
            for (i = n - k; i < n; i++) y[p++] = i;
            for (i = 0; i < n; i++) if (sa[i] >= k) y[p++] = sa[i] - k;
            for (i = 0; i < m; i++) c[i] = 0;
            for (i = 0; i < n; i++) c[x[y[i]]]++;
            for (i = 1; i < m; i++) c[i] += c[i - 1];
            for (i = n - 1; i >= 0; i--) sa[--c[x[y[i]]]] = y[i];

            // swap
            int[] temp = x;
            x = y;
            y = temp;

            p = 1;
            x[sa[0]] = 0;
            for (i = 1; i < n; i++)
                x[sa[i]] = y[sa[i - 1]] == y[sa[i]] && y[sa[i - 1] + k] == y[sa[i] + k] ? p - 1 : p++;
            if (p >= n) break;
            m = p;
        }
    }

    /**
     * Returns the suffix array.
     *
     * @return the suffix array.
     */
    public int[] getSa() {
//        if (n == sa.length)
        return sa;
//        else {
//            int[] rtn = new int[n];
//            System.arraycopy(sa, 0, rtn, 0, n);
//            return rtn;
//        }
    }
}

