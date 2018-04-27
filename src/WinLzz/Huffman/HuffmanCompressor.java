package WinLzz.Huffman;

import WinLzz.Utility.Bytes;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class HuffmanCompressor {

    private String inFile;

    private final static int bufferSize = 8192;

    private HashMap<Byte, Integer> freqMap = new HashMap<>();

    private int lengthRemainder;

    private int length = 0;

    private int compressedLength = 0;

    private int maxHeight = 15;

    private HashMap<Byte, String> huffmanCode;


    /**
     * Constructor of a new HuffmanCompressor Object.
     * <p>
     * Creates a new HuffmanCompressor which takes the "inFile" as the file to compress.
     *
     * @param inFile the file to compress.
     */
    public HuffmanCompressor(String inFile) {
        this.inFile = inFile;
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
            addArrayToFreqMap(buffer, freqMap, read);
            length += read;
        }
        lengthRemainder = length % 256;
        bis.close();
    }

    public static void addArrayToFreqMap(byte[] array, HashMap<Byte, Integer> freqMap, int range) {
        for (int i = 0; i < range; i++) {
            byte b = array[i];
            if (freqMap.containsKey(b)) freqMap.put(b, freqMap.get(b) + 1);
            else freqMap.put(b, 1);
        }
    }

    public static HuffmanNode generateHuffmanTree(HashMap<Byte, Integer> freqMap) {
        ArrayList<HuffmanNode> list = new ArrayList<>();
        for (byte key : freqMap.keySet()) {
            HuffmanNode hn = new HuffmanNode(freqMap.get(key));
            hn.setValue(key);
            list.add(hn);
        }
        if (list.size() == 1) {
            HuffmanNode leaf = list.get(0);
            HuffmanNode root = new HuffmanNode(leaf.getFreq() + 1);
            root.setLeft(leaf);
            return root;
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

    public static void generateCodeLengthMap(HashMap<Byte, Integer> lengthMap, HuffmanNode node, int length) {
        if (node != null) {
            if (node.isLeaf()) {
                lengthMap.put(node.getValue(), length);
            } else {
                generateCodeLengthMap(lengthMap, node.getLeft(), length + 1);
                generateCodeLengthMap(lengthMap, node.getRight(), length + 1);
            }
        }
    }

    public static HashMap<Byte, String> generateCanonicalCode(HashMap<Byte, Integer> lengthCode) {
        HashMap<Byte, String> canonicalCode = new HashMap<>();

        ArrayList<HuffmanTuple> tupleList = new ArrayList<>();

        for (byte key : lengthCode.keySet()) {
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
            if (co.length() < tupleList.get(i).getLength())
                co = Bytes.charMultiply('0', tupleList.get(i).getLength() - co.length()) + co;
            canonicalCode.put(tupleList.get(i).getValue(), co);
        }
        return canonicalCode;
    }

    private byte[] generateCanonicalCodeBlock(HashMap<Byte, Integer> lengthCode) {
        byte[] result = new byte[256];
        for (int i = 0; i < 256; i++) {
            if (lengthCode.containsKey((byte) i)) {
                int len = lengthCode.get((byte) i);
                result[i] = (byte) len;
            } else {
                result[i] = (byte) 0;
            }
        }
        return result;
    }

    private void compressText(HashMap<Byte, String> huffmanCode, OutputStream fos) throws IOException {
        FileInputStream bis = new FileInputStream(inFile);

        byte[] buffer = new byte[bufferSize];
        StringBuilder builder = new StringBuilder();  // sb is not a good name 233333
        int read;
        while ((read = bis.read(buffer, 0, bufferSize)) != -1) {
            addCompressed(buffer, read, builder, huffmanCode);
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

    public static void addCompressed(byte[] buffer, int range, StringBuilder builder, HashMap<Byte, String> huffmanCode) {
        for (int i = 0; i < range; i++) {
            byte b = buffer[i];
            builder.append(huffmanCode.get(b));
        }
    }

    public byte[] getMap(int length) throws IOException {
        generateFreqMap();
        HuffmanNode rootNode = generateHuffmanTree(freqMap);
        HashMap<Byte, Integer> codeLengthMap = new HashMap<>();
        generateCodeLengthMap(codeLengthMap, rootNode, 0);

        heightControl(codeLengthMap);
        huffmanCode = generateCanonicalCode(codeLengthMap);
        byte[] result = new byte[length];
        System.arraycopy(generateCanonicalCodeBlock(codeLengthMap), 0, result, 0, length);
        return result;
    }

    public void SepCompress(OutputStream out) throws IOException {
        compressedLength = 1;
        out.write((byte) lengthRemainder);
        compressText(huffmanCode, out);
    }

    public int getCompressedLength() {
        return compressedLength;
    }

    private void heightControl(HashMap<Byte, Integer> codeLength) {
        ArrayList<LengthTuple> list = new ArrayList<>();
        for (byte key : codeLength.keySet()) list.add(new LengthTuple(key, codeLength.get(key), freqMap.get(key)));
        Collections.sort(list);

        int debt = getTotalDebt(list);
        repay(list, debt);
        for (LengthTuple lt : list) codeLength.put(lt.getByte(), lt.length);
    }

    private int getTotalDebt(ArrayList<LengthTuple> list) {
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

    private void repay(ArrayList<LengthTuple> list, int debt) {
        while (debt > 0) {
            LengthTuple lt = list.get(getLastUnderLimit(list, maxHeight));
            int lengthDiff = maxHeight - lt.length;
            debt -= (int) Math.pow(2, (lengthDiff - 1));
            lt.length += 1;
        }
    }

    public static int getLastUnderLimit(ArrayList<LengthTuple> list, int max) {
        int i = list.size() - 1;
        while (list.get(i).length == max) i -= 1;
        return i;
    }
}
