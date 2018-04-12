package Huffman;

import Utility.Bytes;

import java.util.HashMap;

public class ShortHuffmanDeCompressor {

    private byte[] text;

    public ShortHuffmanDeCompressor(byte[] text) {
        this.text = text;
    }

    private static HashMap<Byte, Integer> recoverLengthCode(String mapBits) {
        HashMap<Byte, Integer> lengthCode = new HashMap<>();
        int i = 0;
        for (int j = 0; j < 64; j += 4) {
            int length = Integer.parseInt(mapBits.substring(j, j + 4), 2);
            if (length != 0) lengthCode.put((byte) i, length);
            i += 1;
        }
        return lengthCode;
    }

    private static HashMap<String, Byte> invertMap(HashMap<Byte, String> map) {
        HashMap<String, Byte> invertedMap = new HashMap<>();
        for (byte b : map.keySet()) invertedMap.put(map.get(b), b);
        return invertedMap;
    }

    private static byte[] decode(String bits, HashMap<String, Byte> decodeMap) {

        StringBuilder temp = new StringBuilder();
        byte[] result = new byte[256];

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

    public byte[] Uncompress() {
        StringBuilder builder = new StringBuilder();
        for (byte b : text) builder.append(Bytes.byteToBitString(b));
        if (builder.charAt(0) == '0') {
            byte b = Bytes.bitStringToByte(builder.substring(1, 9));
            int repeat = Integer.parseInt(builder.substring(9, 18), 2);
            byte[] result = new byte[repeat];
            for (int i = 0; i < repeat; i++) result[i] = b;
            return result;
        }
        int lengthRemainder = Integer.parseInt(builder.substring(1, 4), 2);
        HashMap<Byte, Integer> lengthCode = recoverLengthCode(builder.substring(4, 68));
        HashMap<Byte, String> huffmanCode = HuffmanCompressor.generateCanonicalCode(lengthCode);
        HashMap<String, Byte> decodeMap = invertMap(huffmanCode);

        while (builder.length() % 8 != lengthRemainder) builder.deleteCharAt(builder.length() - 1);

        return decode(builder.substring(68), decodeMap);
    }
}
