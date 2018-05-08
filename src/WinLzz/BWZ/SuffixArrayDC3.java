package WinLzz.BWZ;

public class SuffixArrayDC3 {

    private int[] wa;
    private int[] wb;
    private int[] ws;
    private int[] wv;
    private int[] sa;
    private int n;

    public SuffixArrayDC3(short[] text, int alphabetSize) {
        int max_n = text.length + 256;  // For safe
        wa = new int[max_n];
        wb = new int[max_n];
        ws = new int[max_n];
        wv = new int[max_n];

        int n = text.length;
        this.n = n;
        int[] r = new int[n * 3];
        for (int i = 0; i < n; i++) r[i] = text[i];
        sa = new int[n * 3];
        dc3(r, sa, n, alphabetSize);
    }

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

    private boolean c0(int[] r, int a, int b) {
        return r[a] == r[b] && r[a + 1] == r[b + 1] && r[a + 2] == r[b + 2];
    }

    private boolean c12(int k, int[] r, int a, int b) {
        if (k == 2) return r[a] < r[b] || r[a] == r[b] && c12(1, r, a + 1, b + 1);
        else return r[a] < r[b] || r[a] == r[b] && wv[a + 1] < wv[b + 1];
    }

    private void sort(int[] r, int[] a, int[] b, int n, int m) {
        int i;
        for (i = 0; i < n; i++) wv[i] = r[a[i]];
        for (i = 0; i < m; i++) ws[i] = 0;
        for (i = 0; i < n; i++) ws[wv[i]]++;
        for (i = 1; i < m; i++) ws[i] += ws[i - 1];
        for (i = n - 1; i >= 0; i--) b[--ws[wv[i]]] = a[i];
    }

    private void dc3(int[] r, int[] sa, int n, int m) {
        int i, j;
        int[] rn = new int[r.length - n];
        System.arraycopy(r, n, rn, 0, rn.length);
        int[] san = new int[sa.length - n];
        System.arraycopy(sa, n, san, 0, san.length);
        int ta = 0;
        int tb = (n + 1) / 3, tbc = 0, p;
        r[n] = r[n + 1] = 0;
        for (i = 0; i < n; i++) if (i % 3 != 0) wa[tbc++] = i;
        int[] rPlusTwo = new int[r.length - 2];
        System.arraycopy(r, 2, rPlusTwo, 0, rPlusTwo.length);
        int[] rPlusOne = new int[r.length - 1];
        System.arraycopy(r, 1, rPlusOne, 0, rPlusOne.length);
        sort(rPlusTwo, wa, wb, tbc, m);
        sort(rPlusOne, wb, wa, tbc, m);
        sort(r, wa, wb, tbc, m);
        for (p = 1, rn[F(wb[0], tb)] = 0, i = 1; i < tbc; i++) rn[F(wb[i], tb)] = c0(r, wb[i - 1], wb[i]) ? p - 1 : p++;
        if (p < tbc) dc3(rn, san, tbc, p);
        else for (i = 0; i < tbc; i++) san[rn[i]] = i;
        for (i = 0; i < tbc; i++) if (san[i] < tb) wb[ta++] = san[i] * 3;
        if (n % 3 == 1) wb[ta++] = n - 1;
        sort(r, wb, wa, ta, m);
        for (i = 0; i < tbc; i++) wv[wb[i] = G(san[i], tb)] = i;
        for (i = 0, j = 0, p = 0; i < ta && j < tbc; p++) sa[p] = c12(wb[j] % 3, r, wa[i], wb[j]) ? wa[i++] : wb[j++];
        for (; i < ta; p++) sa[p] = wa[i++];
        for (; j < tbc; p++) sa[p] = wb[j++];
    }
}
