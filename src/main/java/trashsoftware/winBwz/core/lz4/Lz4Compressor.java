package trashsoftware.winBwz.core.lz4;

import trashsoftware.winBwz.core.Compressor;
import trashsoftware.winBwz.packer.Packer;
import trashsoftware.winBwz.utility.Bytes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Lz4Compressor implements Compressor {

    private static int HASH_LOG = 12;
    private static int MIN_MATCH = 4;
    private static long PRIME = 2654435761L;

    private InputStream inputStream;
    private long compressedLength;
    private long totalLength;

    public Lz4Compressor(InputStream inputStream, long totalLength) {
        this.inputStream = inputStream;
        this.totalLength = totalLength;
    }

    private void compressContent(OutputStream out) throws IOException {
        long index = 0;
        byte[] readBuf = new byte[4];
        while (index < totalLength) {

        }
    }

    @Override
    public void compress(OutputStream out) throws Exception {
        compressContent(out);
    }

    @Override
    public void setParent(Packer parent) {

    }

    @Override
    public void setThreads(int threads) {

    }

    @Override
    public void setCompressionLevel(int level) {

    }

    @Override
    public long getCompressedSize() {
        return 0;
    }

    private static int hashPosition(byte[] buffer, int index) {
        return hash16bits(Bytes.bytesToInt32(buffer, index));
    }

    private static int hash16bits(long sequence) {
        return (int) (((sequence) * PRIME) >> ((MIN_MATCH * 8) - (HASH_LOG)));
    }

    public static void main(String[] args) {
        byte[] r = {-127, 3, 22, 100};
        int hash = hashPosition(r, 0);
        System.out.println(hash);
    }
}
