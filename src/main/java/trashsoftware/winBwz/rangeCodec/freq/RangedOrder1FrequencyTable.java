package trashsoftware.winBwz.rangeCodec.freq;

public abstract class RangedOrder1FrequencyTable<T extends FrequencyTable> extends MultipleFrequencyTable<T> {

    protected final int eofSig;
    protected int lastSymbol;
    
    public RangedOrder1FrequencyTable(int nSymbols, int eofSig, int nTables, Class<T> tableClass) {
        super(nSymbols, nTables, tableClass);

        this.eofSig = eofSig;
    }

    protected abstract int findTableIndex(int symbol);

    @Override
    protected int nextSymbolIndex() {
        return findTableIndex(lastSymbol);
    }

    @Override
    public void increment(int symbol) {
        tables[findTableIndex(lastSymbol)].increment(symbol);
        lastSymbol = symbol;
    }

    @Override
    protected void resetContext() {
        lastSymbol = eofSig;
    }
}
