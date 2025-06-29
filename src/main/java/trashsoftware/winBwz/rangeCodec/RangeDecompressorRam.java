package trashsoftware.winBwz.rangeCodec;

public class RangeDecompressorRam extends AbstractRangeDecompressor {

    private final byte[] comText;

    private final int limit;
    private int inputIndex;

    public RangeDecompressorRam(byte[] text, int limit) {
        this.comText = text;
        this.limit = limit;
    }

    @Override
    protected int loadNextByte() {
        if (inputIndex >= limit) {
            return -1;
        }
        blockBytesRead++;
        return comText[inputIndex++] & 0xff;
    }
}
