package trashsoftware.win_bwz;

import trashsoftware.win_bwz.Encrypters.BZSE.BZSEStreamDecoder;
import trashsoftware.win_bwz.Encrypters.BZSE.BZSEStreamEncoder;
import trashsoftware.win_bwz.Utility.Util;

import java.io.*;

public class BZSETest {

    public static void main(String[] args) throws IOException {
        long st = System.currentTimeMillis();

        String name = "allCodes.zip";
        String encName = Util.getCompressFileName(name, "bzse");
        String copyName = Util.getOriginalCopyName(encName);
        String password = "password";
        BufferedInputStream bis1 = new BufferedInputStream(new FileInputStream(name));
        BZSEStreamEncoder be = new BZSEStreamEncoder(bis1, password);
        BufferedOutputStream bos1 = new BufferedOutputStream(new FileOutputStream(encName));
        be.encrypt(bos1);
        long len = be.encryptedLength();
        bis1.close();
        bos1.close();

        long mid = System.currentTimeMillis();
        System.out.println(mid - st);

        BufferedInputStream bis2 = new BufferedInputStream(new FileInputStream(encName));
        BZSEStreamDecoder bd = new BZSEStreamDecoder(bis2, password, len);
        BufferedOutputStream bos2 = new BufferedOutputStream(new FileOutputStream(copyName));
        bd.decrypt(bos2);
        bis2.close();
        bos2.close();

        System.out.println(System.currentTimeMillis() - mid );
    }
}
