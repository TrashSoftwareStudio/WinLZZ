package Interface;

import Packer.Packer;

import java.io.OutputStream;

public interface Compressor {

    void Compress(OutputStream out) throws Exception;
    void setParent(Packer parent);
    void setThreads(int threads);
    void setCompressionLevel(int level);
    long getCompressedSize();
}
