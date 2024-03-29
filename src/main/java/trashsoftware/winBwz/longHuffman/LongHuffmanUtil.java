package trashsoftware.winBwz.longHuffman;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

public abstract class LongHuffmanUtil {

    static void addArrayToFreqMap(int[] array, int[] freqMap, int textBegin, int textLength) {
        for (int i = 0; i < textLength; i++) {
            freqMap[array[textBegin + i]] += 1;
        }
    }

    public static HuffmanNode generateHuffmanTree(int[] freqMap) {
        ArrayList<HuffmanNode> list = new ArrayList<>();
        for (int v = 0; v < freqMap.length; ++v) {
            int freq = freqMap[v];
            if (freq > 0) {
                HuffmanNode hn = new HuffmanNode(freq);
                hn.setValue(v);
                list.add(hn);
            }
        }
        if (list.size() == 1) {
            HuffmanNode root = new HuffmanNode(0);
            root.setLeft(list.remove(0));
            return root;
        }
        while (list.size() > 1) {
            Collections.sort(list);
            // Pop out two nodes with smallest frequency.
            HuffmanNode left = list.remove(list.size() - 1);
            HuffmanNode right = list.remove(list.size() - 1);
            HuffmanNode parent = new HuffmanNode(left.getFreq() + right.getFreq());
            parent.setLeft(left);
            parent.setRight(right);
            list.add(parent);
        }
        return list.get(0);
    }

    public static void generateCodeLengthMap(int[] lengthMap, HuffmanNode node, int length) {
        if (node != null) {
            if (node.isLeaf()) {
                lengthMap[node.getValue()] = length;
            } else {
                generateCodeLengthMap(lengthMap, node.getLeft(), length + 1);
                generateCodeLengthMap(lengthMap, node.getRight(), length + 1);
            }
        }
    }

    public static int[] generateCanonicalCode(int[] lengthCode) {
        int[] canonicalCode = new int[lengthCode.length];

        ArrayList<HuffmanTuple> tupleList = new ArrayList<>();

        for (int i = 0; i < lengthCode.length; ++i) {
            if (lengthCode[i] != 0) {
                HuffmanTuple current = new HuffmanTuple(i, lengthCode[i]);
                tupleList.add(current);
            }
        }
        Collections.sort(tupleList);

        canonicalCode[tupleList.get(0).getValue()] = 0;

        int code = 0;
        for (int i = 1; i < tupleList.size(); i++) {
            code = (code + 1) << (tupleList.get(i).getLength() - tupleList.get(i - 1).getLength());
            canonicalCode[tupleList.get(i).getValue()] = code;
        }
        return canonicalCode;
    }

    public static byte[] generateCanonicalCodeBlock(int[] lengthCode, int alphabetSize) {
        byte[] result = new byte[alphabetSize];
        for (int i = 0; i < alphabetSize; i++)
            result[i] = (byte) lengthCode[i];
        return result;
    }

    public static void generateLengthCode(byte[] canonicalMap, int[] dstTable) {
        for (int i = 0; i < canonicalMap.length; i++) {
            dstTable[i] = canonicalMap[i] & 0xff;
        }
    }

    public static void heightControl(int[] lengthMap, int[] freqMap, int maxHeight) {
        ArrayList<LengthTuple> list = new ArrayList<>();
        int max = 0;
        for (int k = 0; k < lengthMap.length; ++k) {
            int len = lengthMap[k];
            if (len > max) {
                max = len;
            }
            if (len > 0) list.add(new LengthTuple(k, len, freqMap[k]));
        }
        Collections.sort(list);

        int debt = getTotalDebt(list, maxHeight);
        repay(list, debt, maxHeight);
        for (LengthTuple lt : list) lengthMap[lt.getByte()] = lt.length;
    }

    private static int getTotalDebt(ArrayList<LengthTuple> list, int maxHeight) {
        double debt = 0;
        for (LengthTuple lt : list) {
            if (lt.length > maxHeight) {
                double num = Math.pow(2, (lt.length - maxHeight));
                debt += ((num - 1) / num);
                lt.length = maxHeight;
            }
        }
        return (int) Math.round(debt);
    }

    private static void repay(ArrayList<LengthTuple> list, int debt, int maxHeight) {
        while (debt > 0) {
            LengthTuple lt = list.get(getLastUnderLimit(list, maxHeight));
            int lengthDiff = maxHeight - lt.length;
            debt -= (int) Math.pow(2, (lengthDiff - 1));
            lt.length += 1;
        }
    }

    private static int getLastUnderLimit(ArrayList<LengthTuple> list, int maxHeight) {
        int i = list.size() - 1;
        while (list.get(i).length == maxHeight) i -= 1;
        return i;
    }

    public static void identicalMapOneLoop(int[] lengthCode,
                                           int[] canonicalCode,
                                           int plainSymbol,
                                           int shortCodeLen,
                                           int[] shortMapArr,
                                           int longCodeLen,
                                           int[] longMapArr) {
        identicalMapOneLoop(
                lengthCode,
                canonicalCode,
                plainSymbol,
                shortCodeLen,
                shortMapArr,
                longCodeLen,
                longMapArr,
                0,
                null
        );
    }

    public static void identicalMapOneLoop(int[] lengthCode,
                                           int[] canonicalCode,
                                           int plainSymbol,
                                           int shortCodeLen,
                                           int[] shortMapArr,
                                           int longCodeLen,
                                           int[] longMapArr,
                                           int maxCodeLen,
                                           Map<Integer, Integer> extraMap) {
        int len = lengthCode[plainSymbol];
        if (len > 0) {
            int code = canonicalCode[plainSymbol];
            if (len < shortCodeLen) {
                int sup_len = shortCodeLen - len;
                int sup_pow = 1 << sup_len;
                int res = code << sup_len;
                for (int j = 0; j < sup_pow; ++j) {
                    shortMapArr[res + j] = plainSymbol + 1;  // 0 reserved for not found
                }
            } else if (len == shortCodeLen) {
                shortMapArr[code] = plainSymbol + 1;  // 0 reserved for not found
            } else if (len < longCodeLen) {
                int sup_len = longCodeLen - len;
                int sup_pow = 1 << sup_len;
                int res = code << sup_len;
                for (int j = 0; j < sup_pow; ++j) {
                    if (maxCodeLen == 0)
                        longMapArr[res + j] = plainSymbol;
                    else
                        longMapArr[res + j] = plainSymbol + 1;  // 0 reserved for not found
                }
            } else if (len == longCodeLen) {
                if (maxCodeLen == 0)
                    longMapArr[code] = plainSymbol;
                else
                    longMapArr[code] = plainSymbol + 1;  // 0 reserved for not found
            } else if (len <= maxCodeLen) {
                extraMap.put(code, plainSymbol);
            } else {
                throw new RuntimeException("Code too long");
            }
        }
    }

    public static int hufDecompressorMem() {
        return (int) Math.pow(2, 16) * 4 +  // identical map
                300 * 4;  // length map
    }
}


