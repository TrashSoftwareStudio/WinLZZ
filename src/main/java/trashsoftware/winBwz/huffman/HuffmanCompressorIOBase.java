package trashsoftware.winBwz.huffman;

public abstract class HuffmanCompressorIOBase extends HuffmanCompressorBase {
    protected String inFile;
    
    public HuffmanCompressorIOBase(String inFile) {
        this.inFile = inFile;
    }
}
