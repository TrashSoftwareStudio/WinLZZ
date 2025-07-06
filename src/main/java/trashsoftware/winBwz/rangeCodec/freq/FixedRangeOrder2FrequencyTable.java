package trashsoftware.winBwz.rangeCodec.freq;

public class FixedRangeOrder2FrequencyTable extends RangedOrder2FrequencyTable<AdaptiveFrequencyTable> {
    
    protected int bucketSize;
    
    public FixedRangeOrder2FrequencyTable(int nSymbols, int eofSig, int bucketSize) {
        super(nSymbols, eofSig, (nSymbols * nSymbols + (bucketSize - 1)) / bucketSize, AdaptiveFrequencyTable.class);
        
        this.bucketSize = bucketSize;
    }

    @Override
    protected int findTableIndex(int twoSymbols) {
        return twoSymbols / bucketSize;
    }
}
