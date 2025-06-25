package trashsoftware.winBwz.core.options;

public class LZZ2Options extends LZOptions {
    
    protected final EntropyMethod entropyMethod;
    
    public LZZ2Options(int windowSize, int labSize, EntropyMethod entropyMethod) {
        super(windowSize, labSize);
        
        this.entropyMethod = entropyMethod;
    }

    public EntropyMethod getEntropyMethod() {
        return entropyMethod;
    }

    /**
     * @param windowSize window size
     * @param labSize    look ahead buffer size
     * @return the lzz2 option before version 28, inclusive.
     */
    public static LZZ2Options oldDefault(int windowSize, int labSize) {
        return new LZZ2Options(windowSize, labSize, EntropyMethod.FULL_HUFFMAN);
    }

    /**
     * @param windowSize window size
     * @param labSize    look ahead buffer size
     * @return the BWZ option >= version 29, inclusive.
     */
    public static LZZ2Options newDefault(int windowSize, int labSize) {
        return new LZZ2Options(windowSize, labSize, EntropyMethod.FULL_HUFFMAN);
    }

    public static LZZ2Options createFromByte(int windowSize, int labSize, byte byteRep) {
        int rep = byteRep & 0xff;
        int entropyIndex = rep & 0x0f;
        EntropyMethod em = EntropyMethod.values()[entropyIndex];
        return new LZZ2Options(windowSize, labSize, em);
    }

    public byte oneByteRep() {
        int b = entropyMethod.ordinal();
        // [0-3] entropy ordinal
        // [4-7] reserved for now
        return (byte) b;
    }
}
