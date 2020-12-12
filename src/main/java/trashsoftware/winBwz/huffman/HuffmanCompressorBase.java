package trashsoftware.winBwz.huffman;

import trashsoftware.winBwz.longHuffman.HuffmanNode;
import trashsoftware.winBwz.longHuffman.LongHuffmanUtil;

import java.io.*;

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

    protected abstract void compressText(int[] huffmanCode, int[] lengthCode, OutputStream fos) throws IOException;

    public int[] getMap(int alphabetSize) throws IOException {
        freqMap = new int[alphabetSize];
        generateFreqMap();
        HuffmanNode rootNode = LongHuffmanUtil.generateHuffmanTree(freqMap);
        lengthCode = new int[alphabetSize];
        LongHuffmanUtil.generateCodeLengthMap(lengthCode, rootNode, 0);

        LongHuffmanUtil.heightControl(lengthCode, freqMap, MAX_HEIGHT);
        huffmanCode = LongHuffmanUtil.generateCanonicalCode(lengthCode);
        return lengthCode;
    }

    public abstract void SepCompress(OutputStream out) throws IOException;

    public long getCompressedLength() {
        return compressedLength;
    }
}
