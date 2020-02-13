package trashsoftware.win_bwz;

import trashsoftware.win_bwz.utility.FileBitOutputStream;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileBitOutputStreamTest {

    public static void main(String[] args) throws IOException {
        BufferedOutputStream bos1 = new BufferedOutputStream(new FileOutputStream("bos1"));
        FileBitOutputStream fbo1 = new FileBitOutputStream(bos1);
//
        long t0 = System.currentTimeMillis();
//
        for (int i = 0; i < 10000000; ++i) {
            int r = i % 256;
            fbo1.writeByte((byte) r);
        }

        fbo1.flush();
        long t1 = System.currentTimeMillis();

        fbo1.close();

//        BufferedOutputStream bos2 = new BufferedOutputStream(new FileOutputStream("bos1"));
        long t2 = System.currentTimeMillis();

//        for (int i = 0; i < 10000000; ++i) {
//            int r = i % 256;
//            bos2.write((byte) r);
//        }
//
//        bos2.flush();
        long t3 = System.currentTimeMillis();
//        bos2.close();

        System.out.println("T1: " + (t1 - t0) + " T2: " + (t3 - t2));

    }
}
