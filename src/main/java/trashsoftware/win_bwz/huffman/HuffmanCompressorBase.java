package trashsoftware.win_bwz.huffman;

import trashsoftware.win_bwz.longHuffman.HuffmanNode;
import trashsoftware.win_bwz.longHuffman.HuffmanTuple;
import trashsoftware.win_bwz.longHuffman.LongHuffmanUtil;
import trashsoftware.win_bwz.utility.Bytes;
import trashsoftware.win_bwz.utility.FileBitOutputStream;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

public abstract class HuffmanCompressorBase {

    protected String inFile;

    protected final static int bufferSize = 8192;

    protected int[] freqMap;

    protected int lengthRemainder;

    protected int inFileLength = 0;

    protected long compressedLength = 0;

    private static final int MAX_HEIGHT = 15;

    protected int[] huffmanCode;

    protected int[] lengthCode;


    /**
     * Constructor of a new {@code HuffmanCompressor} instance.
     * <p>
     * Creates a new HuffmanCompressor which takes the "inFile" as the file to compress.
     *
     * @param inFile the file to compress.
     */
    public HuffmanCompressorBase(String inFile) {
        this.inFile = inFile;
    }

    protected abstract void generateFreqMap() throws IOException;

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

    protected abstract void compressText(int[] huffmanCode, int[] lengthCode, OutputStream fos) throws IOException;

    public byte[] getMap(int alphabetSize) throws IOException {
        freqMap = new int[alphabetSize];
        generateFreqMap();
        HuffmanNode rootNode = LongHuffmanUtil.generateHuffmanTree(freqMap);
        lengthCode = new int[alphabetSize];
        LongHuffmanUtil.generateCodeLengthMap(lengthCode, rootNode, 0);

        LongHuffmanUtil.heightControl(lengthCode, freqMap, MAX_HEIGHT);
        huffmanCode = LongHuffmanUtil.generateCanonicalCode(lengthCode);
        return generateCanonicalCodeBlock(lengthCode, alphabetSize);
    }

    public abstract void SepCompress(OutputStream out) throws IOException;

    public long getCompressedLength() {
        return compressedLength;
    }
}
