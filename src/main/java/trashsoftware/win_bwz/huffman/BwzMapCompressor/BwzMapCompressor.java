package trashsoftware.win_bwz.huffman.BwzMapCompressor;

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
public class BwzMapCompressor {

    private int[] map;
    private final static int MAX_HEIGHT = 15;
    private int lengthRemainder;

    /**
     * Creates a new {@code MapCompressor} instance.
     * <p>
     * A huffman-based compressor, used to compress canonical huffman maps that have code lengths at most 15.
     *
     * @param mapBytes the content to be compressed.
     */
    public BwzMapCompressor(int[] mapBytes) {
        this.map = mapBytes;
    }

    private static byte[] generateMap(int[] lengthMap, int alphabetSize) {
        byte[] cmpMap = new byte[alphabetSize];
        for (int i = 0; i < alphabetSize; ++i) {
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

    private byte[] cclTruncate(byte[] ccl, int alphabetSize) {
        int i = alphabetSize - 1;
        while (ccl[i] == (byte) 0) i -= 1;
        i += 1;
        byte[] shortCCL = new byte[i];
        System.arraycopy(ccl, 0, shortCCL, 0, i);
        return shortCCL;
    }

    /**
     * Returns the compressed map.
     *
     * @param alphabetSize the size of alphabet. Range is [0, alphabetSize - 1]
     * @return the compressed map.
     */
    public byte[] Compress(int alphabetSize) {
        int[] freqMap = new int[alphabetSize];
        HuffmanCompressor.addArrayToFreqMap(map, freqMap, map.length);
        int[] codeLengthMap = new int[alphabetSize];
        HuffmanNode rootNode = LongHuffmanUtil.generateHuffmanTree(freqMap);
        LongHuffmanUtil.generateCodeLengthMap(codeLengthMap, rootNode, 0);
        LongHuffmanUtil.heightControl(codeLengthMap, freqMap, MAX_HEIGHT);
        int[] huffmanCode = LongHuffmanUtil.generateCanonicalCode(codeLengthMap);

        byte[] origCCL = generateMap(codeLengthMap, alphabetSize);
        byte[] CCL = cclTruncate(origCCL, alphabetSize);

        int hcLen = CCL.length;  // ranged from 1 to 32, 5 bits
        byte[] temp = new byte[map.length + 256];

        int bits = hcLen << 3;  // 3 bit reserved for remainder
        int bitPos = 0;
        temp[0] = (byte) bits;
        int outIndex = 1;

        for (byte b : CCL) {
            bits <<= 4;
            bits |= b;
            bitPos += 4;
            if (bitPos >= 8) {
                bitPos -= 8;
                temp[outIndex++] = (byte) (bits >> bitPos);
            }
        }

        outIndex = compressText(temp, huffmanCode, codeLengthMap, bits, bitPos, outIndex);
        temp[0] = (byte) ((temp[0] & 0xff) | lengthRemainder);

        byte[] result = new byte[outIndex];
        System.arraycopy(temp, 0, result, 0, outIndex);

        return result;
    }
}
