package trashsoftware.winBwz.huffman;

public abstract class HuffmanCompressorRamBase extends HuffmanCompressorBase {
    protected byte[] content;

    public HuffmanCompressorRamBase(byte[] content) {
        this.content = content;
    }
}
