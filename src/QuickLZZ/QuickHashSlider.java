package QuickLZZ;

import java.util.ArrayDeque;
import java.util.HashMap;

public class QuickHashSlider extends HashMap<QuickHashNode, Integer> {

    private ArrayDeque<QuickHashNode> trackQueue = new ArrayDeque<>();

    QuickHashSlider() {
        super();
    }

    @Override
    public Integer put(QuickHashNode key, Integer value) {
        trackQueue.addLast(key);
        return super.put(key, value);
    }

    void clearOutRanged(int dictSize) {
        while (trackQueue.size() > dictSize) {
            QuickHashNode first = trackQueue.removeFirst();

            remove(first);


        }
    }
}
