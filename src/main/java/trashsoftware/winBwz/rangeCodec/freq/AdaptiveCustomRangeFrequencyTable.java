package trashsoftware.winBwz.rangeCodec.freq;

public class AdaptiveCustomRangeFrequencyTable extends RangedOrder1FrequencyTable<AdaptiveFrequencyTable> {

    private final int[] ranges;
    private final int customMethodFlag;

    public AdaptiveCustomRangeFrequencyTable(int nSymbols, int eofSig, int[] ranges, int methodFlag) {
        super(nSymbols, eofSig, ranges.length, AdaptiveFrequencyTable.class);

        this.ranges = ranges;
        this.customMethodFlag = methodFlag;
    }

    public static AdaptiveCustomRangeFrequencyTable createTypical(int nSymbols, int eofSig) {
        if (nSymbols < 256)
            throw new RuntimeException("Cannot create typical table of non-byte content.");

        int[] segments = new int[]{1, 32, 65, 91, 97, 123, 128, 160, 192, 224, nSymbols};
        return new AdaptiveCustomRangeFrequencyTable(nSymbols, eofSig, segments, 0);
    }

    public static AdaptiveCustomRangeFrequencyTable createExpRanged(final int nSymbols, int eofSig) {
        int[] segments;
        if (nSymbols < 96) {
            segments = new int[]{1, 2, 4, 8, 16, 32, nSymbols};
        } else if (nSymbols < 128) {
            segments = new int[]{1, 2, 4, 8, 16, 32, 64, nSymbols};
        } else {
            segments = new int[]{1, 2, 4, 8, 16, 32, 64, 128, nSymbols};
        }
        return new AdaptiveCustomRangeFrequencyTable(nSymbols, eofSig, segments, 1);
    }

    public static AdaptiveCustomRangeFrequencyTable createBalanced(int nSymbols,
                                                                   int eofSig,
                                                                   int[] data,
                                                                   int nBuckets) {
        int[] freq = new int[nSymbols];
        for (int dat : data) freq[dat]++;
        int[] cumFreq = new int[nSymbols + 1];
        cumFreq[0] = 0;
        for (int i = 0; i < freq.length; i++) {
            cumFreq[i + 1] = cumFreq[i] + freq[i];
        }
        double eachBucket = (double) data.length / nBuckets;
        int[] segments = new int[nBuckets];
//        int lastBucketEnds = 0;
        double cumBucketIdeal = 0;
        int index = 0;
        for (int sym = 0; sym < nSymbols; sym++) {
            double gap = cumFreq[sym + 1] - cumBucketIdeal;
            if (sym != 0 && gap >= eachBucket) {
                segments[index++] = sym;
                cumBucketIdeal = index * eachBucket;
            }
        }

        segments[segments.length - 1] = eofSig;
//        for (int i = 0; i < nBuckets; i++) {
//            
//        }
//        System.out.println(Arrays.toString(totalFreq));
//        System.out.println(Arrays.toString(segments));
        return new AdaptiveCustomRangeFrequencyTable(nSymbols, eofSig, segments, 2);
    }

    public int[] getRanges() {
        return ranges;
    }

    public int getCustomMethodFlag() {
        return customMethodFlag;
    }

    @Override
    protected int findTableIndex(int symbol) {
        if (symbol < ranges[0]) return 0;
        if (symbol >= ranges[ranges.length - 2]) return ranges.length - 1;
        int low = 0;
        int high = ranges.length - 1;

        while (low < high) {
            int mid = (low + high) >>> 1;
            if (mid == 0) return 0;
            int rangeLow = ranges[mid - 1];
            int rangeHigh = ranges[mid];
            if (symbol < rangeLow) {
                high = mid;
            } else if (symbol >= rangeHigh) {
                low = mid;
            } else {
                return mid;
            }
        }
        throw new IllegalArgumentException("Value out of range: " + symbol);
    }
}
