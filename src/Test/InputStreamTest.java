package Test;

import WinLzz.Utility.MultipleInputStream;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;

public class InputStreamTest {

    public static void main(String[] args) throws IOException {
        LinkedList<File> files = new LinkedList<>();
        files.addLast(new File("a.txt"));
        files.addLast(new File("a0.txt"));
        files.addLast(new File("a1.txt"));
        files.addLast(new File("a2.txt"));
        MultipleInputStream mis = new MultipleInputStream(files, null);
        byte[] arr = new byte[16];
        System.out.println(mis.read(arr));
        System.out.println(new String(arr));
        System.out.println(mis.read(arr));
        System.out.println(new String(arr));
        System.out.println(mis.read(arr));
        System.out.println(new String(arr));
    }
}
