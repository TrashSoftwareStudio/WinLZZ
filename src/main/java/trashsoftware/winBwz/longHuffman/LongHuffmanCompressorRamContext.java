package trashsoftware.winBwz.longHuffman;

import java.util.ArrayList;
import java.util.List;

public class LongHuffmanCompressorRamContext extends LongHuffmanCompressorRamBase {
    
    private int[][] codeTables;
    private int[][] lengthTables;
    protected final int[] ranges;

    /**
     * Creates a new {@code LongHuffmanCompressorRam} instance.
     * <p>
     * LongHuffmanCompressor deals text from range 0 to 32767.
     * This compressor works completely in random access memory.
     *
     * @param fullText     the total text
     * @param alphabetSize the alphabet size, with endSig and other included
     * @param endSig       the mark of the end of stream.
     * @param ranges       the context division
     */
    public LongHuffmanCompressorRamContext(int[] fullText, int alphabetSize, int endSig, int[] ranges) {
        super(fullText, alphabetSize, endSig);
        
        this.ranges = ranges;
    }

    public LongHuffmanCompressorRamContext(int[] fullText, int alphabetSize, int endSig) {
        this(fullText, alphabetSize, endSig, new int[]{1, 2, 4, 8, 16, 32, 64, 128, alphabetSize});
    }
    
    public int nRanges() {
        return ranges.length;
    }
    
    public byte[][] setBlock(Block block) {
        lengthTables = block.lengthTables;
        textBegin = block.textBegin;
        textSize = block.textLength;
        codeTables = new int[ranges.length][];
        byte[][] res = new byte[ranges.length][];
        for (int i = 0; i < ranges.length; i++) {
            codeTables[i] = LongHuffmanUtil.generateCanonicalCode(lengthTables[i]);
            res[i] = LongHuffmanUtil.generateCanonicalCodeBlock(lengthTables[i], lengthTables[i].length);
        }
//        System.out.printf("Set block: %d, %d\n", textBegin, textBegin + textSize);
        return res;
    }
    
    public List<Block> generateBlocks(int baseChunkSize) {
        List<Block> blocks = new ArrayList<>();
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
            int[][] freqs = new int[ranges.length][alphabetSize];
            int lastSymbol = endSig;
            for (int i = index; i < chunkEnd; i++) {
                int symbol = fullText[i];
                int tableI = findTableIndex(lastSymbol);
                freqs[tableI][symbol]++;
                lastSymbol = symbol;
            }
            int tableI = findTableIndex(lastSymbol);
            freqs[tableI][endSig] = 1;
            
            int[][] lengthMaps = new int[ranges.length][];
            for (int j = 0; j < lengthMaps.length; j++) {
                lengthMaps[j] = LongHuffmanUtil.generateCodeLengthMap(freqs[j]);
                LongHuffmanUtil.heightControl(lengthMaps[j], freqs[j], maxHeight);
            }
            
            blocks.add(new Block(lengthMaps, index, chunkEnd - index));
            if (extendAsFinalChunk) break;
        }
        return blocks;
    }

    protected int findTableIndex(int symbol) {
        if (symbol < ranges[0]) return 0;
        if (symbol >= ranges[ranges.length - 2]) return ranges.length - 1;
        int low = 0;
        int high = ranges.length - 1;

        while (low < high) {
            int mid = (low + high) >>> 1;
            if (mid == 0) return 0;
            int rangeLow = ranges[mid - 1];
            int rangeHigh = ranges[mid];
            if (symbol < rangeLow) {
                high = mid;
            } else if (symbol >= rangeHigh) {
                low = mid;
            } else {
                return mid;
            }
        }
        throw new IllegalArgumentException("Value out of range: " + symbol);
    }

    @Override
    public byte[] compress() {
        byte[] out = new byte[(int) ((double) alphabetSize / 256 * textSize * 1.1) + 1];  // The max possible result length
        int bits = 0;
        int bitPos = 0;
        int resIndex = 0;
        int lastSymbol = endSig;
        for (int i = 0; i < textSize; ++i) {
            int tableI = findTableIndex(lastSymbol);
            int value = fullText[textBegin + i];
            int codeLen = lengthTables[tableI][value];
            int code = codeTables[tableI][value];
            if (codeLen == 0) throw new RuntimeException();
            bits <<= codeLen;
            bits |= code;
            bitPos += codeLen;

            while (bitPos >= 8) {
                bitPos -= 8;
                out[resIndex++] = (byte) (bits >> bitPos);
            }
            lastSymbol = value;
        }

        int tableI = findTableIndex(lastSymbol);
        int codeLen = lengthTables[tableI][endSig];
        int code = codeTables[tableI][endSig];
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
    
    public static class Block {
        public final int[][] lengthTables;
        public final int textBegin;
        public final int textLength;
        
        Block(int[][] lengthTables, int textBegin, int textLength) {
            this.lengthTables = lengthTables;
            this.textBegin = textBegin;
            this.textLength = textLength;
        }
    }
}
