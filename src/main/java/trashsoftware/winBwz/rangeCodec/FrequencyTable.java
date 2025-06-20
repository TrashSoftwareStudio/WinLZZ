package trashsoftware.winBwz.rangeCodec;

import java.util.Arrays;

import static trashsoftware.winBwz.rangeCodec.RangeCodingConstants.MAX_FREQ;

// ===================== Frequency Table =====================
public class FrequencyTable {

    final int nSymbol;
//    static final int MAX_FREQ = 8192;  // 用于标准化频率，避免cunFreq溢出2^32

    int[] freq;
    int[] cumFreq;

    public FrequencyTable(int nSymbols, byte[] data) {
        this.nSymbol = nSymbols;  // 需包含EOF，比如处理正常byte的就得257

        freq = new int[nSymbols];
        cumFreq = new int[nSymbols + 1];

        for (byte b : data) {
            freq[b & 0xFF]++;
        }
        freq[256] = 1;  // EOF sentinel

        // Normalize frequencies to avoid overflows
        int total = Arrays.stream(freq).sum();
        double scale = RangeCodingConstants.MAX_FREQ / (double) total;
        for (int i = 0; i < freq.length; i++) {
            if (freq[i] > 0) {
                freq[i] = Math.max(1, (int) (freq[i] * scale));  // scale only if used
            }
        }

        // Build cumulative frequency table
        cumFreq[0] = 0;
        for (int i = 0; i < freq.length; i++) {
            cumFreq[i + 1] = cumFreq[i] + freq[i];
        }
    }

    public int getSymbolLow(int symbol) {
        return cumFreq[symbol];
    }

    public int getSymbolHigh(int symbol) {
        return cumFreq[symbol + 1];
    }

    public int getTotal() {
        return cumFreq[nSymbol];  // total freq
    }

    @Override
    public String toString() {
        return Arrays.toString(freq) + "\n" + Arrays.toString(cumFreq);
    }

//    public int getSymbolFromValue(int value) {
//        for (int i = 0; i < 257; i++) {
//            if (cumFreq[i + 1] > value)
//                return i;
//        }
//        throw new RuntimeException("Value " + value + " out of range");
//    }

    public int getSymbolFromValue(int value) {
        int low = 0;
        int high = freq.length - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            int symLow = cumFreq[mid];
            int symHigh = cumFreq[mid + 1];

            if (value < symLow) {
                high = mid - 1;
            } else if (value >= symHigh) {
                low = mid + 1;
            } else {
                return mid;
            }
        }
        throw new IllegalArgumentException("Value out of range: " + value);
    }
}
