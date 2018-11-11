package WinLzz.LZZ3;

import WinLzz.Interface.Compressor;
import WinLzz.Packer.Packer;

import java.io.InputStream;
import java.io.OutputStream;

public class LZZ3Compressor implements Compressor {

    private InputStream inputStream;

    private long streamLength;

    private int windowSize;

    public LZZ3Compressor(InputStream inputStream, long streamLength, int windowSize) {
        this.inputStream = inputStream;
        this.streamLength = streamLength;
        this.windowSize = windowSize;
    }

    @Override
    public void compress(OutputStream out) {

    }

    @Override
    public long getCompressedSize() {
        return 0;
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
}
