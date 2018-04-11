package Utility;

import java.io.BufferedInputStream;
import java.io.IOException;

public class ShortInputStream {

    private BufferedInputStream bis;

    private byte[] readBuffer = new byte[2];

    public ShortInputStream(BufferedInputStream bis) {
        this.bis = bis;
    }

    public short read() throws IOException {
        if (bis.read(readBuffer) != 2) {
            throw new IOException("Error occurs while reading");
        }
        return Bytes.bytesToShort(readBuffer);
    }

    public void close() throws IOException {
        bis.close();
    }
}
