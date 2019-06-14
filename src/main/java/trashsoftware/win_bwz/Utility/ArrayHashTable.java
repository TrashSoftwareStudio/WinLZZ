package trashsoftware.win_bwz.Utility;

import java.util.ArrayDeque;

public class ArrayHashTable {

    private ArrayDeque<Long>[] table;

    @SuppressWarnings("all")
    public ArrayHashTable(int fixedWidth) {
        table = new ArrayDeque[fixedWidth];
    }

    public ArrayDeque<Long> put(int key, ArrayDeque<Long> value) {
        table[key] = value;
        return value;
    }

    public ArrayDeque<Long> get(int key) {
        return table[key];
    }

    protected void remove(int key) {
        table[key] = null;
    }
}
