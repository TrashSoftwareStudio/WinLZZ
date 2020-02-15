package trashsoftware.win_bwz.huffman.BwzMapCompressor;

import trashsoftware.win_bwz.huffman.HuffmanCompressorBase;
import trashsoftware.win_bwz.utility.Bytes;

import java.util.Arrays;
import java.util.HashMap;

/**
 * A decompressor that uncompress the canonical huffman map compressed by a {@code MapCompressor}.
 *
 * @author zbh
 * @since 0.4
 */
public class BwzMapDeCompressor {

    private String cclBits;
    private String sqBits;
    private int cclNum;

    /**
     * Creates a new instance of a MapDeCompressor Object.
     *
     * @param csq the compressed text.
     */
    public BwzMapDeCompressor(byte[] csq) {
        String bits = Bytes.bytesToString(csq);
        cclNum = Integer.parseInt(bits.substring(0, 5), 2);
        if (cclNum == 0) cclNum = 32;
        int lengthRem = Integer.parseInt(bits.substring(5, 8), 2);
        cclBits = bits.substring(8, 8 + cclNum * 4);
        StringBuilder builder = new StringBuilder(bits.substring(8 + cclNum * 4));
        while (builder.length() % 8 != lengthRem) builder.deleteCharAt(builder.length() - 1);
        sqBits = builder.toString();
    }

    private static HashMap<Byte, Integer> recoverLengthCode(byte[] ccl, int alphabetSize) {
        HashMap<Byte, Integer> lengthCode = new HashMap<>();
        for (int i = 0; i < alphabetSize; i++) {
            int length = ccl[i] & 0xff;
            if (length != 0) lengthCode.put((byte) i, length);
        }
        return lengthCode;
    }

    private static HashMap<String, Byte> invertMap(HashMap<Byte, String> map) {
        HashMap<String, Byte> invertedMap = new HashMap<>();
        for (byte b : map.keySet()) invertedMap.put(map.get(b), b);
        return invertedMap;
    }

    private static byte[] decode(String bits, HashMap<String, Byte> decodeMap, int maxMapLen) {
        StringBuilder temp = new StringBuilder();
        byte[] result = new byte[maxMapLen];

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

    /**
     * Returns the original text.
     *
     * @param maxMapLen    the max possible length of original text.
     * @param alphabetSize the alphabet size of map
     * @return the original text.
     */
    public byte[] Uncompress(int maxMapLen, int alphabetSize) {
        byte[] CCL = new byte[cclNum];

        for (int i = 0; i < cclNum; i++) CCL[i] = (byte) Integer.parseInt(cclBits.substring(i * 4, i * 4 + 4), 2);
        byte[] fullCCL = new byte[alphabetSize];
        System.arraycopy(CCL, 0, fullCCL, 0, cclNum);
        byte[] origCCL = fullCCL;

        HashMap<Byte, Integer> lengthCode = recoverLengthCode(origCCL, alphabetSize);
        HashMap<Byte, String> canonicalMap = HuffmanCompressorBase.generateCanonicalCode(lengthCode);
        HashMap<String, Byte> decodeMap = invertMap(canonicalMap);

        byte[] res = decode(sqBits, decodeMap, maxMapLen);
//        if (cclNum > 18) System.out.println(Arrays.toString(res));
        return res;
    }
}
