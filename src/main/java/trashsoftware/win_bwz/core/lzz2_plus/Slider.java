package trashsoftware.win_bwz.core.lzz2_plus;

public class Slider {
    private FixedArrayDeque[] array = new FixedArrayDeque[65536];

    void addIndex(int hashCode, int position) {
        FixedArrayDeque fad = array[hashCode];
        if (fad == null) {
            fad = new FixedArrayDeque();
            array[hashCode] = fad;
        }
        fad.array[(fad.tail++) & FixedArrayDeque.RANGE] = position;
    }

    FixedArrayDeque get(int hashCode) {
        return array[hashCode];
    }

    void clear() {
        for (FixedArrayDeque fad : array) {
            if (fad != null) fad.tail = 0;
        }
    }
}

class FixedArrayDeque {
    private static final int ARRAY_SIZE = 64;
    static final int RANGE = 63;
    int[] array = new int[ARRAY_SIZE];
    int tail;

    public int beginPos() {
        return tail >= ARRAY_SIZE ? tail - ARRAY_SIZE : 0;
    }
}
