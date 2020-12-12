package trashsoftware.winBwz.utility;

import java.io.IOException;
import java.io.OutputStream;

public class FixedByteArrayOutputStream extends OutputStream {

    private byte[] buffer;
    private int index;

    public FixedByteArrayOutputStream(int maxSize) {
        buffer = new byte[maxSize];
    }

    @Override
    public void write(int b) throws IOException {
        buffer[index++] = (byte) b;
    }

    public int getLength() {
        return index;
    }

    public void writeToStream(OutputStream outputStream) throws IOException {
        outputStream.write(buffer, 0, index);
    }
}
