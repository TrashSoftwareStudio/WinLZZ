package trashsoftware.winBwz.core.options;

public class BWZOptions extends AlgOptions {

    protected EntropyMethod entropyMethod;

    public BWZOptions(int windowSize, EntropyMethod entropyMethod) {
        super(windowSize);

        this.entropyMethod = entropyMethod;
    }

    /**
     * @param windowSize window size
     * @return the BWZ option before version 28, inclusive.
     */
    public static BWZOptions oldDefault(int windowSize) {
        return new BWZOptions(windowSize, EntropyMethod.BLOCK_HUFFMAN);
    }

    /**
     * @param windowSize window size
     * @return the BWZ option >= version 29, inclusive.
     */
    public static BWZOptions newDefault(int windowSize) {
        return new BWZOptions(windowSize, EntropyMethod.ADAPTIVE_RANGE);
    }

    public static BWZOptions createFromByte(int windowSize, byte byteRep) {
        int rep = byteRep & 0xff;
        int entropyIndex = rep & 0x0f;
        EntropyMethod em = EntropyMethod.values()[entropyIndex];
        return new BWZOptions(windowSize, em);
    }

    public byte oneByteRep() {
        int b = entropyMethod.ordinal();
        // [0-3] entropy ordinal
        // [4-7] reserved for now
        return (byte) b;
    }

    public EntropyMethod getEntropyMethod() {
        return entropyMethod;
    }

    public enum EntropyMethod {
        BLOCK_HUFFMAN,
        STATIC_RANGE,
        ADAPTIVE_RANGE
    }
}
