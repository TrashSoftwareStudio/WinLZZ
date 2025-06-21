package trashsoftware.winBwz.rangeCodec;

import java.util.Arrays;

import static trashsoftware.winBwz.rangeCodec.RangeCodingConstants.MAX_FREQ;

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
}
