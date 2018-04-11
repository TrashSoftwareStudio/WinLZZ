package LongHuffman;

import Utility.Bytes;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class LongHuffmanCompressor {

    private String inFile;

    private final static int bufferSize = 8192;

    private HashMap<Short, Integer> freqMap = new HashMap<>();

    private HashMap<Short, String> huffmanCode = new HashMap<>();

    private int length = 0;

    private int compressedLength = 0;

//    private int lengthRemainder;

    private int maxHeight = 15;

    private int alphabetSize;

    /**
     * Constructor of a new HuffmanCompressor Object.
     * <p>
     * Creates a new HuffmanCompressor which takes the "inFile" as the file to compress.
     * Caution: There must be a end byte int original text!!!!!
     *
     * @param inFile the file to compress.
     */
    public LongHuffmanCompressor(String inFile, int alphabetSize) {
        this.inFile = inFile;
        this.alphabetSize = alphabetSize;
    }


    /**
     * Generates the frequency map.
     *
     * @throws IOException If file cannot be open.
     */
    private void generateFreqMap() throws IOException {

        // Create a buffered input stream
        FileInputStream bis = new FileInputStream(inFile);
        byte[] buffer = new byte[bufferSize];

        // read from file
        int read;
        while ((read = bis.read(buffer, 0, bufferSize)) != -1) {
            short[] shortBuffer = Util.byteArrayToShorts(buffer);
            addArrayToFreqMap(shortBuffer, freqMap, read / 2);
            length += read;
        }
//        lengthRemainder = length % 256;
        bis.close();
    }

    static void addArrayToFreqMap(short[] array, HashMap<Short, Integer> freqMap, int range) {
        for (int i = 0; i < range; i++) {
            short b = array[i];
            if (freqMap.containsKey(b)) {
                freqMap.put(b, freqMap.get(b) + 1);
            } else {
                freqMap.put(b, 1);
            }
        }
    }

    static HuffmanNode generateHuffmanTree(HashMap<Short, Integer> freqMap) {
        ArrayList<HuffmanNode> list = new ArrayList<>();
        for (short key : freqMap.keySet()) {
            HuffmanNode hn = new HuffmanNode(freqMap.get(key));
            hn.setValue(key);
            list.add(hn);
        }
        while (list.size() > 1) {
            Collections.sort(list);
            // Pop out two nodes with smallest frequency.
            HuffmanNode left = list.remove(list.size() - 1);
            HuffmanNode right = list.remove(list.size() - 1);
            HuffmanNode parent = new HuffmanNode(left.getFreq() + right.getFreq());
            parent.setLeft(left);
            parent.setRight(right);
            list.add(parent);
        }
        return list.get(0);
    }

    static void generateCodeLengthMap(HashMap<Short, Integer> lengthMap, HuffmanNode node, int length) {
        if (node != null) {
            if (node.isLeaf()) {
                lengthMap.put(node.getValue(), length);
            } else {
                generateCodeLengthMap(lengthMap, node.getLeft(), length + 1);
                generateCodeLengthMap(lengthMap, node.getRight(), length + 1);
            }
        }
    }

    static HashMap<Short, String> generateCanonicalCode(HashMap<Short, Integer> lengthCode) {
        HashMap<Short, String> canonicalCode = new HashMap<>();

        ArrayList<HuffmanTuple> tupleList = new ArrayList<>();

        for (short key : lengthCode.keySet()) {
            HuffmanTuple current = new HuffmanTuple(key, lengthCode.get(key));
            tupleList.add(current);
        }
        Collections.sort(tupleList);

        HuffmanTuple first = new HuffmanTuple(tupleList.get(0).getValue(), tupleList.get(0).getLength());
        canonicalCode.put(first.getValue(), Bytes.charMultiply('0', first.getLength()));

        int code = 0;
        for (int i = 1; i < tupleList.size(); i++) {
            code = (code + 1) << (tupleList.get(i).getLength() - tupleList.get(i - 1).getLength());
            String co = Integer.toBinaryString(code);
            if (co.length() < tupleList.get(i).getLength()) {
                co = Bytes.charMultiply('0', tupleList.get(i).getLength() - co.length()) + co;
            }
            canonicalCode.put(tupleList.get(i).getValue(), co);
        }

        return canonicalCode;
    }

    static byte[] generateCanonicalCodeBlock(HashMap<Short, Integer> lengthCode, int alphabetSize) {
        byte[] result = new byte[alphabetSize];
        for (int i = 0; i < alphabetSize; i++) {
            if (lengthCode.containsKey((short) i)) {
                int len = lengthCode.get((short) i);
                result[i] = (byte) len;
            } else {
                result[i] = (byte) 0;
            }
        }
        return result;
    }

    private void compressText(HashMap<Short, String> huffmanCode, OutputStream fos) throws IOException {
        FileInputStream bis = new FileInputStream(inFile);

        byte[] buffer = new byte[bufferSize];
        StringBuilder builder = new StringBuilder();  // sb is not a good name 233333
        int read;
        while ((read = bis.read(buffer, 0, bufferSize)) != -1) {
            short[] shortBuffer = Util.byteArrayToShorts(buffer);
            addCompressed(shortBuffer, read / 2, builder, huffmanCode);
            byte[] toWrite = Bytes.stringBuilderToBytes(builder);
            fos.write(toWrite);
            compressedLength += toWrite.length;

            // Fill remaining bits to the start of the builder
            String remain = builder.substring(builder.length() - builder.length() % 8);
            builder.setLength(0);
            builder.append(remain);
        }
        // Deal with the last few bits.
        if (builder.length() > 0) {
            fos.write(Bytes.bitStringToByteNo8(builder.toString()));
            compressedLength += 1;
        }
        bis.close();
    }

    static void addCompressed(short[] buffer, int range, StringBuilder builder, HashMap<Short, String> huffmanCode) {
        for (int i = 0; i < range; i++) {
            short b = buffer[i];
            builder.append(huffmanCode.get(b));
        }
    }


    public byte[] getMap(int length) throws IOException {
        generateFreqMap();
        HuffmanNode rootNode = generateHuffmanTree(freqMap);
        HashMap<Short, Integer> codeLengthMap = new HashMap<>();
        generateCodeLengthMap(codeLengthMap, rootNode, 0);

        heightControl(codeLengthMap, freqMap, maxHeight);
        huffmanCode = generateCanonicalCode(codeLengthMap);
        byte[] result = new byte[length];
        System.arraycopy(generateCanonicalCodeBlock(codeLengthMap, alphabetSize), 0, result, 0, length);
        return result;
    }


    /**
     * Compress using huffman algorithm.
     *
     * @param outFile the output stream.
     * @throws IOException if in-file is not readable or out-file is not writable.
     */
    public void Compress(OutputStream outFile) throws IOException {

        compressedLength = 0;
//        outFile.write((byte) lengthRemainder);
        compressText(huffmanCode, outFile);

//        boolean[] remainder = Bytes.numToBool3bit(lengthRemainder);
//        System.arraycopy(remainder, 0, infoByte, 2, 3);
//
//        byte[] head = new byte[]{Bytes.bitStringToByte(Bytes.booleanArrayToBitString(infoByte)), (byte) map.length,
//                (byte) startAndLen[0]};
//        outFile.write(head);
//        outFile.write(map);
//        compressedLength = head.length + map.length;
//
//        compressText(huffmanCode, outFile);
    }

    public int getCompressedLength() {
        return compressedLength;
    }

    static void heightControl(HashMap<Short, Integer> codeLength, HashMap<Short, Integer> freqMap, int maxHeight) {
        ArrayList<LengthTuple> list = new ArrayList<>();
        for (short key : codeLength.keySet()) {
            list.add(new LengthTuple(key, codeLength.get(key), freqMap.get(key)));
        }
        Collections.sort(list);

        int debt = getTotalDebt(list, maxHeight);
        repay(list, debt, maxHeight);
        for (LengthTuple lt : list) {
            codeLength.put(lt.getByte(), lt.length);
        }
    }

    private static int getTotalDebt(ArrayList<LengthTuple> list, int maxHeight) {
        double debt = 0;
        for (LengthTuple lt : list) {
            if (lt.length > maxHeight) {
                double num = Math.pow(2, (lt.length - maxHeight));
                debt += ((num - 1) / num);
                lt.length = maxHeight;
            }
        }
        return (int) Math.round(debt);
    }

    private static void repay(ArrayList<LengthTuple> list, int debt, int maxHeight) {
        while (debt > 0) {
            LengthTuple lt = list.get(getLastUnderLimit(list, maxHeight));
            int lengthDiff = maxHeight - lt.length;
            debt -= (int) Math.pow(2, (lengthDiff - 1));
            lt.length += 1;
        }
    }

    private static int getLastUnderLimit(ArrayList<LengthTuple> list, int maxHeight) {
        int i = list.size() - 1;
        while (list.get(i).length == maxHeight) {
            i -= 1;
        }
        return i;
    }
}


class LengthTuple implements Comparable<LengthTuple> {

    private short b;

    int length;

    private int freq;

    LengthTuple(short b, int length, int freq) {
        this.b = b;
        this.length = length;
        this.freq = freq;
    }

    public short getByte() {
        return b;
    }

    @Override
    public int compareTo(LengthTuple o) {
        return Integer.compare(o.freq, freq);
    }
}


class HuffmanNode implements Comparable<HuffmanNode> {


    private int freq;

    private short value;

    private HuffmanNode left;

    private HuffmanNode right;

    HuffmanNode(int freq) {
        this.freq = freq;
    }

    public void setValue(short value) {
        this.value = value;
    }

    void setLeft(HuffmanNode left) {
        this.left = left;
    }

    void setRight(HuffmanNode right) {
        this.right = right;
    }

    public short getValue() {
        return value;
    }

    HuffmanNode getLeft() {
        return left;
    }

    HuffmanNode getRight() {
        return right;
    }

    int getFreq() {
        return freq;
    }

    boolean isLeaf() {
        return left == null && right == null;
    }

    @Override
    public int compareTo(HuffmanNode o) {
        return -Integer.compare(freq, o.freq);
    }
}


class HuffmanTuple implements Comparable<HuffmanTuple> {

    private short value;

    private int codeLength;

    HuffmanTuple(short value, int codeLength) {
        this.value = value;
        this.codeLength = codeLength;
    }

    public int getLength() {
        return codeLength;
    }

    public short getValue() {
        return value;
    }

    @Override
    public int compareTo(HuffmanTuple o) {
        int lengthCmp = Integer.compare(codeLength, o.codeLength);
        if (lengthCmp == 0) {
            return Short.compare(value, o.value);
        } else {
            return lengthCmp;
        }
    }
}


