package trashsoftware.win_bwz;

import trashsoftware.win_bwz.core.lzz2.Lzz2HuffmanInputStream;
import trashsoftware.win_bwz.huffman.HuffmanCompressorTwoBytes;

import java.io.*;
import java.util.Arrays;

public class HuffmanTest {

    public static void main(String[] args) throws IOException {
        byte[] origText = new byte[48509];
        FileInputStream fis = new FileInputStream("dsCtrl.txt");
        fis.read(origText);
        fis.close();
        byte[] a = new byte[origText.length * 2];
        for (int i = 0; i < origText.length; i++) {
            a[i * 2] = 0;
            a[i * 2 + 1] = origText[i];
        }
        String name = "hufIn";
        FileOutputStream fos = new FileOutputStream(name);
        fos.write(a);
        fos.flush();
        fos.close();

        ByteArrayOutputStream bao = new ByteArrayOutputStream();

        HuffmanCompressorTwoBytes hct = new HuffmanCompressorTwoBytes(name);
        byte[] map = hct.getMap(257);
        hct.SepCompress(bao);
        bao.flush();
        bao.close();

        byte[] array = bao.toByteArray();
        System.out.println(Arrays.toString(array));

        ByteArrayInputStream bis = new ByteArrayInputStream(array);

        Lzz2HuffmanInputStream his = new Lzz2HuffmanInputStream(map, bis);
        while (true) {
            int r = his.readNext();
            System.out.print(r + " ");
            if (r == 256) {
                break;
            }
        }
        his.close();
    }
}
