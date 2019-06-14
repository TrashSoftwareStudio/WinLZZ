package trashsoftware.win_bwz.Utility;

import trashsoftware.win_bwz.LZZ2.Util.HashNodeBase;

import java.util.ArrayDeque;

public class ArrayHashTable {

    private ArrayDeque<Long>[] table;

    @SuppressWarnings("all")
    public ArrayHashTable(int fixedWidth) {
        table = new ArrayDeque[fixedWidth];
    }

    public ArrayDeque<Long> put(HashNodeBase key, ArrayDeque<Long> value) {
        table[key.hashCode()] = value;
        return value;
    }

    public ArrayDeque<Long> get(HashNodeBase key) {
        return table[key.hashCode()];
    }

    protected void remove(HashNodeBase key) {
        table[key.hashCode()] = null;
    }
}
