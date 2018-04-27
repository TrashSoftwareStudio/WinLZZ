package WinLzz.Huffman;

class HuffmanTuple implements Comparable<HuffmanTuple> {

    private byte value;

    private int codeLength;

    HuffmanTuple(byte value, int codeLength) {
        this.value = value;
        this.codeLength = codeLength;
    }

    public int getLength() {
        return codeLength;
    }

    byte getValue() {
        return value;
    }

    @Override
    public int compareTo(HuffmanTuple o) {
        int lengthCmp = Integer.compare(codeLength, o.codeLength);
        if (lengthCmp == 0) return Byte.compare(value, o.value);
        else return lengthCmp;
    }
}
