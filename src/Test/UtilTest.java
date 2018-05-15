package Test;

import WinLzz.Utility.Bytes;
import WinLzz.Utility.Util;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public abstract class UtilTest {

    public static void main(String[] args) throws IOException {
        String [] s = "D:\\X".split(String.format("%s%s", "\\", File.separator));
        System.out.println(Arrays.toString(s));
    }
}
