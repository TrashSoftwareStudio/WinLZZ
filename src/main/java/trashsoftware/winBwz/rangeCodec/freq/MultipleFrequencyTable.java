package trashsoftware.winBwz.rangeCodec.freq;

public abstract class MultipleFrequencyTable<T extends FrequencyTable> extends FrequencyTable {

    protected final T[] tables;
    
    @SuppressWarnings("unchecked")
    public MultipleFrequencyTable(int nSymbols, int nTables, Class<T> tableClass) {
        super(nSymbols);

        tables = (T[]) new FrequencyTable[nTables];
        for (int i = 0; i < nTables; i++) {
            FrequencyTable ft;
            if (tableClass == AdaptiveFrequencyTable.class) {
                ft = new AdaptiveFrequencyTable(nSymbols);
            } else if (tableClass == StaticFrequencyTable.class) {
                ft = new StaticFrequencyTable(nSymbols);
            } else {
                throw new IllegalArgumentException("Unknown frequency table class: " + tableClass.getName());
            }
            tables[i] = (T) ft;
        }
    }
    
    protected abstract int nextSymbolIndex();
    
    protected abstract void resetContext();

    @Override
    public int getSymbolLow(int symbol) {
        return tables[nextSymbolIndex()].getSymbolLow(symbol);
    }

    @Override
    public int getSymbolHigh(int symbol) {
        return tables[nextSymbolIndex()].getSymbolHigh(symbol);
    }

    @Override
    public int getTotal() {
        return tables[nextSymbolIndex()].getTotal();
    }

    @Override
    public int getSymbolFromValue(int value) {
        return tables[nextSymbolIndex()].getSymbolFromValue(value);
    }

    @Override
    public final void reset() {
        resetContext();
        for (T table : tables) table.reset();
    }

    @Override
    public void printStats() {
        StringBuilder builder = new StringBuilder()
                .append("MulFreqTable")
                .append('[');
        for (T table : tables) {
            builder.append(table.getTotal()).append(", ");
        }
        builder.append(']');
        System.out.println(builder);
    }
}
