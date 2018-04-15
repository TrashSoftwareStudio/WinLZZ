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

    private static HashMap<Short, Integer> generateLengthCode(byte[] canonicalMap) {
        HashMap<Short, Integer> lengthCode = new HashMap<>();
        for (int i = 0; i < canonicalMap.length; i++) {
            int len = canonicalMap[i] & 0xff;
            if (len != 0) {
                lengthCode.put((short) i, len);
            }
        }
        return lengthCode;
    }

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

    public byte[] Compress() {
        return compressText();
    }

    public byte[] Compress(byte[] anotherMap) {
        HashMap<Short, Integer> lengthCode = generateLengthCode(anotherMap);
        huffmanCode = LongHuffmanUtil.generateCanonicalCode(lengthCode);
        return compressText();
    }

    /**
     * Returns true iff the given canonical map contains all symbol of this huffman compressor.
     *
     * @param anotherMap the map to be compare.
     * @return whether anotherMap contains all symbol needed for this compression.
     */
    public boolean compareMap(byte[] anotherMap) {
        for (Short s : huffmanCode.keySet()) {
            if (anotherMap[s] == 0) return false;
        }
        return true;
    }

    public long calculateExpectLength(byte[] codeLengthMap) {
        long aftLen = 0;
        for (int i = 0; i < codeLengthMap.length; i++) {
            Integer freq = freqMap.get((short) i);
            if (freq != null) {
                aftLen += freq * (codeLengthMap[i] & 0xff);
            }
        }
        return aftLen;
    }
}
