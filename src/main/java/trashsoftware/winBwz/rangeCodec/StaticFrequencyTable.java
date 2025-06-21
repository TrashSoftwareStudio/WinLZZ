package trashsoftware.winBwz.rangeCodec;

import java.util.Arrays;

public class StaticFrequencyTable extends FrequencyTable {
    final int[] freq;
    final int[] cumFreq;

    public StaticFrequencyTable(int nSymbols, byte[] data) {
        super(nSymbols);

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

    @Override
    public int getSymbolLow(int symbol) {
        return cumFreq[symbol];
    }

    @Override
    public int getSymbolHigh(int symbol) {
        return cumFreq[symbol + 1];
    }

    @Override
    public int getTotal() {
        return cumFreq[nSymbol];  // total freq
    }

    @Override
    public String toString() {
        return Arrays.toString(freq) + "\n" + Arrays.toString(cumFreq);
    }

    @Override
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

    @Override
    public void increment(int symbol) {
        // do nothing
    }
}
