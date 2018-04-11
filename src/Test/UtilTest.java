package Test;

import Utility.Bytes;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;

public abstract class UtilTest {

    public static void main(String[] args) throws IOException {
        int a = 131342;
        short[] s = Bytes.intToShorts(a);
        int c = Bytes.shortArrayToInt(s);
        System.out.println(c);
    }
}
