package trashsoftware.win_bwz.core.lzz2;

import trashsoftware.win_bwz.longHuffman.LongHuffmanUtil;
import trashsoftware.win_bwz.utility.Bytes;

import java.io.IOException;
import java.io.InputStream;

public class Lzz2HuffmanInputStream {

    private int maxCodeLen;
    private int average = 8;

    private InputStream bis;

    private int[] lengthCode;
    private int[] identicalMap;

    private byte[] buffer = new byte[1];

    private int bits;
    private int bitPos;
    private int averageAndEr;
    private int bigMapLongerAndEr;
    private int bigMapLonger;

    private boolean streamEnds = false;

    public Lzz2HuffmanInputStream(int[] map, InputStream bis) {
        this.bis = bis;
        lengthCode = map;
        calculateCodeLengths();
        int[] canonicalCode = LongHuffmanUtil.generateCanonicalCode(lengthCode);
        generateIdenticalMap(lengthCode, canonicalCode);
    }

    private void calculateCodeLengths() {
        for (int len : lengthCode) {
            if (len > 0) {
                if (len > maxCodeLen) maxCodeLen = len;
            }
        }
        if (average > maxCodeLen) {
            average = maxCodeLen;
        }
        bigMapLonger = maxCodeLen - average;
        averageAndEr = Bytes.getAndEr(average);
        bigMapLongerAndEr = Bytes.getAndEr(bigMapLonger);
    }

    private void generateIdenticalMap(int[] lengthCode, int[] canonicalCode) {
        identicalMap = new int[1 << maxCodeLen];

        for (int i = 0; i < lengthCode.length; ++i) {
            LongHuffmanUtil.identicalMapOneLoop(
                    lengthCode,
                    canonicalCode,
                    i,
                    average,
                    identicalMap,
                    maxCodeLen,
                    identicalMap
            );
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
        int code = identicalMap[index];

        if (code == 0) {  // not in short map
            loadBits(bigMapLonger);
            index <<= bigMapLonger;
            index |= ((bits >> (bitPos - bigMapLonger)) & bigMapLongerAndEr);
            bitPos -= bigMapLonger;
            code = identicalMap[index];
            codeLen = lengthCode[code];
            bitPos += (maxCodeLen - codeLen);
        } else {
            code -= 1;
            codeLen = lengthCode[code];
            bitPos += (average - codeLen);
        }
        return code;
    }

    public void close() throws IOException {
        bis.close();
    }
}
