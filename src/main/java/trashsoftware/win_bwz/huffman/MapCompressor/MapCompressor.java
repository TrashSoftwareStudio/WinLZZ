package trashsoftware.win_bwz.huffman.MapCompressor;

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

//    private byte[] swapCcl(byte[] ccl) {
//        byte[] cclResult = new byte[19];
//        for (int i = 0; i < 19; i++) cclResult[i] = ccl[positions[i]];
//        return cclResult;
//    }

//    /**
//     * Returns the compressed map.
//     *
//     * @param swap whether to swap unusual code length to the front and back of the ccl sequence.
//     * @return the compressed map.
//     */
//    public byte[] Compress(boolean swap) {
//        int[] freqMap = new int[19];
//        HuffmanCompressor.addArrayToFreqMap(map, freqMap, map.length);
//        int[] codeLengthMap = new int[19];
//        HuffmanNode rootNode = LongHuffmanUtil.generateHuffmanTree(freqMap);
//        LongHuffmanUtil.generateCodeLengthMap(codeLengthMap, rootNode, 0);
//        LongHuffmanUtil.heightControl(codeLengthMap, freqMap, MAX_HEIGHT);
//        int[] huffmanCode = LongHuffmanUtil.generateCanonicalCode(codeLengthMap);
//
//        lengthRemainder = map.length % 8;
//
//        byte[] origCCL = generateMap(codeLengthMap, 19);
//        byte[] CCL;
//        if (swap) {
//            byte[] swappedCCL = swapCcl(origCCL);
//            CCL = cclTruncate(swappedCCL, 19);
//        } else {
//            CCL = cclTruncate(origCCL, 19);
//        }
//
//        int hcLen = CCL.length - 4;
//        byte[] temp = new byte[map.length + 256];
//
//        int outIndex = 0;
//        int bits = (hcLen << 3) | lengthRemainder;
//        int bitPos = 7;
//
//        for (byte b : CCL) {
//            bits <<= 3;
//            bits |= b;
//            bitPos += 3;
//            if (bitPos >= 8) {
//                bitPos -= 8;
//                temp[outIndex++] = (byte) (bits >> bitPos);
//            }
//        }
//
//        outIndex = compressText(temp, huffmanCode, codeLengthMap, bits, bitPos, outIndex);
//
//        byte[] result = new byte[outIndex];
//        System.arraycopy(temp, 0, result, 0, outIndex);
//
////        System.out.println(Arrays.toString(map));
//        return result;
//    }
}
