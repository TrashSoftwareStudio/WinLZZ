package trashsoftware.win_bwz.lzz2.util;

import trashsoftware.win_bwz.utility.ArrayHashTable;

import java.util.ArrayDeque;

//public class SimpleHashSlider extends HashMap<HashNodeBase, ArrayDeque<Long>> {
public class SimpleHashSlider extends ArrayHashTable {

    private ArrayDeque<Integer> trackQueue = new ArrayDeque<>();

    private final static int nullRep = -1;

    public SimpleHashSlider() {
        super(16777216);
    }

    @Override
    public ArrayDeque<Long> put(int key, ArrayDeque<Long> value) {
        trackQueue.addLast(key);
        return super.put(key, value);
    }

    public void addIndex(int key, long index) {
        get(key).addLast(index);
        trackQueue.addLast(key);
    }

    public void addVoid() {
        trackQueue.addLast(nullRep);
    }

    public void clearOutRanged(int dictSize) {
        while (trackQueue.size() > dictSize) {
            int first = trackQueue.removeFirst();
            if (first >= 0) {
                ArrayDeque<Long> ll = get(first);
                if (ll.size() > 1) {
                    ll.removeFirst();
                } else {
                    remove(first);
                }
            }
        }
    }
}
