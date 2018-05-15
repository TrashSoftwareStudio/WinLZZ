package WinLzz.LongHuffman;

import WinLzz.Utility.Bytes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

abstract class LongHuffmanUtil {

    static void addArrayToFreqMap(short[] array, HashMap<Short, Integer> freqMap, int range) {
        for (int i = 0; i < range; i++) {
            short b = array[i];
            if (freqMap.containsKey(b)) freqMap.put(b, freqMap.get(b) + 1);
            else freqMap.put(b, 1);
        }
    }

    static HuffmanNode generateHuffmanTree(HashMap<Short, Integer> freqMap) {
        ArrayList<HuffmanNode> list = new ArrayList<>();
        for (short key : freqMap.keySet()) {
            HuffmanNode hn = new HuffmanNode(freqMap.get(key));
            hn.setValue(key);
            list.add(hn);
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

    static void generateCodeLengthMap(HashMap<Short, Integer> lengthMap, HuffmanNode node, int length) {
        if (node != null) {
            if (node.isLeaf()) {
                lengthMap.put(node.getValue(), length);
            } else {
                generateCodeLengthMap(lengthMap, node.getLeft(), length + 1);
                generateCodeLengthMap(lengthMap, node.getRight(), length + 1);
            }
        }
    }

    static HashMap<Short, String> generateCanonicalCode(HashMap<Short, Integer> lengthCode) {
        HashMap<Short, String> canonicalCode = new HashMap<>();

        ArrayList<HuffmanTuple> tupleList = new ArrayList<>();

        for (short key : lengthCode.keySet()) {
            HuffmanTuple current = new HuffmanTuple(key, lengthCode.get(key));
            tupleList.add(current);
        }
        Collections.sort(tupleList);

        HuffmanTuple first = new HuffmanTuple(tupleList.get(0).getValue(), tupleList.get(0).getLength());
        canonicalCode.put(first.getValue(), Bytes.charMultiply('0', first.getLength()));

        int code = 0;
        for (int i = 1; i < tupleList.size(); i++) {
            code = (code + 1) << (tupleList.get(i).getLength() - tupleList.get(i - 1).getLength());
            String co = Integer.toBinaryString(code);
            if (co.length() < tupleList.get(i).getLength())
                co = Bytes.charMultiply('0', tupleList.get(i).getLength() - co.length()) + co;
            canonicalCode.put(tupleList.get(i).getValue(), co);
        }
        return canonicalCode;
    }

    static byte[] generateCanonicalCodeBlock(HashMap<Short, Integer> lengthCode, int alphabetSize) {
        byte[] result = new byte[alphabetSize];
        for (int i = 0; i < alphabetSize; i++)
            if (lengthCode.containsKey((short) i)) result[i] = (byte) (int) lengthCode.get((short) i);
            else result[i] = (byte) 0;
        return result;
    }

    static void addCompressed(short[] buffer, int range, StringBuilder builder, HashMap<Short, String> huffmanCode) {
        for (int i = 0; i < range; i++) {
            short b = buffer[i];
            builder.append(huffmanCode.get(b));
        }
    }

    static HashMap<Short, Integer> generateLengthCode(byte[] canonicalMap) {
        HashMap<Short, Integer> lengthCode = new HashMap<>();
        for (int i = 0; i < canonicalMap.length; i++) {
            int len = canonicalMap[i] & 0xff;
            if (len != 0) {
                lengthCode.put((short) i, len);
            }
        }
        return lengthCode;
    }

    static void heightControl(HashMap<Short, Integer> codeLength, HashMap<Short, Integer> freqMap, int maxHeight) {
        ArrayList<LengthTuple> list = new ArrayList<>();
        for (short key : codeLength.keySet()) list.add(new LengthTuple(key, codeLength.get(key), freqMap.get(key)));
        Collections.sort(list);

        int debt = getTotalDebt(list, maxHeight);
        repay(list, debt, maxHeight);
        for (LengthTuple lt : list) codeLength.put(lt.getByte(), lt.length);
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
}


/**
 * A tuple of a code length in the huffman code, implements the interface {@code Comparable}.
 * <p>
 * This class is used for controlling the code length of a huffman table.
 *
 * @author zbh
 * @see java.lang.Comparable
 * @since 0.5.2
 */
class LengthTuple implements Comparable<LengthTuple> {

    private short b;

    int length;

    private int freq;

    /**
     * Creates a new instance of {@code LengthTuple}.
     *
     * @param b      the value
     * @param length the code length
     * @param freq   the occurrences of <code>b</code> in the original text
     */
    LengthTuple(short b, int length, int freq) {
        this.b = b;
        this.length = length;
        this.freq = freq;
    }

    /**
     * Returns the value.
     *
     * @return the value
     */
    public short getByte() {
        return b;
    }

    /**
     * Compares this {@code LengthTuple} with another {@code LengthTuple}.
     *
     * @param o the {@code LengthTuple} to be compared with this {@code LengthTuple}
     * @return {@code 1} if this {@code LengthTuple} is greater than <code>o</code>, {@code -1} if smaller,
     * {@code 0} if equals
     */
    @Override
    public int compareTo(LengthTuple o) {
        return Integer.compare(o.freq, freq);
    }
}


/**
 * A node of a huffman tree, where the node implements the interface {@code Comparable}.
 * <p>
 * Each {@code HuffmanNode} instance records a symbol or an internal node and its frequency.
 *
 * @author zbh
 * @see java.lang.Comparable
 * @since 0.5
 */
class HuffmanNode implements Comparable<HuffmanNode> {

    private int freq;

    private short value;

    private HuffmanNode left;

    private HuffmanNode right;

    /**
     * Creates a new {@code HuffmanNode} instance.
     *
     * @param freq the occurrence times of the representation of this node
     */
    HuffmanNode(int freq) {
        this.freq = freq;
    }

    /**
     * Sets uo the value represented by this {@code HuffmanNode}.
     *
     * @param value the value
     */
    public void setValue(short value) {
        this.value = value;
    }

    /**
     * Sets up the left child of this {@code HuffmanNode}.
     *
     * @param left the left child of this {@code HuffmanNode}
     */
    void setLeft(HuffmanNode left) {
        this.left = left;
    }

    /**
     * Sets up the right child of this {@code HuffmanNode}.
     *
     * @param right the right child of this {@code HuffmanNode}
     */
    void setRight(HuffmanNode right) {
        this.right = right;
    }

    /**
     * Returns the value represented by this {@code HuffmanNode}.
     *
     * @return the value represented by this {@code HuffmanNode}
     */
    public short getValue() {
        return value;
    }

    HuffmanNode getLeft() {
        return left;
    }

    HuffmanNode getRight() {
        return right;
    }

    int getFreq() {
        return freq;
    }

    /**
     * Returns {@code true} if and only if this {@code HuffmanNode} does not have neither left child nor right child.
     *
     * @return {@code true} if and only if this {@code HuffmanNode} does not have any children
     */
    boolean isLeaf() {
        return left == null && right == null;
    }

    /**
     * Compares this {@code HuffmanNode} with another {@code HuffmanNode}.
     * <p>
     * This method returns 1 if this {@code HuffmanNode} has smaller {@code freq} than <code>o</code>'s,
     * -1 if greater, 0 if equals.
     *
     * @param o the {@code HuffmanNode} to be compared with this {@code HuffmanNode}
     * @return {@code 1} if this {@code HuffmanNode} has smaller {@code freq} than <code>o</code>'s,
     * {@code -1} if greater, {@code 0} if equals.
     */
    @Override
    public int compareTo(HuffmanNode o) {
        return Integer.compare(o.freq, freq);
    }
}


/**
 * A tuple of a huffman code, implements the interface {@code Comparable}.
 *
 * @author zbh
 * @since 0.5
 */
class HuffmanTuple implements Comparable<HuffmanTuple> {

    private short value;

    private int codeLength;

    /**
     * Creates a new {@code HuffmanTuple} instance.
     *
     * @param value      the value of this {@code HuffmanTuple}
     * @param codeLength the length of huffman code of this {@code HuffmanTuple}
     */
    HuffmanTuple(short value, int codeLength) {
        this.value = value;
        this.codeLength = codeLength;
    }

    /**
     * Returns the code length.
     *
     * @return the code length
     */
    public int getLength() {
        return codeLength;
    }

    /**
     * Returns the value.
     *
     * @return the value
     */
    public short getValue() {
        return value;
    }

    /**
     * Compares this {@code HuffmanTuple} to another {@code HuffmanTuple} by, primarily the code length,
     * and secondarily the lexicographical order of the <code>value</code>
     *
     * @param o the {@code HuffmanTuple} to be compared with this {@code HuffmanTuple}
     * @return {@code 1} if this {@code HuffmanTuple} is greater than <code>o</code>, {@code -1} if smaller,
     * {@code 0} if equals
     */
    @Override
    public int compareTo(HuffmanTuple o) {
        int lengthCmp = Integer.compare(codeLength, o.codeLength);
        if (lengthCmp == 0) return Short.compare(value, o.value);
        else return lengthCmp;
    }
}


