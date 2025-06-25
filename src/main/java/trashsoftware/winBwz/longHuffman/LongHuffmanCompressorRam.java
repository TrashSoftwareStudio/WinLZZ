package trashsoftware.winBwz.longHuffman;

import trashsoftware.winBwz.utility.Util;

import java.util.*;

/**
 * A huffman compression program that all operations take places in the random access memory.
 * <p>
 * This compressor takes an alphabet that has size at most 32768.
 *
 * @author zbh
 * @since 0.5
 */
public class LongHuffmanCompressorRam {

    private static final int OPTIMAL_BLOCK_SIZE = 16384;
    private static final int ESTIMATE_CMP_MAP_LENGTH = 36;

    private final int[] fullText;
    private int textBegin;
    private int textSize;
    private final int alphabetSize;
    private int[] codeTable;
    private int[] lengthTable;

    private int[] lastFreqTable;
    private int[] lastLengthTable;

    /**
     * The maximum height (depth) of the huffman tree.
     */
    private static int maxHeight = 29;  // map alphabet size: 30

    /**
     * The signal that marks the EOF
     */
    private final int endSig;

    /**
     * Creates a new {@code LongHuffmanCompressorRam} instance.
     * <p>
     * LongHuffmanCompressor deals text from range 0 to 32767.
     * This compressor works completely in random access memory.
     *
     * @param fullText     the total text
     * @param alphabetSize the alphabet size, with endSig and other included
     * @param endSig       the mark of the end of stream.
     */
    public LongHuffmanCompressorRam(int[] fullText, int alphabetSize, int endSig) {
        this.fullText = fullText;
        this.endSig = endSig;
        this.alphabetSize = alphabetSize;
    }

    public byte[] getMap() {
        codeTable = LongHuffmanUtil.generateCanonicalCode(lengthTable);
        return LongHuffmanUtil.generateCanonicalCodeBlock(lengthTable, lengthTable.length);
    }
    
    public byte[] setBlock(HuffmanBlock block) {
        lengthTable = block.lengthTable;
        textBegin = block.textBegin;
        textSize = block.textLength;
        codeTable = LongHuffmanUtil.generateCanonicalCode(lengthTable);
//        System.out.printf("Set block: %d, %d\n", textBegin, textBegin + textSize);
        return LongHuffmanUtil.generateCanonicalCodeBlock(lengthTable, lengthTable.length);
    }

    private byte[] compressText() {
        byte[] out = new byte[(int) ((double) alphabetSize / 256 * textSize * 1.1) + 1];  // The max possible result length
        int bits = 0;
        int bitPos = 0;
        int resIndex = 0;
        for (int i = 0; i < textSize; ++i) {
            int value = fullText[textBegin + i];
            int codeLen = lengthTable[value];
            int code = codeTable[value];
            if (codeLen == 0) throw new RuntimeException();
            bits <<= codeLen;
            bits |= code;
            bitPos += codeLen;

            while (bitPos >= 8) {
                bitPos -= 8;
                out[resIndex++] = (byte) (bits >> bitPos);
            }
        }

        int codeLen = lengthTable[endSig];
        int code = codeTable[endSig];
        if (codeLen == 0) throw new RuntimeException();

        bits <<= codeLen;
        bits |= code;
        bitPos += codeLen;

        while (bitPos >= 8) {
            bitPos -= 8;
            out[resIndex++] = (byte) (bits >> bitPos);
        }

        if (bitPos > 0) {
            bits <<= (8 - bitPos);
            out[resIndex++] = (byte) bits;
        }
        byte[] result = new byte[resIndex];
        System.arraycopy(out, 0, result, 0, resIndex);
        return result;
    }

    /**
     * Sets up the {@code maxHeight} value which limits the max depth of the huffman tree.
     *
     * @param height the tree-height limit.
     */
    public void setMaxHeight(int height) {
        maxHeight = height;
    }

    /**
     * Returns the compressed text using the native huffman code of this {@code LongHuffmanCompressorRam}.
     *
     * @return the compressed text.
     */
    public byte[] compress() {
        return compressText();
    }

    static int expectLength(int[] codeLengthMap, int[] freqMap) {
        int aftLen = 0;
        for (int i = 0; i < codeLengthMap.length; i++) {
            aftLen += freqMap[i] * codeLengthMap[i];
        }
        return aftLen / 8 + (aftLen % 8 == 0 ? 0 : 1);
    }

    public int findOptimalLength(int textBegin, int minLength) {
        minLength = textBegin + minLength > fullText.length ?
                fullText.length - textBegin : minLength;  // make sure index not outbound

        int[] freq;
        int[] codeLengths;
        if (lastFreqTable == null || lastLengthTable == null) {
            freq = new int[alphabetSize];
            freq[endSig] = 1;
            LongHuffmanUtil.addFrequencies(fullText, freq, textBegin, minLength);

//            HuffmanNode rootNode = LongHuffmanUtil.generateHuffmanTree(freq);
//            LongHuffmanUtil.generateCodeLengthMap(codeLengths, rootNode, 0);

            codeLengths = LongHuffmanUtil.generateCodeLengthMap(freq);
            LongHuffmanUtil.heightControl(codeLengths, freq, maxHeight);
        } else {
            freq = lastFreqTable;
            codeLengths = lastLengthTable;
        }

        int expectLength = expectLength(codeLengths, freq) + ESTIMATE_CMP_MAP_LENGTH;  // tables, etc
        int curLength = minLength;

        int[] mergedFreq = new int[alphabetSize];
        int[] mergedCodeLengths = new int[alphabetSize];
        int[] newPartFreq = new int[alphabetSize];
        int[] newPartCodeLengths = new int[alphabetSize];

        while (textBegin + curLength < fullText.length) {
//            if (true) break;
            int blockEnds = Math.min(textBegin + curLength + OPTIMAL_BLOCK_SIZE, fullText.length);
            int blockSize = blockEnds - curLength - textBegin;

            // Calculate the expected length of new added part only
            Arrays.fill(newPartFreq, 0);
            Arrays.fill(newPartCodeLengths, 0);
            newPartFreq[endSig] = 1;
            LongHuffmanUtil.addFrequencies(fullText, newPartFreq, textBegin + curLength, blockSize);
            HuffmanNode newPartRootNode = LongHuffmanUtil.generateHuffmanTree(newPartFreq);
            
            LongHuffmanUtil.generateCodeLengthMap(newPartCodeLengths, newPartRootNode, 0);
            LongHuffmanUtil.heightControl(newPartCodeLengths, newPartFreq, maxHeight);

            int newPartExpectLength = expectLength(newPartCodeLengths, newPartFreq) + ESTIMATE_CMP_MAP_LENGTH;

            // Expected length of merged text
            System.arraycopy(freq, 0, mergedFreq, 0, alphabetSize);
            Arrays.fill(mergedCodeLengths, 0);

            LongHuffmanUtil.addFrequencies(fullText, mergedFreq, textBegin + curLength, blockSize);
            HuffmanNode mergedRootNode = LongHuffmanUtil.generateHuffmanTree(mergedFreq);
            LongHuffmanUtil.generateCodeLengthMap(mergedCodeLengths, mergedRootNode, 0);
            LongHuffmanUtil.heightControl(mergedCodeLengths, mergedFreq, maxHeight);

            int mergedExpectLength = expectLength(mergedCodeLengths, mergedFreq) + ESTIMATE_CMP_MAP_LENGTH;
            int twoPartsExpectLength = expectLength + newPartExpectLength;

            if (mergedExpectLength > twoPartsExpectLength) {
//                if (curLength != minLength) System.out.print("gg");
                lastFreqTable = newPartFreq;
                lastLengthTable = newPartCodeLengths;
                break;
            } else {
                expectLength = mergedExpectLength;
                curLength += blockSize;
                System.arraycopy(mergedFreq, 0, freq, 0, alphabetSize);
                System.arraycopy(mergedCodeLengths, 0, codeLengths, 0, alphabetSize);
            }
        }
        lengthTable = codeLengths;
//        System.out.println("got: " + ((double) expectLength(lengthTable, freqTable) / curLength / 8));
        this.textBegin = textBegin;
        this.textSize = curLength;

        return curLength;
    }

    public List<HuffmanBlock> findOptimalSegments(int baseChunkSize) {
        TreeSet<HuffmanBlock> baseChunks = new TreeSet<>();
        for (int index = 0; index < fullText.length; index += baseChunkSize) {
            boolean extendAsFinalChunk = false;
            int chunkEnd;
            if (index + baseChunkSize * 1.5 >= fullText.length) {
                // The next chunk is too small, join to this chunk
                extendAsFinalChunk = true;
                chunkEnd = fullText.length;
            } else {
                chunkEnd = Math.min(index + baseChunkSize, fullText.length);
            }
            int[] freq = new int[alphabetSize];
            LongHuffmanUtil.addFrequencies(fullText, freq, index, chunkEnd - index);
            freq[endSig] = 1;
            int[] lengthCodes = LongHuffmanUtil.generateCodeLengthMap(freq);
            LongHuffmanUtil.heightControl(lengthCodes, freq, maxHeight);
            baseChunks.add(new HuffmanBlock(index, chunkEnd - index, freq, lengthCodes, endSig));
            if (extendAsFinalChunk) break;
        }
//        System.out.println(baseChunks);
        
        while (baseChunks.size() > 1) {
            TreeSet<HuffmanBlock> newChunks = new TreeSet<>();
            boolean improved = false;
            while (baseChunks.size() > 1) {
                HuffmanBlock hb1 = baseChunks.pollFirst();
                HuffmanBlock hb2 = baseChunks.pollFirst();
                if (hb1 == null || hb2 == null) throw new RuntimeException();
                if (hb1.nonMergeAble.contains(hb2) || hb2.nonMergeAble.contains(hb1)) {
                    newChunks.add(hb1);
                    newChunks.add(hb2);
                    continue;
                }
                int sepLen = hb1.estimatedLength() + hb2.estimatedLength();
                HuffmanBlock merged = hb1.merge(hb2);
                int mergedLen = merged.estimatedLength();
                if (mergedLen <= sepLen) {
                    improved = true;
                    newChunks.add(merged);
                    // If we want to reduce memory usage (GC), clear the non-merge-able here
                } else {
                    hb1.nonMergeAble.add(hb2);
                    hb2.nonMergeAble.add(hb1);
                    newChunks.add(hb1);
                    newChunks.add(hb2);
                }
            }
            if (!baseChunks.isEmpty()) {
                // only 1
                newChunks.addAll(baseChunks);
            }
            baseChunks = newChunks;
//            System.out.println(baseChunks);
            if (!improved) break;
        }
//        System.out.println(baseChunks);
        
        return new ArrayList<>(baseChunks);
    }

    public void generateSingleMap() {
        textBegin = 0;
        textSize = fullText.length;
        int[] freq = new int[alphabetSize];
        lengthTable = new int[alphabetSize];
        freq[endSig] = 1;
        LongHuffmanUtil.addFrequencies(fullText, freq, textBegin, textSize);

        HuffmanNode rootNode = LongHuffmanUtil.generateHuffmanTree(freq);
        LongHuffmanUtil.generateCodeLengthMap(lengthTable, rootNode, 0);

        LongHuffmanUtil.heightControl(lengthTable, freq, maxHeight);
    }

    public static class HuffmanBlock implements Comparable<HuffmanBlock> {
        final int eof;
        int textBegin;
        int textLength;
        int[] freq;
        int[] lengthTable;
        int estLength;
        Set<HuffmanBlock> nonMergeAble = new HashSet<>();
        
        HuffmanBlock(int textBegin, int textLength, int[] freq, int[] lengthTable, int eof) {
            this.eof = eof;
            this.textBegin = textBegin;
            this.textLength = textLength;
            this.freq = freq;
            this.lengthTable = lengthTable;
            
            this.estLength = LongHuffmanCompressorRam.expectLength(lengthTable, freq) + LongHuffmanCompressorRam.ESTIMATE_CMP_MAP_LENGTH;
        }

        @Override
        public int compareTo(HuffmanBlock o) {
            return Integer.compare(textBegin, o.textBegin);
        }

        int estimatedLength() {
            return estLength;
        }
        
        HuffmanBlock merge(HuffmanBlock other) {
            if (textBegin > other.textBegin) return other.merge(this);
            
            if (textBegin + textLength != other.textBegin) 
                throw new RuntimeException("Non-continuous blocks cannot be merged: " + 
                        String.format("(%d, %d) - %d", textBegin, textBegin + textLength, other.textBegin));
            int[] mergedFreq = Util.elementWiseAdd(freq, other.freq);
            mergedFreq[eof] = 1;
            int[] mergedMap = LongHuffmanUtil.generateCodeLengthMap(mergedFreq);
            LongHuffmanUtil.heightControl(mergedMap, mergedFreq, maxHeight);
            return new HuffmanBlock(textBegin, textLength + other.textLength, mergedFreq, mergedMap, eof);
        }

        @Override
        public String toString() {
            return String.format("Block(%d, %d; %d)", textBegin, textBegin + textLength, estLength);
        }
    }
}
