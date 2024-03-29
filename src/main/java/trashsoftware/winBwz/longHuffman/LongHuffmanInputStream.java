package trashsoftware.winBwz.longHuffman;

import trashsoftware.winBwz.utility.Bytes;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.TreeMap;

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

    /**
     * Code exceeds this length would be stored in extra map, not identical map.
     */
    private static final int CODE_LEN_LIMIT = 16;

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
     * The maximum code length storing in the identical map.
     */
    private int longCodeLen = 0;

    /**
     * The average code length
     */
    private int shortCodeLen = 8;

    /**
     * The maximum code length.
     * <p>
     * If this value is greater than {@code CODE_LEN_LIMIT}, then extra map {@code extraMap} is used.
     */
    private int maxCodeLen = 0;

    /**
     * Map storing codes that longer than {@code CODE_LEN_LIMIT}.
     * <p>
     * Use tree map data structure because the extra map usually contains few elements, but with big value keys.
     */
    private Map<Integer, Integer> extraMap = new TreeMap<>();

    /**
     * This map is a combination of two maps.
     * <p>
     * The short map that records all codes that are shorter than or equal to {@code average}, but all codes
     * shorter than {@code average} is extended by all possible combination of 0's and 1's until they reaches the
     * length {@code average}.
     * The long map that records all codes that are shorter than or equal to {@code maxCodeLen}, but all
     * codes shorter than {@code maxCodeLen} is extended by all possible combination of 0's and 1's until they
     * reaches the length {@code maxCodeLen}.
     * <p>
     * The first {@code Math.power(2, shortMapLength)} ints is the short map. It stores the 8-bits identical code +1.
     * So 0 represents "not in the short map".
     */
    private int[] identicalMap;

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
        if (shortCodeLen > maxCodeLen) {
            shortCodeLen = maxCodeLen;
        }
//        longCodeLen = maxCodeLen;
        longCodeLen = Math.min(maxCodeLen, CODE_LEN_LIMIT);
        return lengthCode;
    }

    private void generateIdenticalMap(int[] lengthCode, int[] canonicalCode) {
        identicalMap = new int[1 << longCodeLen];
//        System.out.println(identicalMap.length);
        extraMap.clear();

        for (int i = 0; i < alphabetSize; ++i) {
            LongHuffmanUtil.identicalMapOneLoop(
                    lengthCode,
                    canonicalCode,
                    i,
                    shortCodeLen,
                    identicalMap,
                    longCodeLen,
                    identicalMap,
                    maxCodeLen,
                    extraMap
            );
        }
    }

    private void loadBits(int leastPos) throws IOException {

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

        int bigMapLonger = longCodeLen - shortCodeLen;
        int bigMapLongerAndEr = Bytes.getAndEr(bigMapLonger);
        int averageAndEr = Bytes.getAndEr(shortCodeLen);
        int maxLonger = maxCodeLen - longCodeLen;

        while (true) {
            loadBits(shortCodeLen);
            int index = (bits >> (bitPos - shortCodeLen)) & averageAndEr;
            bitPos -= shortCodeLen;

            int codeLen;
            int code = identicalMap[index];
            if (code == 0) {  // not in short map, look for long map
                loadBits(bigMapLonger);
                index <<= bigMapLonger;
                index |= ((bits >> (bitPos - bigMapLonger)) & bigMapLongerAndEr);
                bitPos -= bigMapLonger;
                code = identicalMap[index];
                if (code == 0) { //  not in long map, look for extra map
                    loadBits(maxLonger);
                    int exceedLen = 1;
                    while (true) {
                        index <<= 1;
                        index |= (bits >> (bitPos - exceedLen)) & 1;
                        Integer extraCode = extraMap.get(index);
                        codeLen = longCodeLen + exceedLen;
                        if (extraCode != null && lengthMap[extraCode] == codeLen) {
                            code = extraCode;
                            break;
                        }
                        exceedLen++;
                    }
                    bitPos -= exceedLen;
                } else {  // in long map
                    code -= 1;
                    codeLen = lengthMap[code];
                    bitPos += (longCodeLen - codeLen);
                }
            } else {
                code -= 1;
                codeLen = lengthMap[code];
                bitPos += (shortCodeLen - codeLen);
            }
            compressedBitLength += codeLen;

            if (code == endSig) {
                while (compressedBitLength % 8 != 0) compressedBitLength += 1;  // fill to full byte
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
