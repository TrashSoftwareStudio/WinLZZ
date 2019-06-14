package trashsoftware.win_bwz;

import java.io.IOException;
import java.util.zip.CRC32;

public abstract class UtilTest {

    public static void main(String[] args) throws IOException {
        byte[] a = new byte[]{1, 2, 3, 4};
        CRC32 crc = new CRC32();
        crc.update(a);
        System.out.println(crc.getValue());

        byte[] b = new byte[]{1, 2};
        byte[] c = new byte[]{3, 4};
        CRC32 crc2 = new CRC32();
        crc2.update(b);
        crc2.update(c);
        System.out.println(crc2.getValue());
    }
}
