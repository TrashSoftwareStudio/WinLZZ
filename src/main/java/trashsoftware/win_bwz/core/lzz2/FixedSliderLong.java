package trashsoftware.win_bwz.core.lzz2;

public class FixedSliderLong {
    private FixedArrayDeque[] array = new FixedArrayDeque[65536];

    private int arraySize;
    private int andEr;

    public FixedSliderLong(int arraySize) {
        this.arraySize = arraySize;
        this.andEr = arraySize - 1;
    }

    public void addIndex(int hashCode, long position) {
        FixedArrayDeque fad = array[hashCode];
        if (fad == null) {
            fad = new FixedArrayDeque(arraySize);
            array[hashCode] = fad;
        }
        fad.array[(fad.tail++) & andEr] = position;
    }

    public FixedArrayDeque get(int hashCode) {
        return array[hashCode];
    }
}

class FixedArrayDeque {
    private int arraySize;
    long[] array;
    int tail = 0;

    FixedArrayDeque(int arraySize) {
        this.arraySize = arraySize;
        array = new long[arraySize];
    }

    int beginPos() {
        return tail >= arraySize ? tail - arraySize : 0;
    }

    int getTail() {
        return tail;
    }

    long get(int index) {
        return array[index & (arraySize - 1)];
    }

    int size() {
        return tail - beginPos();
    }
}

