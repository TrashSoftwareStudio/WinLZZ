package trashsoftware.winBwz.core.fasterLzz;

import java.util.Arrays;

class FixedIntSlider {
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
