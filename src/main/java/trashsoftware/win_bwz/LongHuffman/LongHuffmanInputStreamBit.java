package trashsoftware.win_bwz.LongHuffman;

import trashsoftware.win_bwz.Utility.Bytes;
import trashsoftware.win_bwz.Utility.FileBitInputStream;

import java.io.*;
import java.util.HashMap;

public class LongHuffmanInputStreamBit {

    private FileBitInputStream inputStream;

    /**
     * The size of alphabet of original text.
     */
    private int alphabetSize;

    /**
     * The signal that makes the end of a part of the stream.
     */
    private int endSig;

    /**
     * The maximum code length.
     */
    private int maxCodeLen = 0;

    /**
     * The average code length
     */
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

    private int[] result;

    private int currentIndex;

    private int current;

    private int currentLength;

    private long cumulativeBitLength;

    public LongHuffmanInputStreamBit(InputStream input, long initBytePos, int alphabetSize, int maxLength) throws IOException {
        inputStream = new FileBitInputStream(input);
        this.alphabetSize = alphabetSize;
        this.result = new int[maxLength];
        cumulativeBitLength = initBytePos * 8;
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
        if (average > maxCodeLen) {
            average = maxCodeLen;
        }
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

    private static int getAND(int digits) {
        int x = 0;
        for (int i = 0; i < digits; i++) {
            x = x << 1 | 1;
        }
        return x;
    }

    private void uncompress() throws IOException {
        int read;
        while (true) {
            while (currentLength < average) {
                if ((read = inputStream.read()) == 2) current <<= 1;
                else current = ((current << 1) | read);
                System.out.print(read);
                currentLength++;
                cumulativeBitLength++;
            }
            int code;
            int len;
            if (shortMap.containsKey(current)) {
                code = shortMap.get(current);
            } else {
                while (currentLength < maxCodeLen) {
                    if ((read = inputStream.read()) == 2) current <<= 1;
                    else current = ((current << 1) | read);
                    System.out.print(read);
                    currentLength++;
                    cumulativeBitLength++;
                }
                code = longMap.get(current);
            }
            len = lengthMap.get(code);
            currentLength -= len;
//            if (code == endSig) //System.out.println(currentLength + "x");
            if (code == endSig) {
                System.out.println();
//                System.out.println(len + " " + maxCodeLen);
                if (currentLength == 0) {
//                if (currentLength != 0) throw new RuntimeException("End signal is not the longest");
                    System.out.println("gg");
                    while (cumulativeBitLength % 8 != 0) {
                        cumulativeBitLength++;
                        inputStream.read();
                    }
                    current = 0;
                } else {
                    long realLen = cumulativeBitLength - currentLength;
                    if ((realLen - 1) / 8 == (cumulativeBitLength - 1) / 8) {
                        System.out.println("b" + realLen + " " + cumulativeBitLength);
                        while (cumulativeBitLength % 8 != 0) {
                            cumulativeBitLength++;
                            inputStream.read();
                        }

                        current = 0;
                        currentLength = 0;
                    } else {
                        System.out.format("%d %d %d %d\n", cumulativeBitLength, realLen, currentLength, current);
                        currentLength = (int) (cumulativeBitLength % 8);
                        current &= getAND(currentLength);

                        while (cumulativeBitLength % 8 != 0) {
                            cumulativeBitLength--;
                        }
                        System.out.println(current + " " + currentLength);
                    }
                }
                System.out.println(cumulativeBitLength);
                break;
            }
            current &= getAND(currentLength);
            result[currentIndex++] = code;
        }
    }

    public byte[] read(int byteLength) {
        byte[] res = new byte[byteLength];
        try {
            int i = 0;
            while (i < byteLength) {
                int read = inputStream.read();
                cumulativeBitLength++;
                if (read == 2) return null;
                current = (current << 1) | read;
                if (currentLength == 7) {
                    res[i++] = (byte) current;
                    current = 0;
                    currentLength = 0;
                } else {
                    currentLength++;
                }
            }
        } catch (IOException e) {
            return null;
        }
        return res;
    }

    public int[] read(byte[] map, short endSig) throws IOException {
        this.endSig = endSig;
        shortMap.clear();
        longMap.clear();
        lengthMap.clear();
        currentIndex = 0;

        HashMap<Integer, Integer> lengthCode = recoverLengthCode(map);
        HashMap<Integer, String> huffmanCode = LongHuffmanUtil.generateCanonicalCode(lengthCode);
        generateIdenticalMap(huffmanCode);
        uncompress();

        int[] rtn = new int[currentIndex];
        System.arraycopy(result, 0, rtn, 0, currentIndex);
        return rtn;
    }

    public void close() throws IOException {
        inputStream.close();
    }
}
