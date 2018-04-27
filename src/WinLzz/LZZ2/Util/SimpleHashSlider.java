package WinLzz.LZZ2.Util;

import java.util.ArrayDeque;
import java.util.HashMap;

public class SimpleHashSlider extends HashMap<HashNode, ArrayDeque<Integer>> {

    private ArrayDeque<HashNodeBase> trackQueue = new ArrayDeque<>();

    private HashNodeBase nullRep = new HashNodeBase();

    public SimpleHashSlider() {
        super();
    }

    @Override
    public ArrayDeque<Integer> put(HashNode key, ArrayDeque<Integer> value) {
        trackQueue.addLast(key);
        return super.put(key, value);
    }

    public void addIndex(HashNode key, int index) {
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
                ArrayDeque<Integer> ll = get(first);
                if (ll.size() > 1) {
                    ll.removeFirst();
                } else {
                    remove(first);
                }
            }
        }
    }
}
