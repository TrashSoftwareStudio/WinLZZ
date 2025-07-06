package trashsoftware.winBwz.rangeCodec.freq;

public class FixedRangeFrequencyTable extends RangedOrder1FrequencyTable<AdaptiveFrequencyTable> {
    
    protected final int bucketSize;
    
    public FixedRangeFrequencyTable(int nSymbols, int eofSig, int bucketSize) {
        super(nSymbols, eofSig, (nSymbols + (bucketSize - 1)) / bucketSize, AdaptiveFrequencyTable.class);
        
        this.bucketSize = bucketSize;
    }

    @Override
    protected int findTableIndex(int symbol) {
        return symbol / bucketSize;
    }
}
