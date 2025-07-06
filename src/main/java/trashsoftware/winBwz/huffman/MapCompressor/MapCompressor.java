package trashsoftware.winBwz.huffman.MapCompressor;

/**
 * A compressor that compresses code lengths of a canonical huffman table.
 * <p>
 * The input table is created by compresses a canonical huffman table again using huffman algorithm, the code lengths
 * of the original canonical huffman table becomes the codes of the second canonical huffman table.
 * Typically it takes code lengths from 0 to 15 inclusive.
 *
 * @author zbh
 * @since 0.4
 */
public class MapCompressor extends MapCompressorBase {

//    static final int[] positions = new int[]{16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15};

    /**
     * Creates a new {@code MapCompressor} instance.
     * <p>
     * A huffman-based compressor, used to compress canonical huffman maps that have code lengths at most 15.
     *
     * @param mapBytes the content to be compressed.
     */
    public MapCompressor(int[] mapBytes) {
        this.map = mapBytes;
    }

    @Override
    protected int getMaxHeight() {
        return 7;
    }

    @Override
    protected int getEachCclLength() {
        return 3;
    }

    @Override
    protected int[] getInitBitsBitPos(int cclLen, int lengthRemainder) {
        return new int[]{((cclLen - 4) << 3) | lengthRemainder, 7};
    }
}
