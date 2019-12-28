package trashsoftware.win_bwz.lzz2_plus;

import trashsoftware.win_bwz.bwz.MTFInverseByte;
import trashsoftware.win_bwz.bwz.ZeroRLCDecoderByte;
import trashsoftware.win_bwz.huffman.HuffmanDeCompressor;
import trashsoftware.win_bwz.huffman.MapCompressor.MapDeCompressor;
import trashsoftware.win_bwz.interfaces.DeCompressor;
import trashsoftware.win_bwz.lzz2.util.LZZ2Util;
import trashsoftware.win_bwz.packer.UnPacker;
import trashsoftware.win_bwz.utility.Bytes;
import trashsoftware.win_bwz.utility.FileBitInputStream;
import trashsoftware.win_bwz.utility.FileOutputBufferArray;
import trashsoftware.win_bwz.utility.Util;

import java.io.*;
import java.util.LinkedList;

/**
 * LZZ2-algorithm decompressor, implements {@code DeCompressor} interface.
 *
 * @author zbh
 * @see DeCompressor
 * @since 0.4
 */
public class Lzz2PlusDecompressor implements DeCompressor {

    private String inFile;

    private final static int readBufferSize = 8192;

    private int windowSize;

    private FileBitInputStream fis;

    private UnPacker parent;

    private int timeAccumulator;

    private long lastUpdateProgress;

    private long startTime;

    private long timeOffset;

    private long umpLength;

    private byte[] outBuffer = new byte[Lzz2PlusCompressor.MEMORY_BUFFER_SIZE];

    public Lzz2PlusDecompressor(String inFile, int windowSize) throws IOException {
        this.inFile = inFile;
//        File f = new File(inFile);
//        int length = (int) f.length();

        this.windowSize = windowSize;

        fis = new FileBitInputStream(new FileInputStream(inFile));
        byte[] lengthBytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            lengthBytes[i] = fis.readByte();
        }
        umpLength = Bytes.bytesToInt32(lengthBytes);
    }

    private void uncompressMain(OutputStream fos) throws IOException {


        long lastCheckTime = System.currentTimeMillis();
        startTime = lastCheckTime;
        if (parent != null) timeOffset = lastCheckTime - parent.startTime;
        long currentTime;

        int indexInBuffer = 0;
        long totalUmpIndex = 0;

        while (totalUmpIndex + indexInBuffer < umpLength) {
            int s = fis.read();
            if (s == 0) {
                byte lit = fis.readByte();
                outBuffer[indexInBuffer++] = lit;
            } else if (s == 1) {
                int distance;
                int length = fis.readByte() & 0xff;
                int dis0 = fis.readByte() & 0xff;
                if (dis0 < 128) {
                    distance = dis0;
                } else {
                    dis0 &= 0x7f;
                    int dis1 = fis.readByte() & 0xff;
                    distance = (dis0 << 8) | dis1;
                }
//                System.out.print(distance + " " + length + ", ");

                int from = indexInBuffer - distance;
                int to = from + length;
//                System.out.println(from + " " + distance + " " + indexInBuffer);
                if (to <= indexInBuffer) {
//                            byte[] repeat = tempResult.subSequence(from, to);
//                            for (byte b : repeat) tempResult.write(b);
                    System.arraycopy(outBuffer, from, outBuffer, indexInBuffer, length);
                } else {
                    int p = 0;
                    int overlap = indexInBuffer - from;
                    while (p < length) {
                        System.arraycopy(outBuffer,
                                from,
                                outBuffer,
                                indexInBuffer + p,
                                Math.min(overlap, length - p));
                        p += overlap;
                    }
//                            byte[] overlapRepeat = new byte[length];
//                            int overlap = (int) (index - from);
//                            byte[] repeat = tempResult.subSequence(from, index);
//                            int p = 0;
//                            while (p < length) {
//                                System.arraycopy(repeat, 0, overlapRepeat, p, Math.min(overlap, length - p));
//                                p += overlap;
//                            }
//                            for (byte b : overlapRepeat) tempResult.write(b);
                }
                indexInBuffer += length;
            } else {
                break;
            }
            if (indexInBuffer >= Lzz2PlusCompressor.MEMORY_BUFFER_SIZE) {
                if (indexInBuffer != Lzz2PlusCompressor.MEMORY_BUFFER_SIZE) throw new RuntimeException();
                totalUmpIndex += indexInBuffer;
                indexInBuffer = 0;
                fos.write(outBuffer);
            }
            if (parent != null && parent.isInterrupted) break;
            if (parent != null && (currentTime = System.currentTimeMillis()) - lastCheckTime >= 50) {
                updateInfo(totalUmpIndex + indexInBuffer, currentTime);
                lastCheckTime = currentTime;
            }
        }
        if (indexInBuffer > 0) {
            fos.write(outBuffer, 0, indexInBuffer);
        }
//        tempResult.flush();
//        tempResult.close();
    }

    @Override
    public void deleteCache() {
    }

    @Override
    public void setParent(UnPacker parent) {
        this.parent = parent;
    }

    @Override
    public void setThreads(int threads) {
    }

    @Override
    public void uncompress(OutputStream outFile) throws IOException {
        uncompressMain(outFile);
        fis.close();
    }

    private void updateInfo(long current, long updateTime) {
        parent.progress.set(current);
        if (timeAccumulator == 19) {
            timeAccumulator = 0;
            double finished = ((double) current) / parent.getTotalOrigSize();
            double rounded = (double) Math.round(finished * 1000) / 10;
            parent.percentage.set(String.valueOf(rounded));
            int newUpdated = (int) (current - lastUpdateProgress);
            lastUpdateProgress = parent.progress.get();
            int ratio = newUpdated / 1024;
            parent.ratio.set(String.valueOf(ratio));

            long timeUsed = updateTime - startTime;
            parent.timeUsed.set(Util.secondToString((timeUsed + timeOffset) / 1000));
            long expectTime = (parent.getTotalOrigSize() - current) / ratio / 1024;
            parent.timeExpected.set(Util.secondToString(expectTime));

            parent.passedLength.set(Util.sizeToReadable(current));
        } else {
            timeAccumulator += 1;
        }
    }
}
