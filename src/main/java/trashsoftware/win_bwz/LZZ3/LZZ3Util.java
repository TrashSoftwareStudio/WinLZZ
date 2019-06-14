package trashsoftware.win_bwz.LZZ3;

public class LZZ3Util {
}

class SliderNode {

    private long position;

    private byte hash0, hash1, hash2, hash3;

    SliderNode(long position, byte hash0, byte hash1, byte hash2, byte hash3) {
        this.position = position;
        this.hash0 = hash0;
        this.hash1 = hash1;
        this.hash2 = hash2;
        this.hash3 = hash3;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
