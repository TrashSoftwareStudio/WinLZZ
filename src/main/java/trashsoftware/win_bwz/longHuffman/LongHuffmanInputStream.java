package trashsoftware.win_bwz.longHuffman;

import trashsoftware.win_bwz.utility.Bytes;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * A Independent input stream that takes an input stream and uncompress data from stream using huffman algorithm.
 *
 * @author zbh
 * @since 0.5.2
 */
public class LongHuffmanInputStream {

    /**
     * The IO buffer size.
     */
    private static final int bufferSize = 8192;

    private FileChannel fc;

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
    private int[] shortMapArr;

    /**
     * The huffman code table that records all codes that are shorter than or equal to {@code maxCodeLen}, but all
     * codes shorter than {@code maxCodeLen} is extended by all possible combination of 0's and 1's until they
     * reaches the length {@code maxCodeLen}.
     */
    private int[] longMapArr;

    /**
     * The table that records all huffman symbol and their corresponding code length.
     */
    private int[] lengthMap;

    private long compressedBitLength;

    private int[] result;

    private int currentIndex;

    private ByteBuffer readBuffer = ByteBuffer.allocate(bufferSize);

    private int bufferIndex;

    int bits = 0;
    int bitPos = 0;

    /**
     * Creates a new {@code LongHuffmanInputStream} instance.
     *
     * @param fc           the input stream file channel.
     * @param alphabetSize the alphabet size.
     * @param maxLength    the maximum length of each part of huffman text.
     */
    public LongHuffmanInputStream(FileChannel fc, int alphabetSize, int maxLength) throws IOException {
        this.fc = fc;
        this.compressedBitLength = fc.position() * 8;
        this.alphabetSize = alphabetSize;
        this.result = new int[maxLength];
    }

    private int[] recoverLengthCode(byte[] map) {
        int[] lengthCode = new int[alphabetSize];
        for (int i = 0; i < alphabetSize; ++i) {
            int len = map[i] & 0xff;
            if (len > 0) {
                lengthCode[i] = len;
                if (len > maxCodeLen) maxCodeLen = len;
            }
        }
        if (average > maxCodeLen) {
            average = maxCodeLen;
        }
//        System.out.println("max:" + maxCodeLen);
        return lengthCode;
    }

    private void generateIdenticalMap(int[] lengthCode, int[] canonicalCode) {
        shortMapArr = new int[1 << average];
        longMapArr = new int[1 << maxCodeLen];

        for (int i = 0; i < alphabetSize; ++i) {
            int len = lengthCode[i];
            if (len > 0) {
                int code = canonicalCode[i];
                if (len < average) {
                    int sup_len = average - len;
                    int sup_pow = 1 << sup_len;
                    int res = code << sup_len;
                    for (int j = 0; j < sup_pow; ++j) {
                        shortMapArr[res + j] = i + 1;  // 0 reserved for not found
//                        shortMap.put(res + j, i);
                    }
                } else if (len == average) {
//                    shortMap.put(code, i);
                    shortMapArr[code] = i + 1;  // 0 reserved for not found
                } else if (len < maxCodeLen) {
                    int sup_len = maxCodeLen - len;
                    int sup_pow = 1 << sup_len;
                    int res = code << sup_len;
                    for (int j = 0; j < sup_pow; ++j) {
                        longMapArr[res + j] = i;
//                        longMap.put(res + j, i);
                    }
                } else if (len == maxCodeLen) {
//                    longMap.put(code, i);
                    longMapArr[code] = i;
                } else {
                    throw new RuntimeException("Code too long");
//                    printf("Code length exceed max. Max: %d, got: %d\n", MAX_CODE_LEN, len);
//                    exit(3);
                }
            }
        }
    }

    private void readBits(int leastPos) throws IOException {

        while (bitPos < leastPos) {
            bitPos += 8;
            bits <<= 8;
            bits |= (readBuffer.array()[bufferIndex++] & 0xff);
            if (bufferIndex >= bufferSize) {
                readBuffer.clear();
                if (fc.read(readBuffer) <= 0) {

                }
                readBuffer.flip();
                bufferIndex = 0;
            }
        }
    }

    private void unCompress() throws IOException {

        readBuffer.clear();
        if (fc.read(readBuffer) <= 0) {
            throw new RuntimeException();
        }
        readBuffer.flip();
        bufferIndex = 0;

        int bigMapLonger = maxCodeLen - average;
        int bigMapLongerAndEr = Bytes.getAndEr(bigMapLonger);
        int averageAndEr = Bytes.getAndEr(average);

        while (true) {
            readBits(average);
            int index = (bits >> (bitPos - average)) & averageAndEr;
            bitPos -= average;

            int codeLen;
            int code = shortMapArr[index];
//            Integer code = shortMap.get(index);
            if (code == 0) {  // not in short map
//                    System.out.println("ind " + index);
                readBits(bigMapLonger);
                index <<= bigMapLonger;
                index |= ((bits >> (bitPos - bigMapLonger)) & bigMapLongerAndEr);
                bitPos -= bigMapLonger;
                code = longMapArr[index];
//                code = longMap.get(index);
                codeLen = lengthMap[code];
                bitPos += (maxCodeLen - codeLen);
            } else {
                code -= 1;
                codeLen = lengthMap[code];
                bitPos += (average - codeLen);
            }
            compressedBitLength += codeLen;

            if (code == endSig) {
//                isTerminated = true;
                while (compressedBitLength % 8 != 0) compressedBitLength += 1;  // fill to full byte
//                System.out.print(getCompressedLength() + " ");
                fc.position(getCompressedLength());
                break;
            } else {
                result[currentIndex++] = code;
            }
        }
    }

    private long getCompressedLength() {
        if (compressedBitLength % 8 == 0) return compressedBitLength / 8;
        else return compressedBitLength / 8 + 1;
    }

    /**
     * Sets up the current bit position.
     *
     * @param compressedBitLength the current bit position.
     */
    @Deprecated
    public void pushCompressedBitLength(long compressedBitLength) {
        this.compressedBitLength += compressedBitLength;
    }

    /**
     * Reads and returns bytes directly from the input stream.
     *
     * @param length the length of read
     * @return the byte content, null if stream ends
     * @throws IOException if the input stream is not readable
     */
    public byte[] read(int length) throws IOException {
        compressedBitLength += length * 8;
        ByteBuffer buffer = ByteBuffer.allocate(length);
        if (fc.read(buffer) != length) return null;
        return buffer.array();
    }

    static long timer;

    /**
     * Reads and uncompress the huffman compression file until reaches the next endSig.
     *
     * @param map    Canonical huffman map for this read action.
     * @param endSig The EOF character.
     * @return The uncompressed text.
     * @throws IOException If the file is not readable.
     */
    public int[] read(byte[] map, int endSig) throws IOException {
        this.endSig = endSig;

        currentIndex = 0;
        bitPos = 0;
        bits = 0;

        int[] lengthCode = recoverLengthCode(map);

        this.lengthMap = lengthCode;
        int[] huffmanCode = LongHuffmanUtil.generateCanonicalCode(lengthCode);

        generateIdenticalMap(lengthCode, huffmanCode);

        long t1 = System.currentTimeMillis();

        unCompress();

        timer += System.currentTimeMillis() - t1;

        int[] rtn = new int[currentIndex];
        System.arraycopy(result, 0, rtn, 0, currentIndex);
        return rtn;
    }
}
