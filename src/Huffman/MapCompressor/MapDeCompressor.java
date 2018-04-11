package Huffman.MapCompressor;

import Huffman.HuffmanCompressor;
import Utility.Bytes;

import java.util.HashMap;

public class MapDeCompressor {

    private String cclBits;

    private String sqBits;

    private int cclNum;

    private int lengthRem;

    public MapDeCompressor(byte[] csq) {

        String bits = Bytes.bytesToString(csq);

        cclNum = Integer.parseInt(bits.substring(0, 4), 2) + 4;
        lengthRem = Integer.parseInt(bits.substring(4, 7), 2);
        cclBits = bits.substring(7, 7 + cclNum * 3);
        StringBuilder builder = new StringBuilder(bits.substring(7 + cclNum * 3));
        while (builder.length() % 8 != lengthRem) {
            builder.deleteCharAt(builder.length() - 1);
        }
        sqBits = builder.toString();
    }

    private byte[] swapCCl(byte[] ccl) {
        byte[] origCCL = new byte[19];
        for (int i = 0; i < 19; i++) {
            origCCL[MapCompressor.positions[i]] = ccl[i];
        }
        return origCCL;
    }

    private static HashMap<Byte, Integer> recoverLengthCode(byte[] ccl) {
        HashMap<Byte, Integer> lengthCode = new HashMap<>();
        for (int i = 0; i < 19; i ++) {
            int length = ccl[i] & 0xff;
            if (length != 0) {
                lengthCode.put((byte) i, length);
            }
        }
        return lengthCode;
    }

    private static HashMap<String, Byte> invertMap(HashMap<Byte, String> map) {
        HashMap<String, Byte> invertedMap = new HashMap<>();
        for (byte b : map.keySet()) {
            invertedMap.put(map.get(b), b);
        }
        return invertedMap;
    }

    private static byte[] decode(String bits, HashMap<String, Byte> decodeMap, int maxMapLen) {

        StringBuilder temp = new StringBuilder();
        byte[] result = new byte[65536];

        int i = 0;
        int j = 0;
        while (i < bits.length()) {
            temp.append(bits.charAt(i));
            i += 1;
            Byte b = decodeMap.get(temp.toString());
            if (b != null) {
                result[j] = b;
                j += 1;
                temp.setLength(0);
            }
        }
        byte[] rtn = new byte[j];
        System.arraycopy(result, 0, rtn, 0, j);

        return rtn;
    }

    public byte[] Uncompress(int maxMapLen) {
        byte[] CCL = new byte[cclNum];

        for (int i = 0; i < cclNum; i++) {
            CCL[i] = (byte) Integer.parseInt(cclBits.substring(i * 3, i * 3 + 3), 2);
        }
        byte[] fullCCL = new byte[19];
        System.arraycopy(CCL, 0, fullCCL, 0, cclNum);
        byte[] origCCL = swapCCl(fullCCL);

        HashMap<Byte, Integer> lengthCode = recoverLengthCode(origCCL);
        HashMap<Byte, String> canonicalMap = HuffmanCompressor.generateCanonicalCode(lengthCode);
        HashMap<String, Byte> decodeMap = invertMap(canonicalMap);

        return decode(sqBits, decodeMap, maxMapLen);
    }
}
