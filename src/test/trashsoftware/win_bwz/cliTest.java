package trashsoftware.win_bwz;

import trashsoftware.win_bwz.packer.Packer;

import java.io.File;

public class cliTest {

    public static void main(String[] args) throws Exception {
        Packer packer = new Packer(new File[]{new File("E:\\Programs\\compTest\\cmpFiles")});

        long st = System.currentTimeMillis();

        packer.setAlgorithm("bwz");
        packer.setThreads(1);
        packer.setCmpLevel(1);
        packer.build();

        packer.Pack("cf.pz", 524288, 0);

        long ed = System.currentTimeMillis();
        System.out.println(ed - st);
    }
}
