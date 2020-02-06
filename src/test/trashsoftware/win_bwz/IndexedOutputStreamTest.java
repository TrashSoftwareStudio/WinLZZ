package trashsoftware.win_bwz;

import trashsoftware.win_bwz.utility.IndexedOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class IndexedOutputStreamTest {

    public static void main(String[] args) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        IndexedOutputStream ios = new IndexedOutputStream(bos, 8);
        for (int i = 0; i < 29; i++) {
            ios.writeOne((byte) i);
        }
        ios.printBuffers();
        ios.copyRepeat(22, 5);

        System.out.println(ios.getIndex());

        ios.flush();
        ios.close();

        byte[] bytes = bos.toByteArray();
        System.out.println(Arrays.toString(bytes));
    }
}
