package trashsoftware.winBwz.core.lzz2;

import trashsoftware.winBwz.utility.FileInputBufferArray;

import java.io.IOException;

public abstract class HashedLzz2Matcher implements Lzz2Matcher {

    protected int distance;
    protected int length;
    protected int actualDictSize;
    protected int bufferMaxSize;
    protected long totalLength;
    protected FixedSliderLong slider;
    protected long[] tempArray;

    public HashedLzz2Matcher(int sliderArraySize,
                             int actualDictSize,
                             int bufferMaxSize,
                             long totalFileLength) {
        slider = new FixedSliderLong(sliderArraySize);
        tempArray = new long[sliderArraySize];
        this.actualDictSize = actualDictSize;
        this.bufferMaxSize = bufferMaxSize;
        this.totalLength = totalFileLength;
    }

    @Override
    public void fillSlider(long prevPos, long currentPos, FileInputBufferArray inputBufferArray) throws IOException {
        int lastHash = -1;
        int repeatCount = 0;
        for (long j = prevPos; j < currentPos; j++) {
            byte b0 = inputBufferArray.getByte(j);
            byte b1 = inputBufferArray.getByte(j + 1);
            byte b2 = inputBufferArray.getByte(j + 2);
            byte b3 = inputBufferArray.getByte(j + 3);
            int hash = hash(b0, b1);
            int nextHash = hash(b2, b3);
            if (hash == lastHash) {
                repeatCount++;
            } else {
                if (repeatCount > 0) {
                    repeatCount = 0;
                    slider.addIndex(lastHash, j - 1, nextHash);
                }
                lastHash = hash;
                slider.addIndex(hash, j, nextHash);
            }
        }
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public int getDistance() {
        return distance;
    }

    protected void calculateLongestMatch(FileInputBufferArray inputBufferArray, long index)
            throws IOException {
        byte b0 = inputBufferArray.getByte(index);
        byte b1 = inputBufferArray.getByte(index + 1);
        int hash = hash(b0, b1);
        FixedArrayDeque positions = slider.get(hash);
        if (positions == null) {  // not a match
            length = 0;
            return;
        }
        byte b2 = inputBufferArray.getByte(index + 2);
        byte b3 = inputBufferArray.getByte(index + 3);
        int nextHash = hash(b2, b3);
        int indexInTemp = 0;
        final int beginPos = positions.beginPos();
        int tail = positions.tailPos();
        long windowBegin = Math.max(index - actualDictSize, 0);

        for (int i = tail - 1; i >= beginPos; --i) {
            int realIndex = i & slider.andEr;
            long pos = positions.array[realIndex];
            if (pos <= windowBegin) break;
            int posNextHash = positions.nextHashArray[realIndex];
            if (nextHash == posNextHash) {
                tempArray[indexInTemp++] = pos;
            }
        }
        if (indexInTemp == 0) {
            length = 0;
            return;
        }

        int maxLen = Math.min(bufferMaxSize, (int) (totalLength - index));
        int longest = 0;

        long indexOfLongest = tempArray[0];
        for (int i = 0; i < indexInTemp; ++i) {
            long pos = tempArray[i];
            int len = 2;  // reason of using 2 instead of 4: see {@code fillSlider}, problem in skipping bytes
            while (len < maxLen &&
                    inputBufferArray.getByte(pos + len) == inputBufferArray.getByte(index + len)) {
                len++;
            }
            if (len > longest) {  // Later match is preferred
                longest = len;
                indexOfLongest = pos;
            }
        }

        distance = (int) (index - indexOfLongest);
        length = longest;
    }

    protected static int hash(byte b0, byte b1) {
        return (b0 & 0xff) << 8 | (b1 & 0xff);
    }
}

class GreedyMatcher extends HashedLzz2Matcher {

    public GreedyMatcher(int sliderArraySize, int actualDictSize, int bufferMaxSize, long totalFileLength) {
        super(sliderArraySize, actualDictSize, bufferMaxSize, totalFileLength);
    }

    @Override
    public int search(FileInputBufferArray inputBufferArray, long position) throws IOException {
        calculateLongestMatch(inputBufferArray, position);
        return 0;
    }
}

class NonGreedyMatcherOneStep extends HashedLzz2Matcher {

    public NonGreedyMatcherOneStep(int sliderArraySize, int actualDictSize, int bufferMaxSize, long totalFileLength) {
        super(sliderArraySize, actualDictSize, bufferMaxSize, totalFileLength);
    }

    @Override
    public int search(FileInputBufferArray inputBufferArray, long position) throws IOException {
        calculateLongestMatch(inputBufferArray, position);
        int dis1 = distance;
        int len1 = length;
        calculateLongestMatch(inputBufferArray, position + 1);
        if (length > len1 + 1) {
            return 1;
        } else {
            distance = dis1;
            length = len1;
            return 0;
        }
    }
}

class NonGreedyMatcherMultiStep extends HashedLzz2Matcher {

    public NonGreedyMatcherMultiStep(int sliderArraySize, int actualDictSize, int bufferMaxSize, long totalFileLength) {
        super(sliderArraySize, actualDictSize, bufferMaxSize, totalFileLength);
    }

    @Override
    public int search(FileInputBufferArray inputBufferArray, long position) throws IOException {
        int skip = 1;
        calculateLongestMatch(inputBufferArray, position);
        int lastDis = distance;
        int lastLen = length;
        while (true) {
            calculateLongestMatch(inputBufferArray, position + skip);
            if (length > lastLen + skip) {
                lastDis = distance;
                lastLen = length;
                skip++;
            } else {
                distance = lastDis;
                length = lastLen;
                return skip - 1;
            }
        }
    }
}
