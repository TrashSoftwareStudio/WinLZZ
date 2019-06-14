package trashsoftware.win_bwz.LZZ3;

import trashsoftware.win_bwz.Interface.Compressor;
import trashsoftware.win_bwz.Packer.Packer;
import trashsoftware.win_bwz.Utility.FileInputBufferArray;

import java.io.InputStream;
import java.io.OutputStream;

public class LZZ3Compressor implements Compressor {

    private InputStream inputStream;

    private long streamLength;

    private int windowSize;

    private int labSize;

    public LZZ3Compressor(InputStream inputStream, long streamLength, int windowSize, int labSize) {
        this.inputStream = inputStream;
        this.streamLength = streamLength;
        this.windowSize = windowSize;
        this.labSize = labSize;
    }

    private void compressText() {
        FileInputBufferArray fba = new FileInputBufferArray(inputStream, streamLength, windowSize);
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
