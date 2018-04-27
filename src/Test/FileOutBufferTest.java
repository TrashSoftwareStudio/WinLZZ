package Test;

import WinLzz.Utility.FileOutputBufferArray;

import java.io.IOException;
import java.util.Arrays;

public abstract class FileOutBufferTest {

    public static void main(String[] args) throws IOException {
        FileOutputBufferArray fob = new FileOutputBufferArray("ntx.txt", 16);
        for (byte b = 0; b < 53; b++) {
            fob.write(b);
            if (b == (byte) 5) {
                System.out.println(Arrays.toString(fob.subSequence(1, 5)));
            }
        }
        fob.flush();
        fob.close();
    }
}
