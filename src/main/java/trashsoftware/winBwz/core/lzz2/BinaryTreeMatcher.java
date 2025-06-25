package trashsoftware.winBwz.core.lzz2;

import trashsoftware.winBwz.utility.FileInputBufferArray;

import java.io.IOException;
import java.util.Arrays;

public abstract class BinaryTreeMatcher implements Lzz2Matcher {

    protected int distance;
    protected int length;
    
    protected int windowSize;
    protected int maxMatch;
    protected int maxDepth;

    protected final int[] leftChild;
    protected final int[] rightChild;
    protected final long[] positions;

    protected int root = -1;
    
    BinaryTreeMatcher(int windowSize, int labSize, int maxDepth) {
        this.windowSize = windowSize;
        this.maxMatch = labSize;
        this.maxDepth = maxDepth;
        
        this.leftChild = new int[windowSize];
        this.rightChild = new int[windowSize];
        this.positions = new long[windowSize];

        Arrays.fill(leftChild, -1);
        Arrays.fill(rightChild, -1);
        Arrays.fill(positions, -1);
    }

    @Override
    public void fillSlider(long prevPos, long currentPos, FileInputBufferArray inputBufferArray) throws IOException {
        for (long p = prevPos; p < currentPos; p++) {
            int index = (int) (p % windowSize);
            positions[index] = p;
            insertNode(index, inputBufferArray);
        }
    }

    private void insertNode(int index, FileInputBufferArray inputBufferArray) throws IOException {
        long pos = positions[index];
        if (root == -1) {
            root = index;
            return;
        }

        int cur = root;
        while (true) {
            long curPos = positions[cur];
            int cmpLen = compare(pos, curPos, inputBufferArray);

            byte nextByteA = inputBufferArray.getByte(pos + cmpLen);
            byte nextByteB = inputBufferArray.getByte(curPos + cmpLen);

            if ((nextByteA & 0xFF) < (nextByteB & 0xFF)) {
                if (leftChild[cur] == -1) {
                    leftChild[cur] = index;
                    break;
                }
                cur = leftChild[cur];
            } else {
                if (rightChild[cur] == -1) {
                    rightChild[cur] = index;
                    break;
                }
                cur = rightChild[cur];
            }
        }
    }

    protected int compare(long a, long b, FileInputBufferArray inputBufferArray) throws IOException {
        int i = 0;
        while (i < maxMatch) {
            byte ba = inputBufferArray.getByte(a + i);
            byte bb = inputBufferArray.getByte(b + i);
            if (ba != bb) break;
            i++;
        }
        return i;
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public int getDistance() {
        return distance;
    }

    public static class Greedy extends BinaryTreeMatcher {
        
        public Greedy(int windowSize, int labSize, int maxDepth) {
            super(windowSize, labSize, maxDepth);
        }
        
        @Override
        public int search(FileInputBufferArray inputBufferArray, long position) throws IOException {
            int curIndex = (int) (position % windowSize);

            int bestLen = 0;
            long bestDist = 0;
            int current = root;
            int depth = 0;

            while (current != -1 && depth++ < maxDepth) {
                long matchPos = positions[current];
                if (matchPos < 0 || matchPos >= position) {
                    break;
                }

                int matchLen = compare(position, matchPos, inputBufferArray);
                if (matchLen > bestLen) {
                    bestLen = matchLen;
                    bestDist = position - matchPos;
                }

                byte nextByteA = inputBufferArray.getByte(position + matchLen);
                byte nextByteB = inputBufferArray.getByte(matchPos + matchLen);

                if ((nextByteA & 0xFF) < (nextByteB & 0xFF)) {
                    current = leftChild[current];
                } else {
                    current = rightChild[current];
                }
            }
            
            length = bestLen;
            distance = (int) bestDist;
            return 0;

//            return bestLen >= MIN_MATCH ? new Match(bestLen, (int) bestDist) : null;
        }
    }
}
