package Utility;

import java.io.*;

public class FileInputBufferArray {

    private InputStream bis;

    private int bufferSize;

    private int remainSize = 0;

    private int index;

    private byte[] array1;

    private byte[] array2;

    private boolean activeArray2 = false;

    private long length;

    public FileInputBufferArray(InputStream fis, long length, int bufferSize) {
        this.bufferSize = bufferSize;
        this.length = length;
        this.bis = fis;
        array1 = new byte[bufferSize];
        array2 = new byte[bufferSize];
    }

    public byte getByte(int index) {
        if (index >= this.index) {
            activeArray2 = !activeArray2;
            if (activeArray2) {
                try {
                    int read = bis.read(array2, 0, bufferSize);
                    if (read > 0) {
                        this.index += read;
                        remainSize = read;
                    } else if (read == -1) {
                        throw new IndexOutOfBoundsException("Index out of range");
                    }
                } catch (IOException e) {
                    throw new IndexOutOfBoundsException();
                }
            } else {
                try {
                    int read = bis.read(array1, 0, bufferSize);
                    if (read > 0) {
                        this.index += read;
                        remainSize = read;
                    } else if (read == -1) {
                        throw new IndexOutOfBoundsException("Index out of range");
                    }
                } catch (IOException e) {
                    throw new IndexOutOfBoundsException();
                }
            }
            return getByte(index);
        } else if (index >= this.index - remainSize) {
            // else if (index < this.index && index >= this.index - remainSize)
            if (activeArray2) return array2[index - this.index + remainSize];
            else return array1[index - this.index + remainSize];
        } else if (index < this.index - remainSize) {
            if (activeArray2) return array1[index - this.index + bufferSize + remainSize];
            else return array2[index - this.index + bufferSize + remainSize];
        } else {
            throw new IndexOutOfBoundsException("Index out of buffer range");
        }
    }

    public void close() throws IOException {
        bis.close();
    }

    public long length() {
        return length;
    }
}
