package trashsoftware.win_bwz.huffman.MapCompressor;

import trashsoftware.win_bwz.huffman.HuffmanCompressor;
import trashsoftware.win_bwz.longHuffman.HuffmanNode;
import trashsoftware.win_bwz.longHuffman.LongHuffmanUtil;

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
public class MapCompressor {

    private int[] map;
    private final static int MAX_HEIGHT = 7;
    static final int[] positions = new int[]{16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15};
//    private int[] freqMap;
    private int lengthRemainder;

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

    private static byte[] generateMap(int[] lengthMap) {
        byte[] cmpMap = new byte[19];
        for (int i = 0; i < 19; ++i) {
            int len = lengthMap[i];
            cmpMap[i] = (byte) len;
        }
        return cmpMap;
    }

    private int compressText(byte[] result, int[] codeTable, int[] lengthTable, int bits, int bitPos, int resIndex) {
        int beginBitPos = bitPos;
        for (int value : map) {
            int codeLen = lengthTable[value];
            int code = codeTable[value];
            if (codeLen == 0) throw new RuntimeException();
            bits <<= codeLen;
            bits |= code;
            bitPos += codeLen;

            while (bitPos >= 8) {
                bitPos -= 8;
                result[resIndex++] = (byte) (bits >> bitPos);
            }
        }

        if (bitPos > 0) {
            bits <<= (8 - bitPos);
            result[resIndex++] = (byte) bits;
        }
        lengthRemainder = (bitPos - beginBitPos) % 8;
        if (lengthRemainder < 0) lengthRemainder += 8;

        return resIndex;
    }

    private byte[] swapCcl(byte[] ccl) {
        byte[] cclResult = new byte[19];
        for (int i = 0; i < 19; i++) cclResult[i] = ccl[positions[i]];
        return cclResult;
    }

    private byte[] cclTruncate(byte[] ccl) {
        int i = 18;
        while (ccl[i] == (byte) 0) i -= 1;
        i += 1;
        byte[] shortCCL = new byte[i];
        System.arraycopy(ccl, 0, shortCCL, 0, i);
        return shortCCL;
    }

    /**
     * Returns the compressed map.
     *
     * @param swap whether to swap unusual code length to the front and back of the ccl sequence.
     * @return the compressed map.
     */
    public byte[] Compress(boolean swap) {
        int[] freqMap = new int[19];
        HuffmanCompressor.addArrayToFreqMap(map, freqMap, map.length);
        int[] codeLengthMap = new int[19];
        HuffmanNode rootNode = LongHuffmanUtil.generateHuffmanTree(freqMap);
        LongHuffmanUtil.generateCodeLengthMap(codeLengthMap, rootNode, 0);
        LongHuffmanUtil.heightControl(codeLengthMap, freqMap, MAX_HEIGHT);
        int[] huffmanCode = LongHuffmanUtil.generateCanonicalCode(codeLengthMap);

//        System.out.println(Arrays.toString(freqMap));
//        System.out.println(Arrays.toString(huffmanCode));

        byte[] origCCL = generateMap(codeLengthMap);
        byte[] CCL;
        if (swap) {
            byte[] swappedCCL = swapCcl(origCCL);
            CCL = cclTruncate(swappedCCL);
        } else {
            CCL = cclTruncate(origCCL);
        }

        int hcLen = CCL.length - 4;
        byte[] temp = new byte[map.length + 256];

        int outIndex = 0;
        int bits = hcLen << 3;
        int bitPos = 7;

        for (byte b : CCL) {
            bits <<= 3;
            bits |= b;
            bitPos += 3;
            if (bitPos >= 8) {
                bitPos -= 8;
                temp[outIndex++] = (byte) (bits >> bitPos);
            }
        }

        outIndex = compressText(temp, huffmanCode, codeLengthMap, bits, bitPos, outIndex);
        temp[0] = (byte) ((temp[0] & 0xff) | (lengthRemainder << 1));

        byte[] result = new byte[outIndex];
        System.arraycopy(temp, 0, result, 0, outIndex);

//        System.out.println(Arrays.toString(map));
        return result;
    }
}
