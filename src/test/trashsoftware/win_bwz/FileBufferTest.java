package trashsoftware.win_bwz;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

public abstract class FileBufferTest {

    public static void main(String[] args) throws IOException {
        long st = System.currentTimeMillis();
        int len = 3476;
//        FileInputBufferArray fba = new FileInputBufferArray("dsCtrl.txt.dlb.temp", 8192);
        byte[] a = new byte[len];
        byte[] b = new byte[len];
        for (int i = 0; i < len; i++) {
//            a[i] = fba.getByte(i);
//            System.out.print(a[i] + " ");
            //fba.getByte(i - 32768);
        }
        System.out.println(System.currentTimeMillis() - st);

        FileInputStream fis = new FileInputStream("dsCtrl.txt.lzz2.dlb.cmp");
        fis.read(b);
//        System.out.println(Arrays.toString(a));
//        System.out.println(Arrays.toString(b));
        System.out.println(Arrays.equals(a, b));
        fis.close();
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream("dsCtrl.txt.lzz2.dis.temp"));
        byte[] c = new byte[1];
        for (int i = 0 ; i < 2952; i++) {
            bis.read(c);
            System.out.print(c[0] + " ");
        }
    }

}
