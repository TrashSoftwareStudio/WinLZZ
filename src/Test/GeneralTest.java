package Test;

import java.util.zip.CRC32;

public abstract class GeneralTest {

    public static void main(String[] args) throws Exception {
        byte[] a = new byte[]{3, 33, 3, 33, 3, 33, 4};
        byte[] b = new byte[]{3, 33, 3, 33, 3, 33, 4};
        CRC32 c = new CRC32();
        c.update(a);
        System.out.println(c.getValue());
        c.reset();
        c.update(b);
        System.out.println(c.getValue());

    }
}
