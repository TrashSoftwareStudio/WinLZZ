package Utility;

import java.io.BufferedOutputStream;
import java.io.IOException;

public class ShortOutputStream {

    private BufferedOutputStream bos;

    public ShortOutputStream(BufferedOutputStream bos) {
        this.bos = bos;
    }

    public void write(short s) throws IOException {
        byte[] b = Bytes.shortToBytes(s);
        bos.write(b);
    }

    public void write(short[] s) throws IOException {
        byte[] b = Bytes.shortArrayToByteArray(s);
        bos.write(b);
    }

    public void flush() throws IOException {
        bos.flush();
    }

    public void close() throws IOException {
        bos.close();
    }
}
