package LongHuffman;

import Utility.Bytes;

import java.util.HashMap;

public class LongHuffmanCompressorRam {

    private short[] text;
    private int alphabetSize;
    private HashMap<Short, Integer> freqMap = new HashMap<>();
    private HashMap<Short, String> huffmanCode = new HashMap<>();
    private final static int maxHeight = 15;
    private short endSig;

    /**
     * Creates a new LongHuffmanCompressorRam instance.
     * <p>
     * LongHuffmanCompressor deals text from range 0 to 32767.
     * This compressor works completely in random access memory.
     *
     * @param text         the text to be compressed.
     * @param alphabetSize the alphabet size.
     * @param endSig       the mark of the end of stream.
     */
    public LongHuffmanCompressorRam(short[] text, int alphabetSize, short endSig) {
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

    /**
     * Returns the canonical huffman code in given length.
     *
     * @param length the length of the returning canonical map.
     * @return the canonical huffman map.
     */
    public byte[] getMap(int length) {
        generateFreqMap();
        HuffmanNode rootNode = LongHuffmanUtil.generateHuffmanTree(freqMap);
        HashMap<Short, Integer> codeLengthMap = new HashMap<>();
        LongHuffmanUtil.generateCodeLengthMap(codeLengthMap, rootNode, 0);

        LongHuffmanUtil.heightControl(codeLengthMap, freqMap, maxHeight);
        huffmanCode = LongHuffmanUtil.generateCanonicalCode(codeLengthMap);
        byte[] result = new byte[length];
        System.arraycopy(LongHuffmanUtil.generateCanonicalCodeBlock(codeLengthMap, alphabetSize), 0, result, 0, length);
        return result;
    }

    /**
     * Returns the compressed text using the native huffman code of this compressor.
     *
     * @return the compressed text.
     */
    public byte[] Compress() {
        return compressText();
    }

    /**
     * Returns the compressed text using the given huffman code of this compressor.
     *
     * @param anotherMap the canonical huffman code uses for creating another huffman map for compressing.
     * @return the compressed text.
     */
    public byte[] Compress(byte[] anotherMap) {
        HashMap<Short, Integer> lengthCode = LongHuffmanUtil.generateLengthCode(anotherMap);
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
            Integer freq = freqMap.get((short) i);
            if (freq != null) {
                if (codeLengthMap[i] == 0) return -1;
                aftLen += freq * (codeLengthMap[i] & 0xff);
            }
        }
        return aftLen;
    }
}
