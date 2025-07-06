package trashsoftware.winBwz.rangeCodec.freq;

public class AdaptiveOrder1FrequencyTable extends RangedOrder1FrequencyTable<AdaptiveFrequencyTable> {
    
    public AdaptiveOrder1FrequencyTable(int nSymbols, int eofSig) {
        super(nSymbols, eofSig, nSymbols, AdaptiveFrequencyTable.class);
    }

    @Override
    protected int findTableIndex(int symbol) {
        return symbol;
    }

    public static int estimatedMemoryUsage(int symbolLimit) {
        return AdaptiveFrequencyTable.estimatedMemoryUsage(symbolLimit) * symbolLimit + 20;
    }
}
