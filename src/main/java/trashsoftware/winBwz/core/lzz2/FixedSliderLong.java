package trashsoftware.winBwz.core.lzz2;

public class FixedSliderLong {
    private final FixedArrayDeque[] array = new FixedArrayDeque[65536];

    private final int arraySize;
    final int andEr;

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
    long[] array;
    int[] nextHashArray;
    int tail = 0;

    FixedArrayDeque(int arraySize) {
        array = new long[arraySize];
        nextHashArray = new int[arraySize];
    }

    int beginPos() {
        return tail >= array.length ? tail - array.length : 0;
    }

    int tailPos() {
        return tail;
    }

    long get(int index) {
        return array[index & (array.length - 1)];
    }
}

