package trashsoftware.win_bwz.huffman.MapCompressor;

import trashsoftware.win_bwz.longHuffman.LongHuffmanUtil;
import trashsoftware.win_bwz.utility.FileBitInputStream;

import java.io.IOException;
import java.util.HashMap;

public abstract class MapDecompressorBase {

    protected int cclNum;
    protected int lengthRemainder;
    protected FileBitInputStream fbi;

    protected static HashMap<Integer, Integer> invertMap(int[] canonicalMap, int[] lengthMap) {
        HashMap<Integer, Integer> invertedMap = new HashMap<>();
        for (int b = 0; b < canonicalMap.length; ++b) {
            if (lengthMap[b] > 0) {  // contains this code
                int code = canonicalMap[b];
                invertedMap.put(code, b);
            }
        }
        return invertedMap;
    }

    protected byte[] decode(HashMap<Integer, Integer> decodeMap, int[] lengthCode, int maxMapLen) throws IOException {
        byte[] result = new byte[maxMapLen];

        int resLen = 0;
        boolean terminate = false;
        while (!terminate) {
            int bits = 0;
            int bitLen = 0;

            while (true) {
                bits <<= 1;
                int newBit = fbi.read();
                if (newBit == 2) {
                    terminate = true;
                    break;
                }
                bits |= newBit;
                bitLen += 1;
                Integer code = decodeMap.get(bits);
                if (code != null && lengthCode[code] == bitLen) {
                    result[resLen++] = code.byteValue();
                    break;
                }
            }
        }
        while (resLen % 8 != lengthRemainder) resLen--;
        byte[] rtn = new byte[resLen];
        System.arraycopy(result, 0, rtn, 0, resLen);
        return rtn;
    }

    /**
     * Returns the original text.
     *
     * @param maxMapLen    the max possible length of original text.
     * @param alphabetSize the alphabet size
     * @return the original text.
     */
    public byte[] Uncompress(int maxMapLen, int alphabetSize) throws IOException {
        int[] CCL = new int[cclNum];

        int eachCclLen = getEachCclLength();
        for (int i = 0; i < cclNum; i++) {
            CCL[i] = fbi.read(eachCclLen);
        }
        int[] fullCCL = new int[alphabetSize];
        System.arraycopy(CCL, 0, fullCCL, 0, cclNum);
//        int[] origCCL;  // this is the length map
//        if (swap) origCCL = swapCCl(fullCCL);
//        else origCCL = fullCCL;

        int[] canonicalMap = LongHuffmanUtil.generateCanonicalCode(fullCCL);
        HashMap<Integer, Integer> decodeMap = invertMap(canonicalMap, fullCCL);

        byte[] res = decode(decodeMap, fullCCL, maxMapLen);
        fbi.close();
        return res;
    }

    protected abstract int getEachCclLength();
}
