package trashsoftware.winBwz;

import trashsoftware.winBwz.utility.LengthOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class LengthOSTest {

    public static void main(String[] args) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        LengthOutputStream los = new LengthOutputStream(os, 8);
        los.write(66);
        los.write(new byte[]{32, 14, 33});
        System.out.println(los.getWrittenLength());
        los.write(new byte[]{77, 66, 55, 44, 33, 22, 11, 0, 99, 88, 78, 67}, 0, 12);

        los.flush();
        los.close();

        System.out.println(Arrays.toString(os.toByteArray()));
    }
}
