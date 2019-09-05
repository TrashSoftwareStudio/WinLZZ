package trashsoftware.win_bwz.longHuffman;

import trashsoftware.win_bwz.utility.Bytes;

import java.util.HashMap;

/**
 * A huffman compression program that all operations take places in the random access memory.
 * <p>
 * This compressor takes an alphabet that has size at most 32768.
 *
 * @author zbh
 * @since 0.5
 */
public class LongHuffmanCompressorRam {

    private int[] text;
    private int alphabetSize;
    private HashMap<Integer, Integer> freqMap = new HashMap<>();
    private HashMap<Integer, String> huffmanCode = new HashMap<>();

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
     * @param alphabetSize the alphabet size, with endSig and other included
     * @param endSig       the mark of the end of stream.
     */
    public LongHuffmanCompressorRam(int[] text, int alphabetSize, int endSig) {
        this.text = text;
        this.endSig = endSig;
        this.alphabetSize = alphabetSize;
    }

    private void generateFreqMap() {
        LongHuffmanUtil.addArrayToFreqMap(text, freqMap, text.length);
        freqMap.put(endSig, 1);
    }

    private byte[] compressText() {
        StringBuilder builder = new StringBuilder();
        LongHuffmanUtil.addCompressed(text, text.length, builder, huffmanCode);
        builder.append(huffmanCode.get(endSig));
        return Bytes.stringBuilderToBytesFull(builder);
    }

//    private byte[] compressText2() {
//        ByteArrayOutputStream os = new ByteArrayOutputStream();
//        LongHuffmanUtil.addCompressed(text, text.length, os, huffmanCode, endSig);
//        return os.toByteArray();
//    }

    /**
     * Sets up the {@code maxHeight} value which limits the max depth of the huffman tree.
     *
     * @param height the tree-height limit.
     */
    @SuppressWarnings("all")
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
        generateFreqMap();
//        System.out.println(freqMap);
        HuffmanNode rootNode = LongHuffmanUtil.generateHuffmanTree(freqMap);
        HashMap<Integer, Integer> codeLengthMap = new HashMap<>();
        LongHuffmanUtil.generateCodeLengthMap(codeLengthMap, rootNode, 0);
//        System.out.println(codeLengthMap);

        LongHuffmanUtil.heightControl(codeLengthMap, freqMap, maxHeight);
        huffmanCode = LongHuffmanUtil.generateCanonicalCode(codeLengthMap);
        byte[] result = new byte[length];
        System.arraycopy(LongHuffmanUtil.generateCanonicalCodeBlock(codeLengthMap, alphabetSize), 0, result, 0, length);
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
        HashMap<Integer, Integer> lengthCode = LongHuffmanUtil.generateLengthCode(anotherMap);
        huffmanCode = LongHuffmanUtil.generateCanonicalCode(lengthCode);
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
            Integer freq = freqMap.get(i);
            if (freq != null) {
                if (codeLengthMap[i] == 0) return -1;
                aftLen += freq * (codeLengthMap[i] & 0xff);
            }
        }
        return aftLen;
    }
}
