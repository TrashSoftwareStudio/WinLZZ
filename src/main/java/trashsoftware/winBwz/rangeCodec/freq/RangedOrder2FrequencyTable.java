package trashsoftware.winBwz.rangeCodec.freq;

public abstract class RangedOrder2FrequencyTable<T extends FrequencyTable> extends MultipleFrequencyTable<T> {
    
    protected final int eofSig;
    protected int lastTwoSymbol;
    
    public RangedOrder2FrequencyTable(int nSymbols, int eofSig, int nTables, Class<T> tableClass) {
        super(nSymbols, nTables, tableClass);
        
        this.eofSig = eofSig;
        
        lastTwoSymbol = eofSig;
    }
    
    protected abstract int findTableIndex(int twoSymbols);

    @Override
    protected int nextSymbolIndex() {
        return findTableIndex(lastTwoSymbol);
    }

    @Override
    public void increment(int symbol) {
        tables[findTableIndex(lastTwoSymbol)].increment(symbol);
        
        // fixme: this version only works for 256 alphabet size
        lastTwoSymbol <<= 8;
        lastTwoSymbol |= symbol;
        lastTwoSymbol &= 0xffff;
    }

    @Override
    protected void resetContext() {
        lastTwoSymbol = eofSig;
    }
}
