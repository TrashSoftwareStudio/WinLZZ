package trashsoftware.win_bwz.lzz2.util;

public class HashNode extends HashNodeBase {

    private byte b1;

    private byte b2;

    private byte b3;

    public HashNode(byte b1, byte b2, byte b3) {
        this.b1 = b1;
        this.b2 = b2;
        this.b3 = b3;
    }

    @Override
    public int hashCode() {
        return ((b1 & 0xff) << 16) | ((b2 & 0xff) << 8) | (b3 & 0xff);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof HashNode && b1 == ((HashNode) obj).b1 && b2 == ((HashNode) obj).b2 &&
                b3 == ((HashNode) obj).b3;
    }

    @Override
    public String toString() {
        return "[" + b1 + ", " + b2 + ", " + b3 + "]";
    }

    public byte getByte(int index) {
        if (index == 0) {
            return b1;
        } else if (index == 1) {
            return b2;
        } else {
            return b3;
        }
    }
}
