package trashsoftware.winBwz.longHuffman;

import trashsoftware.winBwz.utility.IntArrayOutputStream;

import java.io.IOException;

public class LongHuffmanDecompressorRam extends LongHuffmanDecoder {

    private final byte[] comText;

    private final int limit;
    private int inputIndex;

    private final IntArrayOutputStream result = new IntArrayOutputStream();

    public LongHuffmanDecompressorRam(byte[] text, int limit, int alphabetSize) {
        super(alphabetSize);
        this.limit = limit;
        this.comText = text;
    }

    @Override
    protected void loadBits(int leastPos) {
        int drainCount = 0;
        while (bitPos < leastPos) {
            bitPos += 8;
            bits <<= 8;
            if (inputIndex < limit) {
                bits |= (comText[inputIndex++] & 0xff);
            } else {
                if (drainCount > 0) {
                    throw new ArrayIndexOutOfBoundsException("Reaches the stream end before encountering EOF.");
                }
                drainCount++;
            }
        }
    }

    @Override
    protected void setInputPosition(long pos) throws IOException {
        inputIndex = Math.toIntExact(pos);
    }

    @Override
    protected void decodeToEof() throws IOException {
        result.reset();
        
        decodeTillEofCore();
    }

    @Override
    protected void appendResult(int code) {
        result.write(code);
        currentIndex++;
    }

    @Override
    protected int[] packResult() {
        return result.toIntArray();
    }
}
