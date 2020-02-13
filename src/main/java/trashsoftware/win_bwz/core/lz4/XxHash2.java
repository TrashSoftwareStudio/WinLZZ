package trashsoftware.win_bwz.core.lz4;

import trashsoftware.win_bwz.utility.Bytes;

public class XxHash2 {
    static final long Prime1 = 2654435761L;
    static final long Prime2 = 2246822519L;
    static final long Prime3 = 3266489917L;
    static final long Prime4 = 668265263L;
    static final long Prime5 = 374761393L;

    private long state0;
    private long state1;
    private long state2;
    private long state3;

    static final int maxBufferSize = 16;
    private byte[] buffer = new byte[maxBufferSize];
    private int totalLength = 0;
    private int bufferSize = 0;

    public XxHash2() {
        state0 = Prime1 + Prime2;
        state1 = Prime2;
        state2 = 0;
        state3 = (-Prime1) & 0xffffffffL;
    }

    void add(byte[] array, int index, int length) {
        totalLength += length;
        if (bufferSize + length < maxBufferSize) {
            // just add new data
            while (length-- > 0)
                buffer[bufferSize++] = array[index++];
            return;
        }
        int stopIndex = index + length;
        int blockBeforeStop = stopIndex - maxBufferSize;

        // some data left from previous update ?
        if (bufferSize > 0) {
            // make sure temporary buffer is full (16 bytes)
            while (bufferSize < maxBufferSize)
                buffer[bufferSize++] = array[index++];

            // process these 16 bytes (4x4)
            process(buffer, 0);
        }

        // copying state to local variables helps optimizer A LOT
        // 16 bytes at once
        while (index <= blockBeforeStop) {
            // local variables s0..s3 instead of state[0]..state[3] are much faster
            process(array, index);
            index += 16;
        }

        // copy remainder to temporary buffer
        bufferSize = stopIndex - index;
        for (int i = 0; i < bufferSize; i++)
            buffer[i] = array[index + i];
    }

    void process(byte[] buffer, int index) {
        state0 = rotateLeft(state0 + Bytes.bytesToInt32(buffer, index) * Prime2, 13) * Prime1;
    }

//    long hash() {
//        long result = totalLength;
//    }

    private static long rotateLeft(long x, long bits) {
        return (x << bits) | (x >> (32 - bits));
    }
}
