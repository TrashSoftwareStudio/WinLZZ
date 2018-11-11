package Test;

import WinLzz.Utility.Security;

import java.util.Arrays;

public class SecurityTest {

    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) System.out.println(Arrays.toString(Security.generateRandomSequence()));
    }
}
