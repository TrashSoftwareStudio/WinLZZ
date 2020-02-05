package trashsoftware.win_bwz.huffman;

import trashsoftware.win_bwz.longHuffman.HuffmanNode;
import trashsoftware.win_bwz.longHuffman.HuffmanTuple;
import trashsoftware.win_bwz.longHuffman.LongHuffmanUtil;
import trashsoftware.win_bwz.utility.Bytes;
import trashsoftware.win_bwz.utility.FileBitOutputStream;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class HuffmanCompressor {

    private String inFile;

    private final static int bufferSize = 8192;

    private int[] freqMap;

    private int lengthRemainder;

    private int inFileLength = 0;

    private long compressedLength = 0;

    private static final int MAX_HEIGHT = 15;

    private int[] huffmanCode;

    private int[] lengthCode;


    /**
     * Constructor of a new {@code HuffmanCompressor} instance.
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
            inFileLength += read;
        }
        lengthRemainder = inFileLength % 256;
        bis.close();
    }

    public static void addArrayToFreqMap(byte[] array, int[] freqMap, int range) {
        for (int i = 0; i < range; i++) {
            byte b = array[i];
            freqMap[b & 0xff]++;
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
        canonicalCode.put((byte) first.getValue(), Bytes.charMultiply('0', first.getLength()));

        int code = 0;
        for (int i = 1; i < tupleList.size(); i++) {
            code = (code + 1) << (tupleList.get(i).getLength() - tupleList.get(i - 1).getLength());
            String co = Integer.toBinaryString(code);
            if (co.length() < tupleList.get(i).getLength())
                co = Bytes.charMultiply('0', tupleList.get(i).getLength() - co.length()) + co;
            canonicalCode.put((byte) tupleList.get(i).getValue(), co);
        }
        return canonicalCode;
    }


    private static byte[] generateCanonicalCodeBlock(int[] lengthCode, int alphabetSize) {
        byte[] result = new byte[alphabetSize];
        for (int i = 0; i < alphabetSize; i++) {
            result[i] = (byte) lengthCode[i];
        }
        return result;
    }

    private void compressText(int[] huffmanCode, int[] lengthCode, OutputStream fos) throws IOException {
        FileInputStream bis = new FileInputStream(inFile);

        byte[] buffer = new byte[bufferSize];

        FileBitOutputStream fbo = new FileBitOutputStream(fos);

        int read;
        while ((read = bis.read(buffer, 0, bufferSize)) > 0) {
            for (int i = 0; i < read; ++i) {
                int v = buffer[i] & 0xff;
                int len = lengthCode[v];
                int code = huffmanCode[v];
                fbo.write(code, len);
            }
        }
        // Deal with the last few bits.
        fbo.flush();
        compressedLength += fbo.getLength();

//        fbo.close();
        bis.close();

    }

    public byte[] getMap(int alphabetSize) throws IOException {
        freqMap = new int[alphabetSize];
        generateFreqMap();
        HuffmanNode rootNode = LongHuffmanUtil.generateHuffmanTree(freqMap);
        lengthCode = new int[alphabetSize];
        LongHuffmanUtil.generateCodeLengthMap(lengthCode, rootNode, 0);
        LongHuffmanUtil.generateCodeLengthMap(lengthCode, rootNode, 0);

        LongHuffmanUtil.heightControl(lengthCode, freqMap, MAX_HEIGHT);
        huffmanCode = LongHuffmanUtil.generateCanonicalCode(lengthCode);
        return generateCanonicalCodeBlock(lengthCode, alphabetSize);
    }

    public void SepCompress(OutputStream out) throws IOException {
        compressedLength = 1;
        out.write((byte) lengthRemainder);
        compressText(huffmanCode, lengthCode, out);
    }

    public long getCompressedLength() {
        return compressedLength;
    }
}
