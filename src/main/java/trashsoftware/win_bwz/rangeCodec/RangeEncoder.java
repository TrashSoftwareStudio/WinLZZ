package trashsoftware.win_bwz.rangeCodec;

import java.util.Arrays;

public class RangeEncoder {

    private byte[] input;
    private int alphabetSize;
    private int[] freqTable;
    private final static long totalWidth = 100000;
    private final static long minimumRange = 1000;
    private long range = totalWidth;
    private long low = 0;
    private long[] rangeStarts;
    private long[] rangeWidths;

    public RangeEncoder(byte[] input, int alphabetSize) {
        this.input = input;
        this.alphabetSize = alphabetSize;
        freqTable = new int[alphabetSize + 1];  // endSig
        rangeStarts = new long[alphabetSize + 1];
        rangeWidths = new long[alphabetSize + 1];
    }

    private void calculateFreq() {
        for (byte b : input) {
            freqTable[b & 0xff] += 1;
        }
        freqTable[alphabetSize] = 1;  // endSig
    }

    private void calculateRange() {
        int inputLen = input.length + 1;
        long current = 0;
        for (int i = 0; i < alphabetSize + 1; ++i) {
            double ratio = (double) freqTable[i] / inputLen;
            long width = (long) (ratio * totalWidth);
            rangeStarts[i] = current;
            rangeWidths[i] = width;
            current += width;
        }
    }

    private void emit() {
        System.out.println(low / 10000);
        low = (low % 10000) * 10;
        range *= 10;
    }

    private void encode() {
        byte[] out = new byte[input.length + 1];
        int outIndex = 0;
        for (int i = 0; i < input.length; ++i) {
            int in = input[i] & 0xff;
            long symbolBegin = rangeStarts[in];
            long symbolWidth = rangeWidths[in];
            long newRange = range * symbolWidth / totalWidth;
            long lowInRange = symbolBegin * range / totalWidth;
            low += lowInRange;
            range = newRange;

            System.out.println(low + " " + (range + low));
            while (low / 10000 != (range + low) / 10000) {  // 第一位不一样了
                System.out.println(low / 10000);
                low = (low % 10000) * 10;
                range *= 10;
            }

//            if (range < minimumRange) {
//                range = totalWidth - low;
//            }
        }


    }

    public void compress() {
        calculateFreq();
        calculateRange();
        System.out.println(Arrays.toString(freqTable));
        System.out.println(Arrays.toString(rangeStarts));
        System.out.println(Arrays.toString(rangeWidths));
        encode();
    }

    public static void main(String[] args) {
        byte[] text = {0, 0, 1, 0};
        RangeEncoder re = new RangeEncoder(text, 2);
        re.compress();
    }
}
