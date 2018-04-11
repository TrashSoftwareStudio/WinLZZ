package Test;

import Huffman.HuffmanCompressor;
import Huffman.HuffmanDeCompressor;
import Utility.Util;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;

public abstract class HuffmanMain {

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();

        String name = "allCodes.zip";
//        name = "t0.txt";
        String cmpName = Util.getCompressFileName(name, "huf");
        HuffmanCompressor hc = new HuffmanCompressor(name);
        BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(cmpName));
        hc.Compress(fos);
        fos.flush();
        fos.close();

        System.out.println(hc.getCompressedLength());

        long mid = System.currentTimeMillis();
        System.out.println("Compress Time: " + (mid - start) + " ms");

        String cpyName = Util.getOriginalCopyName(cmpName);
        HuffmanDeCompressor hdc = new HuffmanDeCompressor(cmpName);
        hdc.Uncompress(cpyName);

        System.out.println("Uncompress Time: " + (System.currentTimeMillis() - mid) + " ms");
    }
}
