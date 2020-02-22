package trashsoftware.winBwz.huffman;

import trashsoftware.winBwz.longHuffman.LongHuffmanUtil;
import trashsoftware.winBwz.utility.Bytes;

import java.io.*;

public class HuffmanDeCompressor {

    private String inFile;

    private int maxCodeLen = 0;

    private int average = 8;

    private int currentLen = 0;

    private final static int bufferSize = 8192;

    private int[] identicalMap;

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
            int code = identicalMap[index];
            if (code == 0) {  // not in short map
                readBits(bigMapLonger, fis);
                index <<= bigMapLonger;
                index |= ((bits >> (bitPos - bigMapLonger)) & bigMapLongerAndEr);
                bitPos -= bigMapLonger;
                code = identicalMap[index];
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

    private void generateIdenticalMap(int[] lengthCode, int[] canonicalCode) {
        identicalMap = new int[1 << maxCodeLen];

        for (int i = 0; i < lengthCode.length; ++i) {
            LongHuffmanUtil.identicalMapOneLoop(lengthCode, canonicalCode, i, average, identicalMap, maxCodeLen, identicalMap);
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
        int lengthRemainder = firstByte[0] & 0xff;

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
