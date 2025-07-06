package trashsoftware.winBwz.rangeCodec.freq;

// ===================== Frequency Table =====================
public abstract class FrequencyTable {

    protected final int nSymbol;

    public FrequencyTable(int nSymbols) {
        this.nSymbol = nSymbols;  // 需包含EOF，比如处理正常byte的就得257
    }

    public abstract int getSymbolLow(int symbol);

    public abstract int getSymbolHigh(int symbol);

    public abstract int getTotal();

    public abstract int getSymbolFromValue(int value);

    public abstract void increment(int symbol);

    public abstract void reset();
    
    public void printStats() {
        // do nothing
    }
}
