//package BWZ;
//
//public class SuffixArrayDC3 {
//
//    private int max_n;
//
//    int[] wa, wb, wv, ws = new int[max_n];
//
//    SuffixArrayDC3() {
//
//    }
//
//    int F(int x, int tb) {
//        return ((x) / 3 + ((x) % 3 == 1 ? 0 : tb));
//    }
//
//    int G(int x, int tb) {
//        return ((x) < tb ? (x) * 3 + 1 : ((x) - tb) * 3 + 2);
//    }
//
//    boolean c0(int[] r, int a, int b) {
//        return r[a] == r[b] && r[a + 1] == r[b + 1] && r[a + 2] == r[b + 2];
//    }
//
//    boolean c12(int k, int[] r, int a, int b) {
//        if (k == 2) {
//            return r[a] < r[b] || r[a] == r[b] && c12(1, r, a + 1, b + 1);
//        } else {
//            return r[a] < r[b] || r[a] == r[b] && wv[a + 1] < wv[b + 1];
//        }
//    }
//
//    void sort(int[] r, int[] a, int[] b, int n, int m) {
//        int i;
//        for (i = 0; i < n; i++) wv[i] = r[a[i]];
//        for (i = 0; i < m; i++) ws[i] = 0;
//        for (i = 0; i < n; i++) ws[wv[i]]++;
//        for (i = 1; i < m; i++) ws[i] += ws[i - 1];
//        for (i = n - 1; i >= 0; i--) b[--ws[wv[i]]] = a[i];
//    }
//
//    void dc3build(int[] r, int[] sa, int n, int m) {
//        int i, j;
//        int[] rn = new int[r.length - n];
//        System.arraycopy(r, n, rn, 0, rn.length);
//        int[] san = new int[sa.length - n];
//        System.arraycopy(sa, n, san, 0, san.length);
//        int ta = 0;
//        int tb = (n + 1) / 3, tbc = 0, p;
//        r[n] = r[n + 1] = 0;
//        for (i = 0; i < n; i++) if (i % 3 != 0) wa[tbc++] = i;
//        sort(r + 2, wa, wb, tbc, m);
//        sort(r + 1, wb, wa, tbc, m);
//        sort(r, wa, wb, tbc, m)
//    }
//}
