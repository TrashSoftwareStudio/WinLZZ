package trashsoftware.winBwz.longHuffman;

import java.util.Arrays;

/**
 * A huffman compression program that all operations take places in the random access memory.
 * <p>
 * This compressor takes an alphabet that has size at most 32768.
 *
 * @author zbh
 * @since 0.5
 */
public class LongHuffmanCompressorRam {

    private static final int OPTIMAL_BLOCK_SIZE = 16384;
    private static final int ESTIMATE_CMP_MAP_LENGTH = 36;

    private int[] fullText;
    private int textBegin;
    private int textSize;
    private int alphabetSize;
    private int[] codeTable;
    private int[] lengthTable;

    private int[] lastFreqTable;
    private int[] lastLengthTable;

    /**
     * The maximum height (depth) of the huffman tree.
     */
    private static int maxHeight = 29;  // map alphabet size: 30

    /**
     * The signal that marks the
     */
    private int endSig;

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
    public LongHuffmanCompressorRam(int[] fullText, int alphabetSize, int endSig) {
        this.fullText = fullText;
        this.endSig = endSig;
        this.alphabetSize = alphabetSize;
    }

    public byte[] getMap() {
        codeTable = LongHuffmanUtil.generateCanonicalCode(lengthTable);
        return LongHuffmanUtil.generateCanonicalCodeBlock(lengthTable, lengthTable.length);
    }

    private byte[] compressText() {
        byte[] out = new byte[(int) ((double) alphabetSize / 256 * textSize) + 1];  // The max possible result length
        int bits = 0;
        int bitPos = 0;
        int resIndex = 0;
        for (int i = 0; i < textSize; ++i) {
            int value = fullText[textBegin + i];
            int codeLen = lengthTable[value];
            int code = codeTable[value];
            if (codeLen == 0) throw new RuntimeException();
            bits <<= codeLen;
            bits |= code;
            bitPos += codeLen;

            while (bitPos >= 8) {
                bitPos -= 8;
                out[resIndex++] = (byte) (bits >> bitPos);
            }
        }

        int codeLen = lengthTable[endSig];
        int code = codeTable[endSig];
        if (codeLen == 0) throw new RuntimeException();

        bits <<= codeLen;
        bits |= code;
        bitPos += codeLen;

        while (bitPos >= 8) {
            bitPos -= 8;
            out[resIndex++] = (byte) (bits >> bitPos);
        }

        if (bitPos > 0) {
            bits <<= (8 - bitPos);
            out[resIndex++] = (byte) bits;
        }
        byte[] result = new byte[resIndex];
        System.arraycopy(out, 0, result, 0, resIndex);
        return result;
    }

    /**
     * Sets up the {@code maxHeight} value which limits the max depth of the huffman tree.
     *
     * @param height the tree-height limit.
     */
    public void setMaxHeight(int height) {
        maxHeight = height;
    }

//    /**
//     * Returns the canonical huffman code in given length.
//     *
//     * @param length the length of the returning canonical map.
//     * @return the canonical huffman map.
//     */
//    public byte[] getMap(int length) {
//
//        this.freqTable = new int[alphabetSize];
//        this.lengthTable = new int[alphabetSize];
//
//        generateFreqMap();
//        HuffmanNode rootNode = LongHuffmanUtil.generateHuffmanTree(freqTable);
//        LongHuffmanUtil.generateCodeLengthMap(lengthTable, rootNode, 0);
//
//        LongHuffmanUtil.heightControl(lengthTable, freqTable, maxHeight);
//        codeTable = LongHuffmanUtil.generateCanonicalCode(lengthTable);
//        byte[] result = new byte[length];
//        System.arraycopy(
//                LongHuffmanUtil.generateCanonicalCodeBlock(lengthTable, alphabetSize),
//                0,
//                result,
//                0,
//                length);
//        return result;
//    }

    /**
     * Returns the compressed text using the native huffman code of this {@code LongHuffmanCompressorRam}.
     *
     * @return the compressed text.
     */
    public byte[] compress() {
        return compressText();
    }

//    /**
//     * Returns the compressed text using the given huffman code of this {@code LongHuffmanCompressorRam}.
//     *
//     * @param anotherMap the canonical huffman code uses for creating another huffman map for compressing.
//     * @return the compressed text.
//     */
//    public byte[] compress(byte[] anotherMap) {
//        freqTable = new int[alphabetSize];
//        lengthTable = new int[alphabetSize];
//        LongHuffmanUtil.generateLengthCode(anotherMap, lengthTable);
//        codeTable = LongHuffmanUtil.generateCanonicalCode(lengthTable);
//        return compressText();
//    }
//
//
//    /**
//     * Returns the expected length using the given canonical huffman map to encode.
//     * <p>
//     * Returns -1 if the given map does not contain all symbol needed.
//     *
//     * @param codeLengthMap the canonical huffman map to be used to encode.
//     * @return the expected total code length using this map if the map contains all symbols needed, otherwise -1.
//     */
//    public long calculateExpectLength(byte[] codeLengthMap) {
//        long aftLen = 0;
//        for (int i = 0; i < codeLengthMap.length; i++) {
//            int freq = freqTable[i];
//            if (freq > 0) {
//                if (codeLengthMap[i] == 0) return -1;
//                aftLen += freq * (codeLengthMap[i] & 0xff);
//            }
//        }
//        return aftLen;
//    }

    private static int expectLength(int[] codeLengthMap, int[] freqMap) {
        int aftLen = 0;
        for (int i = 0; i < codeLengthMap.length; i++) {
            aftLen += freqMap[i] * codeLengthMap[i];
        }
        return aftLen;
    }

    public int findOptimalLength(int textBegin, int minLength) {
        minLength = textBegin + minLength > fullText.length ?
                fullText.length - textBegin : minLength;  // make sure index not outbound

        int[] freq;
        int[] codeLengths;
        if (lastFreqTable == null || lastLengthTable == null) {
            freq = new int[alphabetSize];
            codeLengths = new int[alphabetSize];
            freq[endSig] = 1;
            LongHuffmanUtil.addArrayToFreqMap(fullText, freq, textBegin, minLength);

            HuffmanNode rootNode = LongHuffmanUtil.generateHuffmanTree(freq);
            LongHuffmanUtil.generateCodeLengthMap(codeLengths, rootNode, 0);

            LongHuffmanUtil.heightControl(codeLengths, freq, maxHeight);
        } else {
            freq = lastFreqTable;
            codeLengths = lastLengthTable;
        }

        int expectLength = expectLength(codeLengths, freq) + ESTIMATE_CMP_MAP_LENGTH;  // tables, etc
        int curLength = minLength;

        int[] mergedFreq = new int[alphabetSize];
        int[] mergedCodeLengths = new int[alphabetSize];
        int[] newPartFreq = new int[alphabetSize];
        int[] newPartCodeLengths = new int[alphabetSize];

        while (textBegin + curLength < fullText.length) {
//            if (true) break;
            int blockEnds = Math.min(textBegin + curLength + OPTIMAL_BLOCK_SIZE, fullText.length);
            int blockSize = blockEnds - curLength - textBegin;

            // Calculate the expected length of new added part only
            Arrays.fill(newPartFreq, 0);
            Arrays.fill(newPartCodeLengths, 0);
            newPartFreq[endSig] = 1;
            LongHuffmanUtil.addArrayToFreqMap(fullText, newPartFreq, textBegin + curLength, blockSize);
            HuffmanNode newPartRootNode = LongHuffmanUtil.generateHuffmanTree(newPartFreq);
            LongHuffmanUtil.generateCodeLengthMap(newPartCodeLengths, newPartRootNode, 0);
            LongHuffmanUtil.heightControl(newPartCodeLengths, newPartFreq, maxHeight);

            int newPartExpectLength = expectLength(newPartCodeLengths, newPartFreq) + ESTIMATE_CMP_MAP_LENGTH;

            // Expected length of merged text
            System.arraycopy(freq, 0, mergedFreq, 0, alphabetSize);
            Arrays.fill(mergedCodeLengths, 0);

            LongHuffmanUtil.addArrayToFreqMap(fullText, mergedFreq, textBegin + curLength, blockSize);
            HuffmanNode mergedRootNode = LongHuffmanUtil.generateHuffmanTree(mergedFreq);
            LongHuffmanUtil.generateCodeLengthMap(mergedCodeLengths, mergedRootNode, 0);
            LongHuffmanUtil.heightControl(mergedCodeLengths, mergedFreq, maxHeight);

            int mergedExpectLength = expectLength(mergedCodeLengths, mergedFreq) + ESTIMATE_CMP_MAP_LENGTH;
            int twoPartsExpectLength = expectLength + newPartExpectLength;

            if (mergedExpectLength > twoPartsExpectLength) {
//                if (curLength != minLength) System.out.print("gg");
                lastFreqTable = newPartFreq;
                lastLengthTable = newPartCodeLengths;
                break;
            } else {
                expectLength = mergedExpectLength;
                curLength += blockSize;
                System.arraycopy(mergedFreq, 0, freq, 0, alphabetSize);
                System.arraycopy(mergedCodeLengths, 0, codeLengths, 0, alphabetSize);
            }
        }
        lengthTable = codeLengths;
//        System.out.println("got: " + ((double) expectLength(lengthTable, freqTable) / curLength / 8));
        this.textBegin = textBegin;
        this.textSize = curLength;

        return curLength;
    }

    public void generateSingleMap() {
        textBegin = 0;
        textSize = fullText.length;
        int[] freq = new int[alphabetSize];
        lengthTable = new int[alphabetSize];
        freq[endSig] = 1;
        LongHuffmanUtil.addArrayToFreqMap(fullText, freq, textBegin, textSize);

        HuffmanNode rootNode = LongHuffmanUtil.generateHuffmanTree(freq);
        LongHuffmanUtil.generateCodeLengthMap(lengthTable, rootNode, 0);

        LongHuffmanUtil.heightControl(lengthTable, freq, maxHeight);
    }
}
