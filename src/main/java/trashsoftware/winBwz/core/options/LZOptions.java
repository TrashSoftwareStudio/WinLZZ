package trashsoftware.winBwz.core.options;

public class LZOptions extends AlgOptions {
    
    protected int labSize;  // Size of look ahead buffer. This does not matter when decompression
    
    public LZOptions(int windowSize, int labSize) {
        super(windowSize);
        
        this.labSize = labSize;
    }

    public int getLabSize() {
        return labSize;
    }
}
