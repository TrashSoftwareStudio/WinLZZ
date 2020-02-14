package trashsoftware.win_bwz;

import java.io.IOException;
import java.util.zip.CRC32;

public abstract class UtilTest {

    public static void main(String[] args) throws IOException {
        int a, b, c;
        for (a = 0; a < 9; a++) {
            for (b = 0; b <= a; b++) {
                for (c = 0; c < a; c++) System.out.print(" ");
            }
            System.out.print("人\n");
        }
    }
}
