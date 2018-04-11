package LongHuffman;

import Utility.Bytes;
import Utility.Util;

import java.util.HashMap;
import java.util.LinkedList;

public class LongHuffmanDeCompressorRam {

    private byte[] text;

    private int maxCodeLen = 0;

    private int average = 8;

    private HashMap<Integer, Short> shortMap = new HashMap<>();

    private HashMap<Integer, Short> longMap = new HashMap<>();

    private HashMap<Short, Integer> lengthMap = new HashMap<>();

    private long compressedBitLength = 0;

    private int alphabetSize;

    private short endSig;

    private boolean isTerminated;

    public LongHuffmanDeCompressorRam(byte[] text, int alphabetSize) {
        this.text = text;
        this.alphabetSize = alphabetSize;
    }

    private HashMap<Short, Integer> recoverLengthCode(byte[] map) {
        HashMap<Short, Integer> lengthCode = new HashMap<>();
        for (int i = 0; i < alphabetSize; i++) {
            int len = map[i] & 0xff;
            if (len != 0) {
                lengthCode.put((short) i, len);
                if (len > maxCodeLen) {
                    maxCodeLen = len;
                }
            }
        }
        if (average > maxCodeLen) {
            average = maxCodeLen;
        }
        return lengthCode;
    }

    private void generateIdenticalMap(HashMap<Short, String> origMap) {
        for (short value : origMap.keySet()) {
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

    private LinkedList<Short> uncompressToArray(StringBuilder builder) {
        int i = 0;
        LinkedList<Short> tempResult = new LinkedList<>();
        while (builder.length() - i > maxCodeLen + 8) {
            int index = Integer.parseInt(builder.substring(i, i + average), 2);
            short value;
            int len;

            if (shortMap.containsKey(index)) {
                value = shortMap.get(index);
                len = lengthMap.get(value);

            } else {
                value = longMap.get(Integer.parseInt(builder.substring(i, i + maxCodeLen), 2));
                len = lengthMap.get(value);
            }
            compressedBitLength += len;
            if (value == endSig) {
                isTerminated = true;
                break;
            }
            tempResult.addLast(value);
            i += len;

        }
        String rem = builder.substring(i);
        builder.setLength(0);
        builder.append(rem);
        return tempResult;
    }

    private LinkedList<Short> unCompressLastText(StringBuilder builder) {
        builder.append(Bytes.charMultiply('0', maxCodeLen * 8));
        int i = 0;
        LinkedList<Short> tempResult = new LinkedList<>();

        while (true) {
            int index = Integer.parseInt(builder.substring(i, i + average), 2);
            short value;
            int len;
            if (shortMap.containsKey(index)) {
                value = shortMap.get(index);
                len = lengthMap.get(value);

            } else {
                value = longMap.get(Integer.parseInt(builder.substring(i, i + maxCodeLen), 2));
                len = lengthMap.get(value);
            }
            compressedBitLength += len;
            if (value == endSig) {
                break;
            }
            tempResult.addLast(value);
            i += len;
        }
        return tempResult;
    }

    public short[] Uncompress(byte[] map, short endSig) {
        this.endSig = endSig;
        HashMap<Short, Integer> lengthCode = recoverLengthCode(map);
        HashMap<Short, String> huffmanCode = LongHuffmanCompressor.generateCanonicalCode(lengthCode);
        generateIdenticalMap(huffmanCode);

        long st = System.currentTimeMillis();
        StringBuilder builder = Bytes.bytesToStringBuilder(text);
        System.out.println(System.currentTimeMillis() - st);
        LinkedList<Short> result = uncompressToArray(builder);
        if (!isTerminated) {
            LinkedList<Short> lastText = unCompressLastText(builder);
            result.addAll(lastText);
        }

        return Util.collectionToShortArray(result);
    }

    public long getCompressedLength() {
        if (compressedBitLength % 8 == 0) {
            return compressedBitLength / 8;
        } else {
            return compressedBitLength / 8 + 1;
        }
    }
}
