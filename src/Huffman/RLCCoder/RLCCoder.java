package Huffman.RLCCoder;

import Utility.Bytes;
import Utility.Util;

import java.util.ArrayList;
import java.util.Arrays;

public class RLCCoder {

    private byte[] text;

    private byte[] mainResult;

    private String rlcBits;

    public RLCCoder(byte[] text) {
        this.text = text;
    }

    public void Encode() {

        ArrayList<Byte> result = new ArrayList<>();
        StringBuilder bits = new StringBuilder();

        int i = 0;
        while (i < text.length) {
            byte current = text[i];
            if (current == (byte) 0) {
                int runLength = 0;
                while (i + runLength + 1 < text.length && runLength < 137 && current == text[i + runLength + 1])
                    runLength += 1;
                if (runLength < 2) {
                    for (int j = 0; j < runLength + 1; j++) result.add(current);
                } else {
                    if (runLength < 10) {
                        result.add((byte) 17);
                        bits.append(Bytes.numberToBitString(runLength - 2, 3));
                    } else {
                        result.add((byte) 18);
                        bits.append(Bytes.numberToBitString(runLength - 10, 7));
                    }
                }
                i += runLength;
                i += 1;
            } else {
                int runLength = 0;
                while (i + runLength + 1 < text.length && current == text[i + runLength + 1]) runLength += 1;
                if (runLength < 3) {
                    for (int j = 0; j < runLength + 1; j++) result.add(current);
                } else {
                    result.add(text[i]);
                    int remain = runLength;
                    while (remain > 2) {
                        int currentLength = Math.min(remain, 6);
                        remain -= currentLength;

                        result.add((byte) 16);
                        bits.append(Bytes.numberToBitString(currentLength - 3, 2));
                    }
                    i -= remain;
                }
                i += runLength;
                i += 1;
            }
        }

        this.mainResult = Util.collectionToArray(result);
        this.rlcBits = bits.toString();
    }

    public byte[] getMainResult() {
        return mainResult;
    }

    public String getRlcBits() {
        return rlcBits;
    }
}
