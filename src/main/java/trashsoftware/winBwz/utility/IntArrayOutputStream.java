package trashsoftware.winBwz.utility;

import java.io.IOException;
import java.io.OutputStream;

public class IntArrayOutputStream extends OutputStream {
    
    private int[] buffer;
    private int index = 0;
    
    public IntArrayOutputStream(int capacity) {
        this.buffer = new int[capacity];
    }
    
    public IntArrayOutputStream() {
        this(32);
    }
    
    @Override
    public void write(int b) throws IOException {
        ensureCapacity();
        buffer[index++] = b;
    }
    
    private void ensureCapacity() {
        if (index >= buffer.length) {
            expand();
        }
    }
    
    private void expand() {
        int newCapacity = buffer.length << 1;
        int[] newBuf = new int[newCapacity];
        System.arraycopy(buffer, 0, newBuf, 0, index);
        buffer = newBuf;
    }
    
    public int[] toIntArray() {
        int[] res = new int[index];
        System.arraycopy(buffer, 0, res, 0, index);
        return res;
    }
}
