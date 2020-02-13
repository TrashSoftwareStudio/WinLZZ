package trashsoftware.win_bwz;

import trashsoftware.win_bwz.core.fastLzz.FastLzzCompressor;
import trashsoftware.win_bwz.core.fastLzz.FastLzzDecompressor;
import trashsoftware.win_bwz.core.fasterLzz.FasterLzzCompressor;
import trashsoftware.win_bwz.utility.Util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;

public class FasterLzzTest {

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();

        byte[] extraLitBuf = new byte[64];
        int litCount = 15 + 255;
        int tokenLit;
        int extraLitLen = 0, extraLenLen = 0;


//        System.exit(0);

        String name;
        name = "dsCtrl.txt";
//        name = "p1.png";
//        name = "t1.bmp";
//        name = "t5.bmp";
//        name = "allCodes.zip";
//        name = "ep.head";
//        name = "t0.txt";
//        name = "t3.tif";
        name = "cf.zip";
        String cmpName = Util.getCompressFileName(name, "flz");
//        Vector<FileInputStream> v = new Vector<>();
//        v.add(new FileInputStream(name));
//        SequenceInputStream sis = new SequenceInputStream(v.elements());
        int ws = 65536;
        FasterLzzCompressor c = new FasterLzzCompressor(name, ws, 255);
        c.setThreads(1);
        c.setCompressionLevel(0);
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
        FastLzzDecompressor d = new FastLzzDecompressor(cmpName, ws);
        FileOutputStream bos = new FileOutputStream(cpyName);
//        d.uncompress(bos);
        bos.close();

        long t2 = System.currentTimeMillis() - mid;
        System.out.println("Uncompress Time: " + t2 + " ms");
        System.out.println("C/U time ratio: " + (double) t1 / t2);

        BufferedInputStream checkBis = new BufferedInputStream(new FileInputStream(name));
        BufferedInputStream checkBis2 = new BufferedInputStream(new FileInputStream(cpyName));
        int read;
        byte[] buffer = new byte[8192];
        byte[] buffer2 = new byte[8192];
        int begin = 0;
        while ((read = checkBis.read(buffer)) > 0) {
            int read2 = checkBis2.read(buffer2);
            if (read != read2) {
                System.out.println("Lengths not match");
                break;
            }
            if (!Arrays.equals(buffer, buffer2)) {
                System.out.println("Content not match between " + begin + " and " + (begin + read));
                break;
            }

            begin += read;
        }
        checkBis.close();
        checkBis2.close();
    }
}
