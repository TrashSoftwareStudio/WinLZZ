package trashsoftware.winBwz.core.options;

public abstract class AlgOptions {
    
    protected int windowSize;
    
    public AlgOptions(int windowSize) {
        this.windowSize = windowSize;
    }

    public int getWindowSize() {
        return windowSize;
    }
}
