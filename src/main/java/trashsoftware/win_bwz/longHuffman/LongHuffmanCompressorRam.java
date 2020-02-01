package trashsoftware.win_bwz.longHuffman;

/**
 * A huffman compression program that all operations take places in the random access memory.
 * <p>
 * This compressor takes an alphabet that has size at most 32768.
 *
 * @author zbh
 * @since 0.5
 */
public class LongHuffmanCompressorRam {

    private int[] fullText;
    private int textBegin;
    private int textSize;
    private int alphabetSize;
    private int[] freqTable;
    private int[] codeTable;
    private int[] lengthTable;

    /**
     * The maximum height (depth) of the huffman tree.
     */
    private int maxHeight = 15;

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
     * @param text         the text to be compressed.
     * @param textBegin    the beginning index in <code>text</code> to be compressed
     * @param textSize     the length of text to be compressed
     * @param alphabetSize the alphabet size, with endSig and other included
     * @param endSig       the mark of the end of stream.
     */
    public LongHuffmanCompressorRam(int[] text, int textBegin, int textSize, int alphabetSize, int endSig) {
        this.fullText = text;
        this.textBegin = textBegin;
        this.textSize = textSize;
        this.endSig = endSig;
        this.alphabetSize = alphabetSize;
    }

    private void generateFreqMap() {
        LongHuffmanUtil.addArrayToFreqMap(fullText, freqTable, textBegin, textSize);
        freqTable[endSig] = 1;
    }

    private byte[] compressText() {
        byte[] out = new byte[textSize + 256];  // assume the compression result will not exceed the orig len + 256
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
        this.maxHeight = height;
    }

    /**
     * Returns the canonical huffman code in given length.
     *
     * @param length the length of the returning canonical map.
     * @return the canonical huffman map.
     */
    public byte[] getMap(int length) {

        this.freqTable = new int[alphabetSize];
        this.lengthTable = new int[alphabetSize];

        generateFreqMap();
        HuffmanNode rootNode = LongHuffmanUtil.generateHuffmanTree(freqTable);
        LongHuffmanUtil.generateCodeLengthMap(lengthTable, rootNode, 0);

        LongHuffmanUtil.heightControl(lengthTable, freqTable, maxHeight);
        codeTable = LongHuffmanUtil.generateCanonicalCode(lengthTable);
        byte[] result = new byte[length];
        System.arraycopy(
                LongHuffmanUtil.generateCanonicalCodeBlock(lengthTable, alphabetSize),
                0,
                result,
                0,
                length);
        return result;
    }

    /**
     * Returns the compressed text using the native huffman code of this {@code LongHuffmanCompressorRam}.
     *
     * @return the compressed text.
     */
    public byte[] compress() {
        return compressText();
    }

    /**
     * Returns the compressed text using the given huffman code of this {@code LongHuffmanCompressorRam}.
     *
     * @param anotherMap the canonical huffman code uses for creating another huffman map for compressing.
     * @return the compressed text.
     */
    public byte[] compress(byte[] anotherMap) {
        freqTable = new int[alphabetSize];
        lengthTable = new int[alphabetSize];
        LongHuffmanUtil.generateLengthCode(anotherMap, lengthTable);
        codeTable = LongHuffmanUtil.generateCanonicalCode(lengthTable);
        return compressText();
    }


    /**
     * Returns the expected length using the given canonical huffman map to encode.
     * <p>
     * Returns -1 if the given map does not contain all symbol needed.
     *
     * @param codeLengthMap the canonical huffman map to be used to encode.
     * @return the expected total code length using this map if the map contains all symbols needed, otherwise -1.
     */
    public long calculateExpectLength(byte[] codeLengthMap) {
        long aftLen = 0;
        for (int i = 0; i < codeLengthMap.length; i++) {
            int freq = freqTable[i];
            if (freq > 0) {
                if (codeLengthMap[i] == 0) return -1;
                aftLen += freq * (codeLengthMap[i] & 0xff);
            }
        }
        return aftLen;
    }
}
