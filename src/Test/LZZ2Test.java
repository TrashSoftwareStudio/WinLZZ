package Test;

import LZZ2.LZZ2Compressor;
import LZZ2.LZZ2DeCompressor;
import Utility.Util;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.SequenceInputStream;
import java.util.Vector;

public abstract class LZZ2Test {

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();

        String name;
        name = "dsCtrl.txt";
//        name = "p1.png";
//        name = "t1.bmp";
//        name = "t5.bmp";
//        name = "allCodes.zip";
//        name = "ep.head";
//        name = "t0.txt";
        String cmpName = Util.getCompressFileName(name, "lzz2");
//        Vector<FileInputStream> v = new Vector<>();
//        v.add(new FileInputStream(name));
//        SequenceInputStream sis = new SequenceInputStream(v.elements());
        int ws = 65536;
        LZZ2Compressor c = new LZZ2Compressor(name, ws, 286);
        c.setCompressionLevel(4);
        BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(cmpName));
        try {
            c.Compress(fos);
            System.out.println(c.getCompressedSize());
        } catch (Exception e) {
            //
        }
        fos.flush();
        fos.close();

        long mid = System.currentTimeMillis();
        System.out.println("Compress Time: " + (mid - start) + " ms");

        String cpyName = Util.getOriginalCopyName(cmpName);
        LZZ2DeCompressor d = new LZZ2DeCompressor(cmpName, ws);
        FileOutputStream bos = new FileOutputStream(cpyName);
        d.Uncompress(bos);
        bos.close();

        System.out.println("Uncompress Time: " + (System.currentTimeMillis() - mid) + " ms");
    }
}
