package Huffman;

import Utility.Bytes;

import java.util.HashMap;

public class ShortHuffmanCompressor {

    private byte[] text;

    private int lengthRemainder;

    public ShortHuffmanCompressor(byte[] text) {
        this.text = text;
    }

    private static String generateMap(HashMap<Byte, Integer> lengthMap) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            if (lengthMap.containsKey((byte) i)) {
                String bits = Bytes.numberToBitString(lengthMap.get((byte) i), 4);
                builder.append(bits);
            } else {
                builder.append("0000");
            }
        }
        return builder.toString();
    }

    private String compressText(HashMap<Byte, String> huffmanCode) {
        StringBuilder builder = new StringBuilder();
        HuffmanCompressor.addCompressed(text, text.length, builder, huffmanCode);
        lengthRemainder = (builder.length() + 4) % 8;
        return builder.toString();
    }

    public byte[] Compress() {
        StringBuilder result = new StringBuilder();
        HashMap<Byte, Integer> freqMap = new HashMap<>();
        HuffmanCompressor.addArrayToFreqMap(text, freqMap, text.length);
        if (freqMap.size() == 1) {
            // If only one length contained in text.
            result.append('0');
            result.append(Bytes.byteToBitString(text[0]));
            result.append(Bytes.numberToBitString(text.length, 9));
        } else {
            HuffmanNode rootNode = HuffmanCompressor.generateHuffmanTree(freqMap);
            HashMap<Byte, Integer> lengthMap = new HashMap<>();
            HuffmanCompressor.generateCodeLengthMap(lengthMap, rootNode, 0);
            HashMap<Byte, String> huffmanCode = HuffmanCompressor.generateCanonicalCode(lengthMap);

            result.append("1000");  // Obligate space for length remainder code.
            result.append(generateMap(lengthMap));
            result.append(compressText(huffmanCode));
            result.replace(1, 4, Bytes.numberToBitString(lengthRemainder, 3));
        }
        return Bytes.stringBuilderToBytesFull(result);
    }
}
