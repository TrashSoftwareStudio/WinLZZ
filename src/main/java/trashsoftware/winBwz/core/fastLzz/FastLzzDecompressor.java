package trashsoftware.winBwz.core.fastLzz;

import trashsoftware.winBwz.core.DeCompressor;
import trashsoftware.winBwz.packer.pz.PzUnPacker;
import trashsoftware.winBwz.utility.Bytes;
import trashsoftware.winBwz.utility.FileBitInputStream;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * LZZ2-algorithm decompressor, implements {@code DeCompressor} interface.
 *
 * @author zbh
 * @see DeCompressor
 * @since 0.4
 */
public class FastLzzDecompressor implements DeCompressor {

    private final FileBitInputStream fis;
    private final long umpLength;
    private final byte[] outBuffer = new byte[FastLzzCompressor.MEMORY_BUFFER_SIZE];
    private PzUnPacker unPacker;
    private long totalOutLength;

    public FastLzzDecompressor(String inFile, int windowSize) throws IOException {

        fis = new FileBitInputStream(new FileInputStream(inFile));
        byte[] lengthBytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            lengthBytes[i] = fis.readByte();
        }
        umpLength = Bytes.bytesToInt32(lengthBytes);
    }

    private void uncompressMain(OutputStream fos) throws IOException {
        int indexInBuffer = 0;
        long totalUmpIndex = 0;

        while (totalUmpIndex + indexInBuffer < umpLength) {
            int s = fis.read();
            if (s == 0) {
                byte lit = fis.readByte();
                outBuffer[indexInBuffer++] = lit;
            } else if (s == 1) {
                int length = FastLzzUtil.readLengthFromStream(fis);
                int distance = FastLzzUtil.readDistanceFromStream(fis);

                int from = indexInBuffer - distance;
                int to = from + length;
                if (to <= indexInBuffer) {
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
                }
                indexInBuffer += length;
            } else {
                break;
            }
            if (indexInBuffer >= FastLzzCompressor.MEMORY_BUFFER_SIZE) {
                if (indexInBuffer != FastLzzCompressor.MEMORY_BUFFER_SIZE) throw new RuntimeException();
                totalUmpIndex += indexInBuffer;
                indexInBuffer = 0;
                fos.write(outBuffer);
                fis.alignByte();
            }
            totalOutLength = totalUmpIndex + indexInBuffer;
        }
        if (indexInBuffer > 0) {
            fos.write(outBuffer, 0, indexInBuffer);
        }
    }

    @Override
    public long getOutputSize() {
        return totalOutLength;
    }

    @Override
    public void deleteCache() {
    }

    @Override
    public void setUnPacker(PzUnPacker unPacker) {
        this.unPacker = unPacker;
    }

    @Override
    public void setThreads(int threads) {
    }

    @Override
    public void uncompress(OutputStream outFile) throws IOException {
        uncompressMain(outFile);
        fis.close();
    }
}
