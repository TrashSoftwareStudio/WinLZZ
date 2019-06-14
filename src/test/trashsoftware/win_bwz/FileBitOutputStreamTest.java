package trashsoftware.win_bwz;

import trashsoftware.win_bwz.Utility.FileBitOutputStream;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileBitOutputStreamTest {

    public static void main(String[] args) throws IOException {
        BufferedOutputStream bos1 = new BufferedOutputStream(new FileOutputStream("bos1"));
        FileBitOutputStream fbo1 = new FileBitOutputStream(bos1);
        fbo1.writeByte((byte) 98);
        fbo1.flush();
//        BufferedOutputStream bos2 = new BufferedOutputStream(new FileOutputStream("bos2"));
//        FileBitOutputStream fbo2 = new FileBitOutputStream(bos2);
//
//        long st1 = System.currentTimeMillis();
//        for (int i = 0; i < 50000000; i++) {
//            fbo1.write(0);
//            fbo1.write(1);
//        }
//        long mid = System.currentTimeMillis();
//        System.out.println(mid - st1);
//        for (int i = 0; i < 50000000; i++) {
//            fbo2.write('0');
//            fbo2.write('1');
//        }
//        System.out.println(System.currentTimeMillis() - mid);
    }
}
