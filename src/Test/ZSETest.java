package Test;

import ZSE.ZSEFileDecoder;
import ZSE.ZSEFileEncoder;

import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.util.Arrays;

public abstract class ZSETest {

    public static void main(String[] args) throws Exception {

//        byte a = (byte) -255;
//
//        System.out.println(a);

        ZSEFileEncoder zfe = new ZSEFileEncoder("dsCtrl.txt", "abcd");
        FileOutputStream fos = new FileOutputStream("ds.zse");
        zfe.Encode(fos);
        fos.flush();
        fos.close();

//        ZSEFileDecoder zfd = new ZSEFileDecoder("ds.zse", "abcd");
//        FileOutputStream fos2 = new FileOutputStream("ds.txt");
//        zfd.Decode(fos2);
//        fos2.flush();
//        fos2.close();
    }
}
