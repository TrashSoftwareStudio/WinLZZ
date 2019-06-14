package trashsoftware.win_bwz.Console;

import trashsoftware.win_bwz.Main;
import trashsoftware.win_bwz.Packer.*;
import trashsoftware.win_bwz.Encrypters.WrongPasswordException;

import java.io.File;
import java.security.NoSuchAlgorithmException;

public class Console {

    private static final String usage = "Usage:\n" +
            "java -jar WinLZZ.jar -c inFile outFile [-a algorithm(bwz/lzz2)] [-l level(0 to 5)] [-t threads(1 to 4)]" +
            "[-e encryptLevel(0 to 2) password]\n" +
            "or\n" +
            "java -jar WinLZZ.jar -u inFile outFile [-t threads(1 to 4)] [-e password]\n";

    /**
     * WinLZZ console client.
     *
     * @param args command line arguments.
     */
    public static void console(String[] args) {
        try {
            String mode = args[0];
            if (mode.equals("-i")) {
                System.out.println("WinLZZ " + Main.version);
                return;
            }
            String inFile = args[1];
            String outFile = args[2];
            File in = new File(inFile);
            File out = new File(outFile);
            if (!out.isAbsolute()) outFile = in.getParent() + File.separator + outFile;
            String alg = "bwz";
            int level = 3;
            int threads = 1;
            int enc = 0;
            String password = "";
            int algIndex = getArgIndex(args, "-a");
            if (algIndex != -1) alg = args[algIndex];
            int levelIndex = getArgIndex(args, "-l");
            if (levelIndex != -1) level = Integer.valueOf(args[levelIndex]);
            int threadIndex = getArgIndex(args, "-t");
            if (threadIndex != -1) threads = Integer.valueOf(args[threadIndex]);
            int encLevel = getArgIndex(args, "-e");
            if (encLevel != -1) {
                if (mode.equals("-c")) {
                    enc = Integer.valueOf(args[encLevel]);
                    password = args[encLevel + 1];
                } else {
                    password = args[encLevel];
                }
            }

            switch (mode) {
                case "-c": {
                    int[] pref = getPreferences(alg, level);
                    Packer p = new Packer(new File[]{new File(inFile)});
                    p.setAlgorithm(alg);
                    p.setThreads(threads);
                    p.setCmpLevel(pref[2]);
                    p.setEncrypt(password, enc, "bzse", "sha-256");

                    long start = System.currentTimeMillis();
                    System.out.println("Compressing...");
                    if (!outFile.endsWith(".pz")) outFile += ".pz";
                    p.Pack(outFile, pref[0], pref[1]);

                    long timeUsed = System.currentTimeMillis() - start;
                    double seconds = (double) timeUsed / 1000;
                    double rounded;
                    if (p.getTotalOrigSize() == 0) {
                        rounded = 0;
                    } else {
                        double compressRate = (double) p.getCompressedLength() / p.getTotalOrigSize();
                        rounded = (double) Math.round(compressRate * 10000) / 100;
                    }
                    System.out.println("Compression done.");
                    System.out.println("Time used: " + seconds + "s, compression ratio: " + rounded + "%");
                    break;
                }
                case "-u": {
                    int sigCheck = UnPacker.checkSignature(inFile);
                    if (sigCheck == 1) {
                        System.out.println("This is an archive subsection. Please open the first section.");
                        return;
                    } else if (sigCheck == 2) {
                        System.out.println("File may not be a WinLZZ archive");
                        return;
                    }
                    UnPacker up = new UnPacker(inFile);
                    try {
                        up.readInfo();
                    } catch (UnsupportedVersionException e) {
                        System.out.println("Unsupported archive version");
                        return;
                    }
                    if (up.getEncryptLevel() != 0) {
                        try {
                            up.setPassword(password);
                        } catch (WrongPasswordException wpe) {
                            System.out.println("Wrong password");
                            return;
                        }
                    }
                    try {
                        up.readMap();
                    } catch (Exception e) {
                        System.out.println("Archive might being damaged");
                        return;
                    }
                    up.setThreads(threads);

                    long start = System.currentTimeMillis();
                    System.out.println("Uncompressing...");
                    up.unCompressFrom(outFile, up.getRootNode());

                    long timeUsed = System.currentTimeMillis() - start;
                    double seconds = (double) timeUsed / 1000;
                    System.out.println("Decompression finished in " + seconds + "s");
                    up.close();
                    break;
                }
                default:
                    System.out.println(usage);
                    break;
            }
        } catch (Exception e) {
            System.out.println(usage);
        }
    }

    private static int getArgIndex(String[] args, String flag) {
        for (int i = 3; i < args.length; i++) if (args[i].equals(flag)) return i + 1;
        return -1;
    }

    private static int[] getPreferences(String alg, int level) throws NoSuchAlgorithmException {
        int[] lzz2Windows = new int[]{4096, 16384, 32768, 65536, 262144};
        int[] lzz2Buffers = new int[]{16, 32, 64, 64, 128};
        int[] bwzWindows = new int[]{131072, 262144, 524288, 1048576, 4194304};
        int[] levels = new int[]{0, 0, 1, 1, 1};
        switch (alg) {
            case "bwz":
                return new int[]{bwzWindows[level - 1], 0, levels[level - 1]};
            case "lzz2":
                return new int[]{lzz2Windows[level - 1], lzz2Buffers[level - 1], levels[level - 1]};
            default:
                throw new NoSuchAlgorithmException("NoSuckAlgorithm");
        }
    }
}
