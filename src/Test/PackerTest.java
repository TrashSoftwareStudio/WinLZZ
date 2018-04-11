package Test;

import Packer.Packer;
import Packer.UnPacker;

import java.io.File;

public abstract class PackerTest {

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        Packer p = new Packer(new File("E:\\Programs\\allCodes"));
        p.Pack("E:\\Programs\\allCodes.pz", 262144, 286);
        long mid = System.currentTimeMillis();
        System.out.println("Pack time: " + (mid - start));

        UnPacker up = new UnPacker("E:\\Programs\\allCodes.pz");
        up.readInfo();
//        up.UncompressAll("E:\\Programs\\result");
        up.unCompressFrom("E:\\Programs\\result", up.getRootNode());
        up.close();
        System.out.println("Unpack time: " + (System.currentTimeMillis() - mid));

    }
}
