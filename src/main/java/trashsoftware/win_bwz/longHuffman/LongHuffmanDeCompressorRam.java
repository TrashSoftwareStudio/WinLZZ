package trashsoftware.win_bwz.longHuffman;

import trashsoftware.win_bwz.utility.Bytes;

import java.util.HashMap;

/**
 * A decompressor of huffman coding that all operations take places in the random access memory.
 *
 * @author zbh
 * @since 0.5
 */
public class LongHuffmanDeCompressorRam {

    private int alphabetSize;
    private int endSig;
    private int maxCodeLen = 0;
    private int average = 8;

    /**
     * The huffman code table that records all codes that are shorter than or equal to {@code average}, but all codes
     * shorter than {@code average} is extended by all possible combination of 0's and 1's until they reaches the
     * length {@code average}.
     */
    private HashMap<Integer, Integer> shortMap = new HashMap<>();

    /**
     * The huffman code table that records all codes that are shorter than or equal to {@code maxCodeLen}, but all
     * codes shorter than {@code maxCodeLen} is extended by all possible combination of 0's and 1's until they
     * reaches the length {@code maxCodeLen}.
     */
    private HashMap<Integer, Integer> longMap = new HashMap<>();

    /**
     * The table that records all huffman symbol and their corresponding code length.
     */
    private HashMap<Integer, Integer> lengthMap = new HashMap<>();

    private byte[] text;
    private int[] result;
    private int currentIndex;
    private StringBuilder builder = new StringBuilder();

    /**
     * Creates a new {@code LongHuffmanDeCompressorRam} instance,
     * <p>
     * This decompressor works completely in random access memory.
     *
     * @param text         the text to be uncompressed.
     * @param alphabetSize the alphabet size.
     * @param maxLength    the maximum length of original text.
     */
    public LongHuffmanDeCompressorRam(byte[] text, int alphabetSize, int maxLength) {
        this.text = text;
        this.alphabetSize = alphabetSize;
        this.result = new int[maxLength];
    }

    private HashMap<Integer, Integer> recoverLengthCode(byte[] map) {
        HashMap<Integer, Integer> lengthCode = new HashMap<>();
        for (int i = 0; i < alphabetSize; i++) {
            int len = map[i] & 0xff;
            if (len != 0) {
                lengthCode.put(i, len);
                if (len > maxCodeLen) maxCodeLen = len;
            }
        }
        if (average > maxCodeLen) average = maxCodeLen;
        return lengthCode;
    }

    private void generateIdenticalMap(HashMap<Integer, String> origMap) {
        for (int value : origMap.keySet()) {
            String s = origMap.get(value);
            lengthMap.put(value, s.length());

            if (s.length() > average) {
                int len = maxCodeLen - s.length();
                if (len == 0) {
                    longMap.put(Integer.parseInt(s, 2), value);
                } else {
                    for (int i = 0; i < Math.pow(2, len); i++) {
                        String key = s + Bytes.numberToBitString(i, len);
                        longMap.put(Integer.parseInt(key, 2), value);
                    }
                }
            } else {
                int len = average - s.length();
                if (len == 0) {
                    shortMap.put(Integer.parseInt(s, 2), value);
                } else {
                    for (int i = 0; i < Math.pow(2, len); i++) {
                        String key = s + Bytes.numberToBitString(i, len);
                        shortMap.put(Integer.parseInt(key, 2), value);
                    }
                }
            }
        }
    }

    private void uncompressToArray() {
        builder.append(Bytes.charMultiply('0', maxCodeLen * 8));
        int i = 0;
        while (true) {
            int index = Integer.parseInt(builder.substring(i, i + average), 2);
            int value;
            int len;
            if (shortMap.containsKey(index)) {
                value = shortMap.get(index);
                len = lengthMap.get(value);
            } else {
                value = longMap.get(Integer.parseInt(builder.substring(i, i + maxCodeLen), 2));
                len = lengthMap.get(value);
            }
            if (value == endSig) {
                break;
            }
            result[currentIndex] = value;
            currentIndex += 1;
            i += len;
        }
        String rem = builder.substring(i);
        builder.setLength(0);
        builder.append(rem);
    }

    /**
     * Read and uncompress the huffman compression file until reaches the next endSig.
     *
     * @param map    Canonical huffman map for this read action.
     * @param endSig The EOF character.
     * @return The uncompressed text.
     */
    public int[] uncompress(byte[] map, int endSig) {
        this.endSig = endSig;

        currentIndex = 0;
        builder.setLength(0);
        shortMap.clear();
        longMap.clear();
        lengthMap.clear();

        HashMap<Integer, Integer> lengthCode = recoverLengthCode(map);
        HashMap<Integer, String> huffmanCode = LongHuffmanUtil.generateCanonicalCode(lengthCode);
        generateIdenticalMap(huffmanCode);
        builder = Bytes.bytesToStringBuilder(text);
        uncompressToArray();

        int[] rtn = new int[currentIndex];
        System.arraycopy(result, 0, rtn, 0, currentIndex);
        return rtn;
    }
}

