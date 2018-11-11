package Test;

import WinLzz.Utility.SeparateInputStream;
import WinLzz.Utility.SeparateOutputStream;

import java.io.IOException;
import java.util.Arrays;

public class SeparateOutputStreamTest {

    public static void main(String[] args) throws IOException {
        SeparateOutputStream sos = new SeparateOutputStream("block.txt", 8, true);
        sos.write("abc".getBytes());
        sos.write("def".getBytes());
        sos.write("ghijklmnopqrs".getBytes());
        sos.flush();
        sos.close();

        SeparateInputStream sis = SeparateInputStream.createNew("block", ".txt", 4, null, 4);
        byte[] b = new byte[6];
//        System.out.println(sis.skip(1));
        int read;
        while ((read = sis.read(b)) > 0) {
            System.out.println(new String(b) + " " + read);
        }
        sis.close();
    }
}
