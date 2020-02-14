package trashsoftware.win_bwz.core.lzz2;

public class FixedSliderLong {
    private FixedArrayDeque[] array = new FixedArrayDeque[65536];

    private int arraySize;
    int andEr;

    public FixedSliderLong(int arraySize) {
        this.arraySize = arraySize;
        this.andEr = arraySize - 1;
    }

    public void addIndex(int hashCode, long position, int nextHash) {
        FixedArrayDeque fad = array[hashCode];
        if (fad == null) {
            fad = new FixedArrayDeque(arraySize);
            array[hashCode] = fad;
        }
        int index = (fad.tail++) & andEr;
        fad.array[index] = position;
        fad.nextHashArray[index] = nextHash;
    }

    public FixedArrayDeque get(int hashCode) {
        return array[hashCode];
    }
}

class FixedArrayDeque {
    private int arraySize;
    long[] array;
    int[] nextHashArray;
    int tail = 0;

    FixedArrayDeque(int arraySize) {
        this.arraySize = arraySize;
        array = new long[arraySize];
        nextHashArray = new int[arraySize];
    }

    int beginPos() {
        return tail >= arraySize ? tail - arraySize : 0;
    }

    int tailPos() {
        return tail;
    }

    long get(int index) {
        return array[index & (arraySize - 1)];
    }
}

