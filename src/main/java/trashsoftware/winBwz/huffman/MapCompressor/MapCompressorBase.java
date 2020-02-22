package trashsoftware.winBwz.huffman.MapCompressor;

import trashsoftware.winBwz.huffman.HuffmanCompressor;
import trashsoftware.winBwz.longHuffman.HuffmanNode;
import trashsoftware.winBwz.longHuffman.LongHuffmanUtil;

public abstract class MapCompressorBase {

    protected int[] map;

    protected int compressText(byte[] result, int[] codeTable, int[] lengthTable, int bits, int bitPos, int resIndex) {
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

        return resIndex;
    }

    protected static byte[] generateMap(int[] lengthMap, int alphabetSize) {
        byte[] cmpMap = new byte[alphabetSize];
        for (int i = 0; i < alphabetSize; ++i) {
            int len = lengthMap[i];
            cmpMap[i] = (byte) len;
        }
        return cmpMap;
    }

    protected byte[] cclTruncate(byte[] ccl, int alphabetSize) {
        int i = alphabetSize - 1;
        while (ccl[i] == (byte) 0) i -= 1;
        i += 1;
        byte[] shortCCL = new byte[i];
        System.arraycopy(ccl, 0, shortCCL, 0, i);
        return shortCCL;
    }

    protected abstract int getMaxHeight();

    protected abstract int getEachCclLength();

    protected abstract int[] getInitBitsBitPos(int cclLen, int lengthRemainder);

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
        LongHuffmanUtil.heightControl(codeLengthMap, freqMap, getMaxHeight());
        int[] huffmanCode = LongHuffmanUtil.generateCanonicalCode(codeLengthMap);

        byte[] origCCL = generateMap(codeLengthMap, alphabetSize);
        byte[] CCL = cclTruncate(origCCL, alphabetSize);

//        int hcLen = CCL.length;  // ranged from 1 to 32, 5 bits
        byte[] temp = new byte[map.length + 256];

        int[] initBitsPos = getInitBitsBitPos(CCL.length,map.length % 8);
        int bits = initBitsPos[0];
        int bitPos = initBitsPos[1];
        int outIndex = 0;

        int eachLen = getEachCclLength();

        for (byte b : CCL) {
            bits <<= eachLen;
            bits |= b;
            bitPos += eachLen;
            if (bitPos >= 8) {
                bitPos -= 8;
                temp[outIndex++] = (byte) (bits >> bitPos);
            }
        }

        outIndex = compressText(temp, huffmanCode, codeLengthMap, bits, bitPos, outIndex);

        byte[] result = new byte[outIndex];
        System.arraycopy(temp, 0, result, 0, outIndex);

        return result;
    }
}
