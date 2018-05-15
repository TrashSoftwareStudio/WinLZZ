package WinLzz.LongHuffman;

import WinLzz.Utility.Bytes;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;

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
    private short endSig;

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
    private HashMap<Integer, Short> shortMap = new HashMap<>();

    /**
     * The huffman code table that records all codes that are shorter than or equal to {@code maxCodeLen}, but all
     * codes shorter than {@code maxCodeLen} is extended by all possible combination of 0's and 1's until they
     * reaches the length {@code maxCodeLen}.
     */
    private HashMap<Integer, Short> longMap = new HashMap<>();

    /**
     * The table that records all huffman symbol and their corresponding code length.
     */
    private HashMap<Short, Integer> lengthMap = new HashMap<>();

    private long compressedBitLength;

    private boolean isTerminated;

    private short[] result;

    private int currentIndex;

    private StringBuilder builder = new StringBuilder();

    /**
     * Creates a new {@code LongHuffmanInputStream} instance.
     *
     * @param fc           the input stream file channel.
     * @param alphabetSize the alphabet size.
     * @param maxLength    the maximum length of each part of huffman text.
     */
    public LongHuffmanInputStream(FileChannel fc, int alphabetSize, int maxLength) {
        this.fc = fc;
        this.alphabetSize = alphabetSize;
        this.result = new short[maxLength];
    }

    private HashMap<Short, Integer> recoverLengthCode(byte[] map) {
        HashMap<Short, Integer> lengthCode = new HashMap<>();
        for (int i = 0; i < alphabetSize; i++) {
            int len = map[i] & 0xff;
            if (len != 0) {
                lengthCode.put((short) i, len);
                if (len > maxCodeLen) maxCodeLen = len;
            }
        }
        if (average > maxCodeLen) {
            average = maxCodeLen;
        }
        return lengthCode;
    }

    private void generateIdenticalMap(HashMap<Short, String> origMap) {
        for (short value : origMap.keySet()) {
            String s = origMap.get(value);
            lengthMap.put(value, s.length());

            if (s.length() > average) {
                int len = maxCodeLen - s.length();
                if (len == 0) {
                    longMap.put(Integer.parseInt(s, 2), value);
                } else {
                    for (int i = 0; i < Math.pow(2, len); i++) {
                        String key = s + Bytes.numberToBitString(i, len);
                        longMap.put(Integer.parseInt(key, 2), value);
                    }
                }
            } else {
                int len = average - s.length();
                if (len == 0) {
                    shortMap.put(Integer.parseInt(s, 2), value);
                } else {
                    for (int i = 0; i < Math.pow(2, len); i++) {
                        String key = s + Bytes.numberToBitString(i, len);
                        shortMap.put(Integer.parseInt(key, 2), value);
                    }
                }
            }
        }
    }

    private void uncompressToArray() {
        int i = 0;
        while (builder.length() - i > maxCodeLen) {
            int index = Integer.parseInt(builder.substring(i, i + average), 2);
            short value;
            int len;
            if (shortMap.containsKey(index)) {
                value = shortMap.get(index);
                len = lengthMap.get(value);
            } else {
                value = longMap.get(Integer.parseInt(builder.substring(i, i + maxCodeLen), 2));
                len = lengthMap.get(value);
            }
            compressedBitLength += len;
            if (value == endSig) {
                while (compressedBitLength % 8 != 0) compressedBitLength += 1;
                isTerminated = true;
                break;
            }
            result[currentIndex] = value;
            currentIndex += 1;
            i += len;
        }
        String rem = builder.substring(i);
        builder.setLength(0);
        builder.append(rem);
    }

    private void unCompressLastText() {
        builder.append(Bytes.charMultiply('0', maxCodeLen * 8));
        int i = 0;
        while (true) {
            int index = Integer.parseInt(builder.substring(i, i + average), 2);
            short value;
            int len;
            if (shortMap.containsKey(index)) {
                value = shortMap.get(index);
                len = lengthMap.get(value);
            } else {
                value = longMap.get(Integer.parseInt(builder.substring(i, i + maxCodeLen), 2));
                len = lengthMap.get(value);
            }
            compressedBitLength += len;
            if (value == endSig) {
                isTerminated = true;
                break;
            }
            result[currentIndex] = value;
            currentIndex += 1;
            i += len;
        }
    }

    private void unCompress() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        int read;
        while ((read = fc.read(buffer)) > 0) {
            buffer.flip();
            for (int i = 0; i < read; i++) builder.append(Bytes.byteToBitString(buffer.get(i)));
            buffer.clear();
            uncompressToArray();
            if (isTerminated) {
                fc.position(getCompressedLength());
                break;
            }
        }
        if (!isTerminated) unCompressLastText();
        if (!isTerminated) throw new IOException("Cannot find EOF character");
    }

    private long getCompressedLength() {
        if (compressedBitLength % 8 == 0) return compressedBitLength / 8;
        else return compressedBitLength / 8 + 1;
    }

    /**
     * Read and uncompress the huffman compression file until reaches the next endSig.
     *
     * @param map    Canonical huffman map for this read action.
     * @param endSig The EOF character.
     * @return The uncompressed text.
     * @throws IOException If the file is not readable.
     */
    public short[] read(byte[] map, short endSig) throws IOException {
        this.endSig = endSig;

        currentIndex = 0;
        builder.setLength(0);
        isTerminated = false;
        shortMap.clear();
        longMap.clear();
        lengthMap.clear();

        HashMap<Short, Integer> lengthCode = recoverLengthCode(map);
        HashMap<Short, String> huffmanCode = LongHuffmanUtil.generateCanonicalCode(lengthCode);
        generateIdenticalMap(huffmanCode);
        unCompress();

        short[] rtn = new short[currentIndex];
        System.arraycopy(result, 0, rtn, 0, currentIndex);
        return rtn;
    }
}
