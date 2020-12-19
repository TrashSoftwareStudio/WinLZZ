package trashsoftware.winBwz;

import trashsoftware.winBwz.packer.zip.ZipPacker;

import java.io.File;

public class ZipTest {

    public static void main(String[] args) throws Exception {
        String name = "cache";
        ZipPacker zipPacker = new ZipPacker(new File[]{new File(name)});
        zipPacker.build();
        zipPacker.pack("dsc.zip", 32768, 64);
    }
}
