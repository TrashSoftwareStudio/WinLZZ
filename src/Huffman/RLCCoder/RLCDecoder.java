package Huffman.RLCCoder;

import Utility.Bytes;
import Utility.Util;

import java.util.ArrayList;
import java.util.Arrays;

public class RLCDecoder {

    private byte[] rlcMain;

    private String rlcBits;

    public RLCDecoder(byte[] rlcMain, String rlcBits) {
        this.rlcMain = rlcMain;
        this.rlcBits = rlcBits;
    }

    public byte[] Decode() {
        ArrayList<Byte> tempResult = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < rlcMain.length) {
            if (rlcMain[i] == (byte) 16) {
                byte repeat = tempResult.get(tempResult.size() - 1);
                String bitString = rlcBits.substring(j, j + 2);
                int runLength = Integer.parseInt(bitString, 2) + 3;
                for (int k = 0 ; k < runLength; k++) tempResult.add(repeat);
                j += 2;
            } else  if (rlcMain[i] == (byte) 17) {
                String bitString = rlcBits.substring(j, j + 3);
                int runLength = Integer.parseInt(bitString, 2) + 3;
                for (int k = 0 ; k < runLength; k++) tempResult.add((byte) 0);
                j += 3;
            } else  if (rlcMain[i] == (byte) 18) {
                String bitString = rlcBits.substring(j, j + 7);
                int runLength = Integer.parseInt(bitString, 2) + 11;
                for (int k = 0 ; k < runLength; k++) tempResult.add((byte) 0);
                j += 7;
            } else {
                tempResult.add(rlcMain[i]);
            }
            i += 1;
        }

        return Util.collectionToArray(tempResult);
    }
}
