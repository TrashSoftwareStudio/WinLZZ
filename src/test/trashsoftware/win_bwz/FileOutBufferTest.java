package trashsoftware.win_bwz;

import trashsoftware.win_bwz.utility.FileOutputBufferArray;
import trashsoftware.win_bwz.utility.IndexedOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public abstract class FileOutBufferTest {

    public static void main(String[] args) throws IOException {
        ByteArrayOutputStream fob2 = new ByteArrayOutputStream();
        IndexedOutputStream ios = new IndexedOutputStream(fob2, 8192);
        long t2 = System.currentTimeMillis();
        for (int i = 0; i < 1048576; ++i) {
            ios.writeOne((byte) (i & 0xff));
            if (Math.random() > 0.96) {
                long from = Math.max(ios.getIndex() - (long) (Math.random() * 8192), 0);
                int len = (int) (Math.random() * (ios.getIndex() - from));
                if (len > 0)
                    ios.copyRepeat(from, len);
            }
        }
        ios.flush();
        ios.close();
        long t3 = System.currentTimeMillis();
        System.out.println("Time used: " + (t3 - t2) + "ms, length: " + ios.getIndex());



        ByteArrayOutputStream fob = new ByteArrayOutputStream();
        FileOutputBufferArray fba = new FileOutputBufferArray(fob, 8192);
        long t0 = System.currentTimeMillis();
        for (int i = 0; i < 1048576; ++i) {
            fba.write((byte) (i & 0xff));
            if (Math.random() > 0.96) {
                long from = Math.max(fba.getIndex() - (long) (Math.random() * 8192), 0);
                int len = (int) (Math.random() * (fba.getIndex() - from));
                if (len > 0)
                    fba.copyRepeat(from, len);
            }
        }
        fob.flush();
        fob.close();
        long t1 = System.currentTimeMillis();
        System.out.println("Time used: " + (t1 - t0) + "ms, length: " + fba.getIndex());

        System.out.println("Ratio: " + ((double) (t1 - t0) / (t3 - t2)));
    }
}
