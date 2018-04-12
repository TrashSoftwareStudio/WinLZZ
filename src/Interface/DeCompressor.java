package Interface;

import Packer.UnPacker;

import java.io.OutputStream;

public interface DeCompressor {

    void Uncompress(OutputStream out) throws Exception;
    void setParent(UnPacker parent);
    void deleteCache();
    void setThreads(int threads);
}
