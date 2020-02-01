package trashsoftware.win_bwz;

import trashsoftware.win_bwz.core.lzz2_plus.Lzz2PlusCompressor;
import trashsoftware.win_bwz.core.lzz2_plus.Lzz2PlusDecompressor;
import trashsoftware.win_bwz.utility.Util;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;

public class Lzz2PlusTest {

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
        String cmpName = Util.getCompressFileName(name, "lzz2p");
//        Vector<FileInputStream> v = new Vector<>();
//        v.add(new FileInputStream(name));
//        SequenceInputStream sis = new SequenceInputStream(v.elements());
        int ws = 32768;
        Lzz2PlusCompressor c = new Lzz2PlusCompressor(name, ws, 255);
        c.setCompressionLevel(1);
        BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(cmpName));
        try {
            c.compress(fos);
            System.out.println(c.getCompressedSize());
        } catch (Exception e) {
            e.printStackTrace();
        }
        fos.flush();
        fos.close();

        long mid = System.currentTimeMillis();
        long t1 = mid - start;
        System.out.println("compress Time: " + t1 + " ms");

        String cpyName = Util.getOriginalCopyName(cmpName);
        Lzz2PlusDecompressor d = new Lzz2PlusDecompressor(cmpName, ws);
        FileOutputStream bos = new FileOutputStream(cpyName);
        d.uncompress(bos);
        bos.close();

        long t2 = System.currentTimeMillis() - mid;
        System.out.println("Uncompress Time: " + t2 + " ms");
        System.out.println("C/U time ratio: " + (double) t1 / t2);
    }
}
