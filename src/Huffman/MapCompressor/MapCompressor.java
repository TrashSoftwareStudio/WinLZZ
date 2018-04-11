package Huffman.MapCompressor;

import Huffman.HuffmanCompressor;
import Huffman.HuffmanNode;
import Utility.Bytes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class MapCompressor {

    private byte[] map;

    private int maxHeight = 7;

    static final int[] positions = new int[]{16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15};

    private HashMap<Byte, Integer> freqMap = new HashMap<>();

    public MapCompressor(byte[] mapBytes) {
        this.map = mapBytes;
    }

    private static byte[] generateMap(HashMap<Byte, Integer> lengthMap) {
        byte[] cmpMap = new byte[19];
        for (int i = 0; i < 19; i++) {
            if (lengthMap.containsKey((byte) i)) {
                cmpMap[i] = (byte) (int) (lengthMap.get((byte) i));
            } else {
                cmpMap[i] = (byte) 0;
            }
        }
        return cmpMap;
    }

    private static String compressText(HashMap<Byte, String> huffmanCode, byte[] text) {
        StringBuilder builder = new StringBuilder();
        HuffmanCompressor.addCompressed(text, text.length, builder, huffmanCode);
        return builder.toString();
    }

    private void heightControl(HashMap<Byte, Integer> codeLength) {
        ArrayList<Huffman.LengthTuple> list = new ArrayList<>();
        for (byte key : codeLength.keySet()) {
            list.add(new Huffman.LengthTuple(key, codeLength.get(key), freqMap.get(key)));
        }
        Collections.sort(list);

        int debt = getTotalDebt(list);
        repay(list, debt);
        for (Huffman.LengthTuple lt : list) {
            codeLength.put(lt.getByte(), lt.length);
        }
    }

    private int getTotalDebt(ArrayList<Huffman.LengthTuple> list) {
        double debt = 0;
        for (Huffman.LengthTuple lt : list) {
            if (lt.length > maxHeight) {
                double num = Math.pow(2, (lt.length - maxHeight));
                debt += ((num - 1) / num);
                lt.length = maxHeight;
            }
        }
        return (int) Math.round(debt);
    }

    private void repay(ArrayList<Huffman.LengthTuple> list, int debt) {
        while (debt > 0) {
            Huffman.LengthTuple lt = list.get(HuffmanCompressor.getLastUnderLimit(list, maxHeight));
            int lengthDiff = maxHeight - lt.length;
            debt -= (int) Math.pow(2, (lengthDiff - 1));
            lt.length += 1;
        }
    }

    private byte[] swapCcl(byte[] ccl) {
        byte[] cclResult = new byte[19];
        for (int i = 0; i < 19; i++) {
            cclResult[i] = ccl[positions[i]];
        }
        return cclResult;
    }

    private byte[] cclTruncate(byte[] ccl) {
        int i = 18;
        while (ccl[i] == (byte) 0) {
            i -= 1;
        }
        i += 1;
        byte[] shortCCL = new byte[i];
        System.arraycopy(ccl, 0, shortCCL, 0, i);
        return shortCCL;
    }

    public byte[] Compress() {

        HuffmanCompressor.addArrayToFreqMap(map, freqMap, map.length);
        HashMap<Byte, Integer> codeLengthMap = new HashMap<>();
        HuffmanNode rootNode = HuffmanCompressor.generateHuffmanTree(freqMap);
        HuffmanCompressor.generateCodeLengthMap(codeLengthMap, rootNode, 0);
        heightControl(codeLengthMap);
        HashMap<Byte, String> huffmanCode = HuffmanCompressor.generateCanonicalCode(codeLengthMap);

//        System.out.println(huffmanCode);

        byte[] origCCL = generateMap(codeLengthMap);
//        System.out.println(Arrays.toString(origCCL));
        byte[] swappedCCL = swapCcl(origCCL);
        byte[] CCL = cclTruncate(swappedCCL);

        String cmpMap = compressText(huffmanCode, map);

        String hcLen = Bytes.numberToBitString(CCL.length - 4, 4);
        StringBuilder builder = new StringBuilder();
        builder.append(hcLen);
        builder.append(Bytes.numberToBitString(cmpMap.length() % 8, 3));  // Record length remainder.

        for (byte b : CCL) {
            builder.append(Bytes.numberToBitString((b & 0xff), 3));
        }

        builder.append(cmpMap);

        return Bytes.stringBuilderToBytesFull(builder);
    }

}
