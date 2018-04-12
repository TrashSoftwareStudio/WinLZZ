package Utility;

import java.io.BufferedInputStream;
import java.io.IOException;

public class FileBitInputStream {

    private BufferedInputStream bis;

    private int index = 0;

    private String currentByte;

    public FileBitInputStream(BufferedInputStream bis) {
        this.bis = bis;
    }

    public char read() throws IOException {
        char c;
        if (index % 8 == 0) {
            byte[] b = new byte[1];
            if (bis.read(b) != 1) return '2';
            currentByte = Bytes.byteToBitString(b[0]);
            c = currentByte.charAt(0);
        } else {
            c = currentByte.charAt(index % 8);
        }
        index += 1;
        return c;
    }

    public void close() throws IOException {
        bis.close();
    }

}
