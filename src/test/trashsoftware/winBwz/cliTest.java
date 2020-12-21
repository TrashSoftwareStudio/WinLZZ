package trashsoftware.winBwz;

import trashsoftware.winBwz.packer.pz.PzSolidPacker;

import java.io.File;

public class cliTest {

    public static void main(String[] args) throws Exception {
        PzSolidPacker packer = new PzSolidPacker(new File[]{new File("E:\\Programs\\compTest\\cmpFiles")});

        long st = System.currentTimeMillis();

        packer.setAlgorithm("bwz");
        packer.setThreads(1);
        packer.setCmpLevel(1);
        packer.build();

        packer.pack("cf.pz", 524288, 0);

        long ed = System.currentTimeMillis();
        System.out.println(ed - st);
    }
}
