package WinLzz.Utility;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class FileOutputBufferArray {

    private OutputStream fos;

    private int bufferSize;

    private byte[] array1, array2;

    private boolean activeArray2, array1Init, array2Init = false;

    private int index, front = 0;

    public FileOutputBufferArray(String fileName, int bufferSize) throws IOException {
        this.fos = new FileOutputStream(fileName);
        this.bufferSize = bufferSize;
        this.array1 = new byte[bufferSize];
        this.array2 = new byte[bufferSize];
    }

    public FileOutputBufferArray(OutputStream fos, int bufferSize) {
        this.fos = fos;
        this.bufferSize = bufferSize;
        this.array1 = new byte[bufferSize];
        this.array2 = new byte[bufferSize];
    }

    public void write(byte b) throws IOException {
        if (index % bufferSize == 0) {
            activeArray2 = !activeArray2;
            if (activeArray2) {
                if (array2Init) fos.write(array2);
                else array2Init = true;
                array2[0] = b;
            } else {
                if (array1Init) fos.write(array1);
                else array1Init = true;
                array1[0] = b;
            }
            front += bufferSize;
        } else {
            if (activeArray2) array2[index % bufferSize] = b;
            else array1[index % bufferSize] = b;
        }
        index += 1;
    }

    public byte[] subSequence(int from, int to) {
        int len = to - from;
        byte[] rtn = new byte[len];
        if (to > index) throw new IndexOutOfBoundsException("Index Out Of Bounds: Index: " + index + ", To: " + to);
        else if (from > to) throw new NegativeArraySizeException("Negative sequence size");
        if (activeArray2) {
            if (front - from <= bufferSize) {
                System.arraycopy(array2, from % bufferSize, rtn, 0, len);
            } else if (front - from <= bufferSize * 2) {
                int remain = from % bufferSize;
                int firstLen = Math.min(bufferSize - remain, len);
                System.arraycopy(array1, remain, rtn, 0, firstLen);
                System.arraycopy(array2, 0, rtn, firstLen, len - firstLen);
            } else {
                throw new IndexOutOfBoundsException("Index Out Of Window");
            }
        } else {
            if (front - from <= bufferSize) {
                System.arraycopy(array1, from % bufferSize, rtn, 0, len);
            } else if (front - from <= bufferSize * 2) {
                int remain = from % bufferSize;
                int firstLen = Math.min(bufferSize - remain, len);
                System.arraycopy(array2, remain, rtn, 0, firstLen);
                System.arraycopy(array1, 0, rtn, firstLen, len - firstLen);
            } else {
                throw new IndexOutOfBoundsException("Index Out Of Window");
            }
        }

        return rtn;
    }

    public int getIndex() {
        return index;
    }

    public void flush() throws IOException {
        int len = index % bufferSize;
        if (activeArray2) {
            if (array1Init) fos.write(array1);
            fos.write(array2, 0, len);
        } else {
            if (array2Init) fos.write(array2);
            fos.write(array1, 0, len);
        }
    }

    public void close() throws IOException {
        this.fos.flush();
        this.fos.close();
    }
}
