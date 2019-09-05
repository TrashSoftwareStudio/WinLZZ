package trashsoftware.win_bwz.huffman;

public class LengthTuple implements Comparable<LengthTuple> {

    private byte b;

    public int length;

    private int freq;

    public LengthTuple(byte b, int length, int freq) {
        this.b = b;
        this.length = length;
        this.freq = freq;
    }

    public byte getByte() {
        return b;
    }

    @Override
    public int compareTo(LengthTuple o) {
        return Integer.compare(o.freq, freq);
    }
}

