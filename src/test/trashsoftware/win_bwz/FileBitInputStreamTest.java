package trashsoftware.win_bwz;

import trashsoftware.win_bwz.Utility.FileBitInputStream;
import trashsoftware.win_bwz.Utility.Bytes;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;

public abstract class FileBitInputStreamTest {

    public static void main(String[] args) throws IOException {
        String name = "d.cmp";
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(name));
        FileBitInputStream fbi = new FileBitInputStream(bis);
        for (int i = 0; i < 100; i ++) {
            System.out.print(fbi.read());
        }
        System.out.println();
        bis.reset();
        byte[] b = new byte[25];
        bis.read(b);
        StringBuilder sb = new StringBuilder();
        for (int i = 0 ; i < 25; i++) {
            sb.append(Bytes.byteToBitString(b[i]));
        }
        System.out.println(sb);
    }
}
