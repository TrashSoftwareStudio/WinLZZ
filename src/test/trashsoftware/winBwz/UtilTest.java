package trashsoftware.winBwz;

import java.io.IOException;

public abstract class UtilTest {

    public static void main(String[] args) throws IOException {
//        int a, b, c;
//        for (a = 0; a < 9; a++) {
//            for (b = 0; b <= a; b++) {
//                for (c = 0; c < a; c++) System.out.print(" ");
//            }
//            System.out.print("人\n");
//        }
        mx(2);
    }

    private static void mx(Object obj) {
        System.out.println(obj instanceof Integer);
    }
}
