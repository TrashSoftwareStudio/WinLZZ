package trashsoftware.winBwz;

import trashsoftware.winBwz.core.lzz2.LZZ2Compressor;
import trashsoftware.winBwz.core.lzz2.LZZ2DeCompressor;
import trashsoftware.winBwz.utility.Util;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;

public class LZZ2Test {

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();

        String name;
//        name = "dsCtrl.txt";
        name = "2.py";
//        name = "p1.png";
//        name = "t1.bmp";
//        name = "t5.bmp";
//        name = "allCodes.zip";
//        name = "ep.head";
//        name = "t0.txt";
//        name = "t4.tif";
        String cmpName = Util.getCompressFileName(name, "lzz2");
//        Vector<FileInputStream> v = new Vector<>();
//        v.add(new FileInputStream(name));
//        SequenceInputStream sis = new SequenceInputStream(v.elements());
        int ws = 32768;
        LZZ2Compressor c = new LZZ2Compressor(name, ws, 255);
        c.setCompressionLevel(4);
        BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(cmpName));
        try {
            c.compress(fos);
            System.out.printf("Size after compression: %d, compress rate: %.2f%%%n",
                    c.getOutputSize(), (double) c.getOutputSize() / c.getSizeBeforeCompression() * 100);
        } catch (Exception e) {
            //
        }
        fos.flush();
        fos.close();

        long mid = System.currentTimeMillis();
        long t1 = mid - start;
        System.out.println("compress Time: " + t1 + " ms");

//        System.exit(0);

        String cpyName = Util.getOriginalCopyName(cmpName);
        LZZ2DeCompressor d = new LZZ2DeCompressor(cmpName, ws);
        FileOutputStream bos = new FileOutputStream(cpyName);
        d.uncompress(bos);
        bos.close();

        long t2 = System.currentTimeMillis() - mid;
        System.out.println("Uncompress Time: " + t2 + " ms");
        System.out.println("C/U time ratio: " + (double) t1 / t2);

        System.out.println(Util.checkFileIdentical(name, cpyName));
    }
}
