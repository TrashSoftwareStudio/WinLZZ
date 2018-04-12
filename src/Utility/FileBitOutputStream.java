package Utility;

import java.io.BufferedOutputStream;
import java.io.IOException;

public class FileBitOutputStream {

    private BufferedOutputStream bos;

    private char[] currentByte = new char[8];

    private short pointer = 0;

    public FileBitOutputStream(BufferedOutputStream bos) {
        this.bos = bos;
    }

    public void write(char bit) throws IOException {
        currentByte[pointer] = bit;
        if (pointer != 7) {
            pointer += 1;
        } else {
            bos.write(charArrayToByte());
            pointer = 0;
        }
    }

    public void write(String s) throws IOException {
        for (int i = 0; i < s.length(); i++) write(s.charAt(i));
    }

    private byte charArrayToByte() {
        return Bytes.bitStringToByte(String.valueOf(currentByte));
    }

    public void flush() throws IOException {
        if (pointer != 0) {
            char[] remain = new char[pointer];
            System.arraycopy(currentByte, 0, remain, 0, pointer);
            byte b = Bytes.bitStringToByteNo8(String.valueOf(remain));
            bos.write(b);
        }
        bos.flush();
    }

    public void close() throws IOException {
        bos.close();
    }
}
