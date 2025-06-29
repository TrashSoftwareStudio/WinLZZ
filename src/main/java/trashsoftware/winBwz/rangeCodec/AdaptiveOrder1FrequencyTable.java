package trashsoftware.winBwz.rangeCodec;

public class AdaptiveOrder1FrequencyTable extends FrequencyTable {
    
    protected final AdaptiveFrequencyTable[] tables;
    
    private final int eofSig;
    private int lastSymbol;
    
    public AdaptiveOrder1FrequencyTable(int nSymbols, int eofSig) {
        super(nSymbols);
        
        this.eofSig = eofSig;
        tables = new AdaptiveFrequencyTable[nSymbols];
        for (int i = 0; i < nSymbols; i++) {
            tables[i] = new AdaptiveFrequencyTable(nSymbols);
        }
        lastSymbol = eofSig;
    }

    public static int estimatedMemoryUsage(int symbolLimit) {
        return AdaptiveFrequencyTable.estimatedMemoryUsage(symbolLimit) * symbolLimit + 20;
    }

    @Override
    public int getSymbolLow(int symbol) {
        return tables[lastSymbol].getSymbolLow(symbol);
    }

    @Override
    public int getSymbolHigh(int symbol) {
        return tables[lastSymbol].getSymbolHigh(symbol);
    }

    @Override
    public int getTotal() {
        return tables[lastSymbol].getTotal();
    }

    @Override
    public int getSymbolFromValue(int value) {
        return tables[lastSymbol].getSymbolFromValue(value);
    }

    @Override
    public void increment(int symbol) {
        tables[lastSymbol].increment(symbol);
        lastSymbol = symbol;
    }

    @Override
    public void reset() {
        lastSymbol = eofSig;
        for (AdaptiveFrequencyTable table : tables) table.reset();
    }
}
