package trashsoftware.winBwz;

import trashsoftware.winBwz.packer.ZipPacker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipOutputStream;

public class ZipTest {

    public static void main(String[] args) throws Exception {
        String name = "cache";
        ZipPacker zipPacker = new ZipPacker(new File[]{new File(name)});
        zipPacker.build();
        zipPacker.pack("dsc.zip", 32768, 64);
    }
}
