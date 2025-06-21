package trashsoftware.winBwz.rangeCodec;

public class AdaptiveFrequencyTable extends FrequencyTable {

    private final int[] frequencies;
    private FenwickTree tree;

    public AdaptiveFrequencyTable(int symbolLimit) {
        super(symbolLimit);
        this.frequencies = new int[symbolLimit];
        this.tree = new FenwickTree(symbolLimit);

        // Initialize with 1 to avoid zero probabilities
        for (int i = 0; i < symbolLimit; i++) {
            frequencies[i] = 1;
            tree.update(i, 1);
        }
    }

    @Override
    public int getTotal() {
        return tree.getTotal();
    }

    @Override
    public int getSymbolLow(int symbol) {
        return tree.query(symbol);
    }

    @Override
    public int getSymbolHigh(int symbol) {
        return tree.query(symbol + 1);
    }

    @Override
    public int getSymbolFromValue(int value) {
        return tree.findSymbol(value);
    }

    @Override
    public void increment(int symbol) {
        frequencies[symbol]++;
        tree.update(symbol, 1);

        if (tree.getTotal() >= RangeCodingConstants.MAX_FREQ) {
            rescale();
        }
    }

    private void rescale() {
        tree = new FenwickTree(nSymbol);  // Clear tree
        for (int i = 0; i < nSymbol; i++) {
            frequencies[i] = (frequencies[i] + 1) >>> 1;
            tree.update(i, frequencies[i]);
        }
    }
    
    static class FenwickTree {
        private final int[] tree;

        public FenwickTree(int size) {
            tree = new int[size + 1];  // 1-based indexing
        }

        // Adds delta to index (0-based)
        public void update(int index, int delta) {
            for (index++; index < tree.length; index += index & -index)
                tree[index] += delta;
        }

        // Returns prefix sum [0, index) (0-based)
        public int query(int index) {
            int sum = 0;
            for (; index > 0; index -= index & -index)
                sum += tree[index];
            return sum;
        }

        public int getTotal() {
            return query(tree.length - 1);
        }

        // Binary search for symbol from cumulative value
        public int findSymbol(int target) {
            int idx = 0, mask = Integer.highestOneBit(tree.length - 1);
            int sum = 0;

            while (mask != 0) {
                int next = idx + mask;
                if (next < tree.length && sum + tree[next] <= target) {
                    sum += tree[next];
                    idx = next;
                }
                mask >>>= 1;
            }
            return idx;
        }
    }
}
