package trashsoftware.win_bwz.huffman;

import trashsoftware.win_bwz.longHuffman.LongHuffmanUtil;
import trashsoftware.win_bwz.utility.Bytes;
import trashsoftware.win_bwz.utility.FileBitInputStream;
import trashsoftware.win_bwz.utility.Util;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;

import static trashsoftware.win_bwz.longHuffman.LongHuffmanUtil.identicalMapOneLoop;

public class HuffmanDeCompressor {

    private String inFile;

    private int lengthRemainder;

    private int maxCodeLen = 0;

    private int average = 8;

    private int currentLen = 0;

    private final static int bufferSize = 8192;

//    private HashMap<Integer, Byte> shortMap = new HashMap<>();
//
//    private HashMap<Integer, Byte> longMap = new HashMap<>();
//
//    private HashMap<Byte, Integer> lengthMap = new HashMap<>();

    private int[] shortMapArr;

    private int[] longMapArr;

    private int[] lengthMap;

    private int bufferIndex;

    private byte[] buffer = new byte[bufferSize];

    int bits = 0;
    int bitPos = 0;

    private boolean hasNextByte = true;

    /**
     * Constructor of a new HuffmanDeCompressor Object.
     * <p>
     * Creates a new HuffmanDeCompressor which takes "inFile" as the compressed file.
     *
     * @param inFile the compressed file.
     */
    public HuffmanDeCompressor(String inFile) {
        this.inFile = inFile;
    }

//    private HashMap<Byte, Integer> recoverLengthCode(byte[] map) {
//        HashMap<Byte, Integer> lengthCode = new HashMap<>();
//        for (int i = 0; i < 256; i++) {
//            int len;
//            try {
//                len = map[i] & 0xff;
//            } catch (IndexOutOfBoundsException e) {
//                break;
//            }
//            if (len != 0) {
//                lengthCode.put((byte) i, len);
//                if (len > maxCodeLen) maxCodeLen = len;
//            }
//        }
//        if (average > maxCodeLen) average = maxCodeLen;
//        return lengthCode;
//    }
//
//    private void generateIdenticalMap(HashMap<Byte, String> origMap) {
//        for (byte value : origMap.keySet()) {
//            String s = origMap.get(value);
//            lengthMap.put(value, s.length());
//
//            if (s.length() > average) {
//                int len = maxCodeLen - s.length();
//                if (len == 0) {
//                    longMap.put(Integer.parseInt(s, 2), value);
//                } else {
//                    for (int i = 0; i < Math.pow(2, len); i++) {
//                        String key = s + Bytes.numberToBitString(i, len);
//                        longMap.put(Integer.parseInt(key, 2), value);
//                    }
//                }
//            } else {
//                int len = average - s.length();
//                if (len == 0) {
//                    shortMap.put(Integer.parseInt(s, 2), value);
//                } else {
//                    for (int i = 0; i < Math.pow(2, len); i++) {
//                        String key = s + Bytes.numberToBitString(i, len);
//                        shortMap.put(Integer.parseInt(key, 2), value);
//                    }
//                }
//            }
//        }
//    }

    private void uncompressText(InputStream fis, String outFile) throws IOException {
        BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(outFile));

        int read;
        if ((read = fis.read(buffer)) <= 0) {
            throw new RuntimeException();
        }
        bufferIndex = 0;

        int bigMapLonger = maxCodeLen - average;
        int bigMapLongerAndEr = Bytes.getAndEr(bigMapLonger);
        int averageAndEr = Bytes.getAndEr(average);

        while (hasNextByte) {
            readBits(average, fis);
            int index = (bits >> (bitPos - average)) & averageAndEr;
            bitPos -= average;

            int codeLen;
            int code = shortMapArr[index];
            if (code == 0) {  // not in short map
                readBits(bigMapLonger, fis);
                index <<= bigMapLonger;
                index |= ((bits >> (bitPos - bigMapLonger)) & bigMapLongerAndEr);
                bitPos -= bigMapLonger;
                code = longMapArr[index];
                codeLen = lengthMap[code];
                bitPos += (maxCodeLen - codeLen);
            } else {
                code -= 1;
                codeLen = lengthMap[code];
                bitPos += (average - codeLen);
            }
//            result[currentIndex++] = code;
            currentLen++;
            fos.write(code);
        }
    }

    private void readBits(int leastPos, InputStream fis) throws IOException {

        while (bitPos < leastPos) {
            bitPos += 8;
            bits <<= 8;
            bits |= (buffer[bufferIndex++] & 0xff);
            if (bufferIndex >= bufferSize) {

                if (fis.read(buffer) <= 0) {
                    hasNextByte = false;
                }
                bufferIndex = 0;
            }
        }
    }

//
//    private void uncompressText(InputStream bis, String outFile) throws IOException {
//        // Read and uncompress the main block
//        FileOutputStream fos = new FileOutputStream(outFile);
//        byte[] buffer = new byte[bufferSize];
//        StringBuilder builder = new StringBuilder();
//        int read;
//        while ((read = bis.read(buffer, 0, bufferSize)) != -1) {
//            for (int i = 0; i < read; i++) {
//                String s = Bytes.byteToBitString(buffer[i]);
//                builder.append(s);
//            }
//            byte[] uncompressed = uncompressToArray(builder);
//            fos.write(uncompressed);
//        }
//        fos.write(unCompressLastText(builder));
//        // Uncompress the last few bits
//        fos.flush();
//        fos.close();
//    }
//
//    private byte[] uncompressToArray(StringBuilder builder) {
//        int i = 0;
//        LinkedList<Byte> tempResult = new LinkedList<>();
//        while (builder.length() - i > maxCodeLen + 8) {
//            int index = Integer.parseInt(builder.substring(i, i + average), 2);
//            byte value;
//            int len;
//            if (shortMap.containsKey(index)) {
//                value = shortMap.get(index);
//                len = lengthMap.get(value);
//            } else {
//                value = longMap.get(Integer.parseInt(builder.substring(i, i + maxCodeLen), 2));
//                len = lengthMap.get(value);
//            }
//            tempResult.addLast(value);
//            i += len;
//        }
//        String rem = builder.substring(i);
//        builder.setLength(0);
//        builder.append(rem);
//        currentLen += tempResult.size();
//        return Util.collectionToArray(tempResult);
//    }
//
//    private byte[] unCompressLastText(StringBuilder builder) {
//        builder.append(Bytes.charMultiply('0', maxCodeLen * 8));
//        int i = 0;
//        LinkedList<Byte> tempResult = new LinkedList<>();
//
//        while (currentLen % 256 != lengthRemainder) {
//            int index = Integer.parseInt(builder.substring(i, i + average), 2);
//            byte value;
//            int len;
//            if (shortMap.containsKey(index)) {
//                value = shortMap.get(index);
//                len = lengthMap.get(value);
//
//            } else {
//                value = longMap.get(Integer.parseInt(builder.substring(i, i + maxCodeLen), 2));
//                len = lengthMap.get(value);
//            }
//            tempResult.addLast(value);
//            i += len;
//            currentLen += 1;
//        }
//        return Util.collectionToArray(tempResult);
//    }

    private void generateIdenticalMap(int[] lengthCode, int[] canonicalCode) {
        shortMapArr = new int[1 << average];
        longMapArr = new int[1 << maxCodeLen];

        for (int i = 0; i < lengthCode.length; ++i) {
            identicalMapOneLoop(lengthCode, canonicalCode, i, average, shortMapArr, maxCodeLen, longMapArr);
        }
    }

    private int[] recoverLengthCode(byte[] map) {
        int[] lengthCode = new int[map.length];
        for (int i = 0; i < map.length; ++i) {
            int len = map[i] & 0xff;
            if (len > 0) {
                lengthCode[i] = len;
                if (len > maxCodeLen) maxCodeLen = len;
            }
        }
        if (average > maxCodeLen) {
            average = maxCodeLen;
        }
        return lengthCode;
    }

    public void Uncompress(String outFile, byte[] map) throws IOException {

        FileInputStream fis = new FileInputStream(inFile);

        byte[] firstByte = new byte[1];
        if (fis.read(firstByte) != 1) throw new IOException("Error occurs while reading");
        lengthRemainder = firstByte[0] & 0xff;

        int alphabetSize = map.length;
        lengthMap = recoverLengthCode(map);
        int[] huffmanCode = LongHuffmanUtil.generateCanonicalCode(lengthMap);
        generateIdenticalMap(lengthMap, huffmanCode);

//        HashMap<Byte, Integer> lengthCode = recoverLengthCode(map);
//        HashMap<Byte, String> huffmanCode = HuffmanCompressor.generateCanonicalCode(lengthCode);
//        generateIdenticalMap(huffmanCode);

        // Uncompress main text
        uncompressText(fis, outFile);
        fis.close();

    }
}
