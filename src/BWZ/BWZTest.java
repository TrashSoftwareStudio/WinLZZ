package BWZ;

import Utility.Util;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class BWZTest {

    public static void main(String[] args) throws Exception {

        String name;
        name = "dsCtrl.txt";
//        name = "p1.png";
//        name = "t1.bmp";
//        name = "cmpFiles.tar";
        int ws = 8192;
        String cmpName = Util.getCompressFileName(name, "bwz");
        long start = System.currentTimeMillis();

        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(cmpName));
        BWZCompressor bwz = new BWZCompressor(name, ws);
        bwz.Compress(bos);
        bos.flush();
        bos.close();

        long mid = System.currentTimeMillis();
        System.out.println("Size after compression: " + bwz.getCompressedSize());
        System.out.println("Compression time: " + (mid - start) + " ms");

        String cpyName = Util.getOriginalCopyName(cmpName);
        BWZDeCompressor d = new BWZDeCompressor(cmpName, ws);
        FileOutputStream bos2 = new FileOutputStream(cpyName);
        d.Uncompress(bos2);
        bos.close();

        System.out.println("Uncompress Time: " + (System.currentTimeMillis() - mid) + " ms");

    }
}
