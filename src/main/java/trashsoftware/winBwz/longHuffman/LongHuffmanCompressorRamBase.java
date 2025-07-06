package trashsoftware.winBwz.longHuffman;

public abstract class LongHuffmanCompressorRamBase {

    protected final int[] fullText;
    protected int textBegin;
    protected int textSize;
    protected final int alphabetSize;

    /**
     * The maximum height (depth) of the huffman tree.
     */
    protected static int maxHeight = 29;  // map alphabet size: 30

    /**
     * The signal that marks the EOF
     */
    protected final int endSig;

    /**
     * Creates a new {@code LongHuffmanCompressorRam} instance.
     * <p>
     * LongHuffmanCompressor deals text from range 0 to 32767.
     * This compressor works completely in random access memory.
     *
     * @param fullText     the total text
     * @param alphabetSize the alphabet size, with endSig and other included
     * @param endSig       the mark of the end of stream.
     */
    public LongHuffmanCompressorRamBase(int[] fullText, int alphabetSize, int endSig) {
        this.fullText = fullText;
        this.endSig = endSig;
        this.alphabetSize = alphabetSize;
    }

    public abstract byte[] compress();

    /**
     * Sets up the {@code maxHeight} value which limits the max depth of the huffman tree.
     *
     * @param height the tree-height limit.
     */
    public void setMaxHeight(int height) {
        maxHeight = height;
    }
}
