package WinLzz.LZZ2.Util;

import WinLzz.Utility.ArrayHashTable;

import java.util.ArrayDeque;
import java.util.HashMap;

//public class SimpleHashSlider extends HashMap<HashNodeBase, ArrayDeque<Long>> {
public class SimpleHashSlider extends ArrayHashTable {

    private ArrayDeque<HashNodeBase> trackQueue = new ArrayDeque<>();

    private HashNodeBase nullRep = new HashNodeBase();

    public SimpleHashSlider() {
        super(16777216);
    }

    @Override
    public ArrayDeque<Long> put(HashNodeBase key, ArrayDeque<Long> value) {
        trackQueue.addLast(key);
        return super.put(key, value);
    }

    public void addIndex(HashNode key, long index) {
        get(key).addLast(index);
        trackQueue.addLast(key);
    }

    public void addVoid() {
        trackQueue.addLast(nullRep);
    }

    public void clearOutRanged(int dictSize) {
        while (trackQueue.size() > dictSize) {
            HashNodeBase first = trackQueue.removeFirst();
            if (first instanceof HashNode) {
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
