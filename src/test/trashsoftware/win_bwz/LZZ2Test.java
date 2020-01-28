package trashsoftware.win_bwz;

import trashsoftware.win_bwz.lzz2.LZZ2Compressor;
import trashsoftware.win_bwz.lzz2.LZZ2DeCompressor;
import trashsoftware.win_bwz.utility.ArrayHashTable;
import trashsoftware.win_bwz.utility.Util;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;

public class LZZ2Test {

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
        int ws = 32768;
        LZZ2Compressor c = new LZZ2Compressor(name, ws, 255);
        c.setCompressionLevel(2);
        BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(cmpName));
        try {
            c.compress(fos);
            System.out.println(c.getCompressedSize());
        } catch (Exception e) {
            //
        }
        fos.flush();
        fos.close();

        long mid = System.currentTimeMillis();
        long t1 = mid - start;
        System.out.println("compress Time: " + t1 + " ms");

        String cpyName = Util.getOriginalCopyName(cmpName);
        LZZ2DeCompressor d = new LZZ2DeCompressor(cmpName, ws);
        FileOutputStream bos = new FileOutputStream(cpyName);
        d.uncompress(bos);
        bos.close();

        long t2 = System.currentTimeMillis() - mid;
        System.out.println("Uncompress Time: " + t2 + " ms");
        System.out.println("C/U time ratio: " + (double) t1 / t2);
    }
}