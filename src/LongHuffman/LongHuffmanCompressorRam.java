package LongHuffman;

import Utility.Bytes;

import java.util.HashMap;

public class LongHuffmanCompressorRam {

    private short[] text;

    private int alphabetSize;

    private HashMap<Short, Integer> freqMap = new HashMap<>();

    private HashMap<Short, String> huffmanCode = new HashMap<>();

    private int maxHeight = 15;

    private short endSig;

    public LongHuffmanCompressorRam(short[] text, int alphabetSize, short endSig) {
        this.text = text;
        this.endSig = endSig;
        this.alphabetSize = alphabetSize;
    }

    private void generateFreqMap() {
        LongHuffmanCompressor.addArrayToFreqMap(text, freqMap, text.length);
        freqMap.put(endSig, 1);
    }

    private byte[] compressText() {
        StringBuilder builder = new StringBuilder();
        LongHuffmanCompressor.addCompressed(text, text.length, builder, huffmanCode);
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

    public double calculateAverageCodeLength(byte[] codeLengthMap) {
        long aftLen = 0;
        long origLen = 0;
        for (int i = 0; i < codeLengthMap.length; i++) {
            Integer freq = freqMap.get((short) i);
            if (freq != null) {
                aftLen += freq * (codeLengthMap[i] & 0xff);
                origLen += freq;
            }
        }
        return (double) aftLen / origLen;
    }

    public byte[] getMap(int length) {
        generateFreqMap();
        HuffmanNode rootNode = LongHuffmanCompressor.generateHuffmanTree(freqMap);
        HashMap<Short, Integer> codeLengthMap = new HashMap<>();
        LongHuffmanCompressor.generateCodeLengthMap(codeLengthMap, rootNode, 0);

        LongHuffmanCompressor.heightControl(codeLengthMap, freqMap, maxHeight);
        huffmanCode = LongHuffmanCompressor.generateCanonicalCode(codeLengthMap);
        byte[] result = new byte[length];
        System.arraycopy(LongHuffmanCompressor.generateCanonicalCodeBlock(codeLengthMap, alphabetSize), 0, result, 0, length);
        return result;
    }

    public HashMap<Short, Integer> getFreqMap() {
        return freqMap;
    }

    public byte[] Compress() {
        return compressText();
    }

    public byte[] Compress(byte[] anotherMap) {
        HashMap<Short, Integer> lengthCode = generateLengthCode(anotherMap);
        huffmanCode = LongHuffmanCompressor.generateCanonicalCode(lengthCode);
        return compressText();
    }
}
