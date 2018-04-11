package Test;

import QuickLZZ.QuickLZZCompressor;
import QuickLZZ.QuickLZZDeCompressor;
import Utility.Util;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;

public class QuickLZZTest {

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();

        String name;
        name = "dsCtrl.txt";
//        name = "p1.png";
//        name = "t1.bmp";
//        name = "t5.bmp";
//        name = "allCodes.zip";
        name = "ep.head";
        String cmpName = Util.getCompressFileName(name, "qlz");
//        Vector<FileInputStream> v = new Vector<>();
//        v.add(new FileInputStream(name));
//        SequenceInputStream sis = new SequenceInputStream(v.elements());
        int ws = 32768;
        QuickLZZCompressor c = new QuickLZZCompressor(name, ws);
//        c.setCompressionLevel(0);
        BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(cmpName));
        try {
            c.Compress(fos);

            System.out.println(c.getCompressedSize());
        } catch (Exception e) {
            e.printStackTrace();
        }
        fos.flush();
        fos.close();

        long mid = System.currentTimeMillis();
        System.out.println("Compress Time: " + (mid - start) + " ms");

        String cpyName = Util.getOriginalCopyName(cmpName);
        QuickLZZDeCompressor d = new QuickLZZDeCompressor(cmpName, ws);
        FileOutputStream bos = new FileOutputStream(cpyName);
        d.Uncompress(bos);
        bos.close();

        System.out.println("Uncompress Time: " + (System.currentTimeMillis() - mid) + " ms");
    }
}
