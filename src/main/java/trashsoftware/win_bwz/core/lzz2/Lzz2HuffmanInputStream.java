package trashsoftware.win_bwz.core.lzz2;

import trashsoftware.win_bwz.longHuffman.LongHuffmanUtil;
import trashsoftware.win_bwz.utility.Bytes;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import static trashsoftware.win_bwz.longHuffman.LongHuffmanUtil.identicalMapOneLoop;

public class Lzz2HuffmanInputStream {

//    private static final int BUFFER_SIZE = 8192;

    private int maxCodeLen;
    private int average = 8;

    private InputStream bis;

    private int[] lengthCode;
    private int[] shortMapArr;
    private int[] longMapArr;

    private byte[] buffer = new byte[1];
//    private int bufferIndex;

    private int bits;
    private int bitPos;
    private int averageAndEr;
    private int bigMapLongerAndEr;
    private int bigMapLonger;

    private boolean streamEnds = false;

    public Lzz2HuffmanInputStream(byte[] map, InputStream bis) {
        this.bis = bis;
        lengthCode = recoverLengthCode(map);
        int[] canonicalCode = LongHuffmanUtil.generateCanonicalCode(lengthCode);
        generateIdenticalMap(lengthCode, canonicalCode);
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
        bigMapLonger = maxCodeLen - average;
        averageAndEr = Bytes.getAndEr(average);
        bigMapLongerAndEr = Bytes.getAndEr(bigMapLonger);
        return lengthCode;
    }

    private void generateIdenticalMap(int[] lengthCode, int[] canonicalCode) {
        shortMapArr = new int[1 << average];
        longMapArr = new int[1 << maxCodeLen];

        for (int i = 0; i < lengthCode.length; ++i) {
            identicalMapOneLoop(lengthCode, canonicalCode, i, average, shortMapArr, maxCodeLen, longMapArr);
        }
    }

    private void loadBits(int leastPos) throws IOException {

        while (bitPos < leastPos) {
            bitPos += 8;
            bits <<= 8;
            if (bis.read(buffer) <= 0) {
                streamEnds = true;
            } else {
                bits |= (buffer[0] & 0xff);
            }
        }
    }

    public int readNext() throws IOException {
        loadBits(average);
        int index = (bits >> (bitPos - average)) & averageAndEr;
        bitPos -= average;

        int codeLen;
        int code = shortMapArr[index];
        if (code == 0) {  // not in short map
            loadBits(bigMapLonger);
            index <<= bigMapLonger;
            index |= ((bits >> (bitPos - bigMapLonger)) & bigMapLongerAndEr);
            bitPos -= bigMapLonger;
            code = longMapArr[index];
            codeLen = lengthCode[code];
            bitPos += (maxCodeLen - codeLen);
        } else {
            code -= 1;
            codeLen = lengthCode[code];
            bitPos += (average - codeLen);
        }
//            result[currentIndex++] = code;
//        currentLen++;
//        fos.write(code);
        return code;
    }

    public void close() throws IOException {
        bis.close();
    }
}
