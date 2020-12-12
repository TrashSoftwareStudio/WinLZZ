package trashsoftware.winBwz.core.fastLzz;

import java.util.Arrays;

public abstract class FixedSlider {
    abstract void clear();

    abstract void addIndex(int hashCode, int position);
}

class FixedArraySlider extends FixedSlider {
    private FixedArrayDeque[] array = new FixedArrayDeque[65536];

    private int arraySize;
    private int andEr;

    public FixedArraySlider(int arraySize) {
        this.arraySize = arraySize;
        this.andEr = arraySize - 1;
    }

    void addIndex(int hashCode, int position) {
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

    void clear() {
        for (FixedArrayDeque fad : array) {
            if (fad != null) fad.tail = 0;
        }
    }

    public int getAndEr() {
        return andEr;
    }

    public static class FixedArrayDeque {
        private int arraySize;
        int[] array;
        int tail = 0;

        FixedArrayDeque(int arraySize) {
            this.arraySize = arraySize;
            array = new int[arraySize];
        }

        int beginPos() {
            return tail >= arraySize ? tail - arraySize : 0;
        }
    }
}

class FixedIntSlider extends FixedSlider {
    private int[] array = new int[65536];

    FixedIntSlider() {
        resetArray();
    }

    void addIndex(int hashCode, int position) {
        array[hashCode] = position;
    }

    void clear() {
        resetArray();
    }

    int get(int hashCode) {
        return array[hashCode];
    }

    private void resetArray() {
        Arrays.fill(array, -1);
    }
}
