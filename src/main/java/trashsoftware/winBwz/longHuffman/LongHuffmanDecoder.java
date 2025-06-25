package trashsoftware.winBwz.longHuffman;

import trashsoftware.winBwz.utility.Bytes;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

public abstract class LongHuffmanDecoder {

    /**
     * Code exceeds this length would be stored in extra map, not identical map.
     */
    protected static final int CODE_LEN_LIMIT = 16;

    /**
     * The size of alphabet of original text.
     */
    protected final int alphabetSize;

    /**
     * The signal that makes the end of a part of the stream.
     */
    protected int endSig;

    /**
     * The maximum code length storing in the identical map.
     */
    protected int longCodeLen = 0;

    /**
     * The average code length
     */
    protected int shortCodeLen = 8;

    /**
     * The maximum code length.
     * <p>
     * If this value is greater than {@code CODE_LEN_LIMIT}, then extra map {@code extraMap} is used.
     */
    protected int maxCodeLen = 0;

    /**
     * Map storing codes that longer than {@code CODE_LEN_LIMIT}.
     * <p>
     * Use tree map data structure because the extra map usually contains few elements, but with big value keys.
     */
    protected final Map<Integer, Integer> extraMap = new TreeMap<>();

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
    protected int[] identicalMap;

    /**
     * The table that records all huffman symbol and their corresponding code length.
     */
    protected int[] lengthMap;

    protected long compressedBitLength;

    protected int currentIndex;

    protected int bits = 0;
    protected int bitPos = 0;
    
    protected LongHuffmanDecoder(int alphabetSize) {
        this.alphabetSize = alphabetSize;
    }
    
    protected abstract void loadBits(int leastPos) throws IOException;

    protected long getCompressedLength() {
        if (compressedBitLength % 8 == 0) return compressedBitLength / 8;
        else return compressedBitLength / 8 + 1;
    }
    
    protected abstract void setInputPosition(long pos) throws IOException;
    
    protected abstract void appendResult(int code);
    
    protected abstract void decodeToEof() throws IOException;
    
    protected abstract int[] packResult();

    protected void decodeTillEofCore() throws IOException {
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
                setInputPosition(getCompressedLength());
                break;
            } else {
                appendResult(code);
            }
        }
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

    static long timer;

    /**
     * Reads and uncompress the huffman compression file until reaches the next endSig.
     *
     * @param map    Canonical huffman map for this read action.
     * @param endSig The EOF character.
     * @return The uncompressed text.
     * @throws IOException If the file is not readable.
     */
    public int[] readNextCompressedBlock(byte[] map, int endSig) throws IOException {
        this.endSig = endSig;

        currentIndex = 0;
        bitPos = 0;
        bits = 0;

        int[] lengthCode = recoverLengthCode(map);

        this.lengthMap = lengthCode;
        int[] huffmanCode = LongHuffmanUtil.generateCanonicalCode(lengthCode);

        generateIdenticalMap(lengthCode, huffmanCode);

        long t1 = System.currentTimeMillis();

        decodeToEof();

        timer += System.currentTimeMillis() - t1;

        return packResult();
    }
}
