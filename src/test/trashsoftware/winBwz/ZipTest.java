package trashsoftware.winBwz;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipOutputStream;

public class ZipTest {

    public static void main(String[] args) throws IOException {
        String name = "";
        ZipOutputStream zip = new ZipOutputStream(new FileOutputStream("z.zip"));

    }
}
