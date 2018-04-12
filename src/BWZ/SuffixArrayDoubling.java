package BWZ;

class SuffixArrayDoubling {

    private short[] text;

    private int n;

    private int[] sa, t, t2, c;

    SuffixArrayDoubling(short[] text) {
        n = text.length;
        int max_n = n + 256;
        this.text = text;
        sa = new int[n];
        t = new int[max_n];
        t2 = new int[max_n];
        c = new int[max_n];
    }

    /**
     * Build suffix array using doubling algorithm.
     * <p>
     * Algorithm from:
     * https://wenku.baidu.com/view/228caa45b307e87101f696a8.html
     *
     * @param m initial alphabet size.
     */
    void build(int m) {
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
            for (i = 0; i < n; i++) {
                if (sa[i] >= k) y[p++] = sa[i] - k;
            }
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
            for (i = 1; i < n; i++) {
                x[sa[i]] = y[sa[i - 1]] == y[sa[i]] && y[sa[i - 1] + k] == y[sa[i] + k] ? p - 1 : p++;
            }
            if (p >= n) break;
            m = p;

        }
    }

    int[] getSa() {
        return sa;
    }

}
