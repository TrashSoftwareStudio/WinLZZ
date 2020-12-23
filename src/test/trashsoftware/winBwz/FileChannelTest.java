package trashsoftware.winBwz;

import java.io.FileInputStream;
import java.nio.channels.FileChannel;

public class FileChannelTest {

    public static void main(String[] args) throws Exception {
        FileInputStream fis = new FileInputStream("dsCtrl.txt");
        FileChannel fc = fis.getChannel();
        fc.position(30);
        byte[] buf = new byte[20];
        fis.read(buf);
        System.out.println(new String(buf));
        System.out.println(fc.position());
        fc.position(2);
        fis.read(buf);
        System.out.println(new String(buf));

        fc.close();
    }
}
