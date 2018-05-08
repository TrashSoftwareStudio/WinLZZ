/*
 * PZ archive packer.
 *
 * Archive header info:
 * 4 bytes: PZ header
 * 2 bytes: version
 * 1 byte: info byte
 * 1 byte: window size
 * 4 bytes: time of creation
 * 4 bytes: CRC32 checksum
 * 4 bytes: followed context length (n)
 * 2 bytes: extra field length (m)
 * m bytes: extra field
 * n bytes: context
 */

package WinLzz.Packer;

import WinLzz.BWZ.BWZCompressor;
import WinLzz.Interface.Compressor;
import WinLzz.LZZ2.LZZ2Compressor;
import WinLzz.LZZ2.Util.LZZ2Util;
import WinLzz.ResourcesPack.Languages.LanguageLoader;
import WinLzz.Utility.Bytes;
import WinLzz.Utility.CRC32Generator;
import WinLzz.Utility.Util;
import WinLzz.ZSE.ZSEFileEncoder;
import javafx.beans.property.*;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

public class Packer {

    /**
     * The primary core version.
     */
    public final static byte primaryVersion = 23;

    /**
     * The secondary core version.
     */
    public final static byte secondaryVersion = 0;

    /**
     * The signature for a WinLZZ archive (*.pz) file.
     */
    final static int HEADER = 0x03F92FBD;

    /**
     * Set 1999-12-31 19:00 as the initial time.
     */
    final static long dateOffset = 946684800000L;

    static final int defaultWindowSize = 32768;

    private int fileCount, cmpLevel, encryptLevel, threads;
    private long totalLength, compressedLength;
    private ArrayList<IndexNode> indexNodes = new ArrayList<>();
    private String password;
    private String alg;

    public final ReadOnlyLongWrapper progress = new ReadOnlyLongWrapper();
    public final ReadOnlyStringWrapper percentage = new ReadOnlyStringWrapper();
    public final ReadOnlyStringWrapper ratio = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper step = new ReadOnlyStringWrapper();
    public final ReadOnlyStringWrapper timeUsed = new ReadOnlyStringWrapper();
    public final ReadOnlyStringWrapper timeExpected = new ReadOnlyStringWrapper();
    public final ReadOnlyStringWrapper passedLength = new ReadOnlyStringWrapper();
    public final ReadOnlyStringWrapper cmpLength = new ReadOnlyStringWrapper();
    public final ReadOnlyStringWrapper currentCmpRatio = new ReadOnlyStringWrapper();

    public long startTime = System.currentTimeMillis();
    public boolean isInterrupted;
    private LanguageLoader lanLoader;

    public Packer(File[] inFiles) {
        RootFile rf = new RootFile(inFiles);
        IndexNode rootNode = new IndexNode(rf);
        fileCount = 1;
        indexNodes.add(rootNode);
        buildIndexTree(rf, rootNode);
    }

    private void buildIndexTree(File file, IndexNode currentNode) {
        if (file.isDirectory()) {
            File[] sub = file.listFiles();

            int currentCount = fileCount;
            assert sub != null;
            ArrayList<IndexNode> tempList = new ArrayList<>();
            for (File f : sub) {
                IndexNode in = new IndexNode(f.getName(), f);
                tempList.add(in);
                fileCount += 1;
            }
            currentNode.setChildrenRange(currentCount, fileCount);
            indexNodes.addAll(tempList);
            for (int i = 0; i < sub.length; i++) if (!sub[i].isDirectory()) buildIndexTree(sub[i], tempList.get(i));
            for (int i = 0; i < sub.length; i++) if (sub[i].isDirectory()) buildIndexTree(sub[i], tempList.get(i));
        } else {
            long start = totalLength;
            totalLength += file.length();
            currentNode.setSize(start, totalLength);
        }
    }

    private void writeMapToStream(OutputStream headBos, OutputStream mainBos) throws IOException {
        for (IndexNode in : indexNodes) {
            headBos.write(in.toByteArray());
            if (!in.isDir()) {
                FileInputStream bis = new FileInputStream(in.getFile());
                byte[] mid = new byte[8192];
                int read;
                while ((read = bis.read(mid)) != -1) mainBos.write(mid, 0, read);
                bis.close();
            }
        }
    }

    public long getTotalOrigSize() {
        return totalLength;
    }


    /**
     * Pack the directory into a *.pz file.
     *
     * @param outFile    the output package file.
     * @param windowSize size of sliding window.
     * @param bufferSize size of look ahead buffer (if algorithm supported).
     * @throws Exception if any IO error occurs.
     */
    public void Pack(String outFile, int windowSize, int bufferSize) throws Exception {
        if (lanLoader != null) step.setValue(lanLoader.get(270));
        percentage.set("0.0");
        progress.set(1);
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outFile));
        bos.write(Bytes.intToBytes32(HEADER));  // Write header : 4 bytes
        bos.write(primaryVersion);  // Write version : 2 bytes
        bos.write(secondaryVersion);

        /*
         * Info:
         * [0:2] encrypt level
         * [2:4] algorithm
         */
        byte inf = (byte) 0b00000000;

        switch (encryptLevel) {
            case 0:
                break;
            case 1:
                inf = (byte) (inf | 0b10000000);
                break;
            case 2:
                inf = (byte) (inf | 0b01000000);
                break;
        }

        switch (alg) {
            case "lzz2":
                break;
            case "bwz":
                inf = (byte) (inf | 0b00100000);
                break;
        }
        compressedLength = 22;

        bos.write(inf);  // Write info: 1 byte
        bos.write(LZZ2Util.windowSizeToByte(windowSize));  // Write window size: 1 byte
        bos.write(new byte[4]);  // Reserved for creation time.
        bos.write(new byte[4]);  // Reserved for crc32 checksum
        bos.write(new byte[4]);  // Reserved for header size.

        byte[] extraField = new byte[0];  // Extra field
        compressedLength += extraField.length;
        bos.write(Bytes.shortToBytes((short) extraField.length));  // Extra field length
        bos.write(extraField);

        String tempHeadName = outFile + ".head";
        String tempMainName = outFile + ".main";
        BufferedOutputStream headBos = new BufferedOutputStream(new FileOutputStream(tempHeadName));
        FileOutputStream mainBos = new FileOutputStream(tempMainName);

        writeMapToStream(headBos, mainBos);

        headBos.flush();
        headBos.close();
        mainBos.flush();
        mainBos.close();

        long crc32 = CRC32Generator.generateCRC32(tempMainName);
        byte[] fullBytes = Bytes.longToBytes(crc32);
        byte[] crc32Checksum = new byte[4];
        System.arraycopy(fullBytes, 4, crc32Checksum, 0, 4);

        if (encryptLevel != 0) {
            bos.write(ZSEFileEncoder.md5PlainCode(password));
            compressedLength += 16;
        }

        Compressor headCompressor;
        if (windowSize == 0) {
            switch (alg) {
                case "lzz2":
                    headCompressor = new LZZ2Compressor(tempHeadName, defaultWindowSize, 64);
                    break;
                case "bwz":
                    headCompressor = new BWZCompressor(tempHeadName, defaultWindowSize);
                    break;
                default:
                    throw new NoSuchAlgorithmException("No such algorithm");
            }
        } else {
            switch (alg) {
                case "lzz2":
                    headCompressor = new LZZ2Compressor(tempHeadName, windowSize, bufferSize);
                    headCompressor.setCompressionLevel(cmpLevel);
                    break;
                case "bwz":
                    headCompressor = new BWZCompressor(tempHeadName, windowSize);
                    break;
                default:
                    throw new NoSuchAlgorithmException("No such algorithm");
            }
        }
        if (encryptLevel != 2) {
            headCompressor.Compress(bos);
        } else {
            String encHeadName = outFile + ".head.enc";
            FileOutputStream encFos = new FileOutputStream(encHeadName);
            headCompressor.Compress(encFos);
            encFos.flush();
            encFos.close();
            ZSEFileEncoder zfe = new ZSEFileEncoder(encHeadName, password);
            zfe.Encode(bos);
            Util.deleteFile(encHeadName);
        }
        compressedLength += headCompressor.getCompressedSize();

        Util.deleteFile(tempHeadName);

        if (lanLoader != null) step.setValue(lanLoader.get(206));

        String encMainName = outFile + ".enc";

        if (windowSize == 0) {
            if (encryptLevel == 0) {
                compressedLength += Util.fileConcatenate(bos, new String[]{tempMainName}, 8192);
            } else {
                ZSEFileEncoder zfe = new ZSEFileEncoder(tempMainName, password);
                zfe.Encode(bos);
                compressedLength += zfe.getEncodeLength();
            }
        } else if (totalLength != 0) {
            Compressor mainCompressor;
            switch (alg) {
                case "lzz2":
                    mainCompressor = new LZZ2Compressor(tempMainName, windowSize, bufferSize);
                    break;
                case "bwz":
                    mainCompressor = new BWZCompressor(tempMainName, windowSize);
                    break;
                default:
                    throw new NoSuchAlgorithmException("No such algorithm");
            }
            mainCompressor.setParent(this);
            mainCompressor.setCompressionLevel(cmpLevel);
            mainCompressor.setThreads(threads);
            if (encryptLevel == 0) {
                mainCompressor.Compress(bos);
            } else {
                FileOutputStream encFos = new FileOutputStream(encMainName);
                mainCompressor.Compress(encFos);
                encFos.flush();
                encFos.close();
                ZSEFileEncoder zfe = new ZSEFileEncoder(encMainName, password);
                zfe.Encode(bos);
            }
            compressedLength += mainCompressor.getCompressedSize();
        }
        Util.deleteFile(encMainName);
        if (isInterrupted) {
            bos.flush();
            bos.close();
            Util.deleteFile(outFile);
            Util.deleteFile(tempMainName);
            return;
        }

        bos.flush();
        bos.close();

        RandomAccessFile raf = new RandomAccessFile(outFile, "rw");
        raf.seek(8);
        int currentTimeInt = (int) ((System.currentTimeMillis() - dateOffset) / 1000);  // Creation time,
        // rounded to second. Starting from 1999-12-31 19:00
        raf.writeInt(currentTimeInt);
        raf.write(crc32Checksum);
        raf.writeInt((int) headCompressor.getCompressedSize());
        raf.close();

        Util.deleteFile(tempMainName);
    }

    public void setAlgorithm(String alg) {
        this.alg = alg;
    }

    public long getCompressedLength() {
        return compressedLength;
    }

    public void setCmpLevel(int cmpLevel) {
        this.cmpLevel = cmpLevel;
    }

    public void setEncrypt(String password, int level) {
        this.password = password;
        this.encryptLevel = level;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public void interrupt() {
        this.isInterrupted = true;
    }

    public void setLanLoader(LanguageLoader lanLoader) {
        this.lanLoader = lanLoader;
    }

    public ReadOnlyLongProperty progressProperty() {
        return progress;
    }

    public ReadOnlyStringProperty stepProperty() {
        return step;
    }

    public ReadOnlyStringProperty percentageProperty() {
        return percentage;
    }

    public ReadOnlyStringProperty ratioProperty() {
        return ratio;
    }

    public ReadOnlyStringProperty timeUsedProperty() {
        return timeUsed;
    }

    public ReadOnlyStringProperty timeExpectedProperty() {
        return timeExpected;
    }

    public ReadOnlyStringProperty passedLengthProperty() {
        return passedLength;
    }

    public ReadOnlyStringProperty compressedSizeProperty() {
        return cmpLength;
    }

    public ReadOnlyStringProperty currentCmpRatioProperty() {
        return currentCmpRatio;
    }
}


class IndexNode {

    private String name;
    private long start;
    private long end;
    private boolean isDir;
    private File file;
    private int[] childrenRange;

    /**
     * Constructor of a PackNode Object for a directory.
     *
     * @param name directory path.
     */
    IndexNode(String name, File file) {
        this.file = file;
        this.name = name;
        isDir = true;
    }

    IndexNode(RootFile rf) {
        this.file = rf;
        this.name = "";
        isDir = true;
    }

    public File getFile() {
        return file;
    }

    public long getSize() {
        return end - start;
    }

    void setSize(long start, long end) {
        this.start = start;
        this.end = end;
        isDir = false;
    }

    void setChildrenRange(int begin, int stop) {
        childrenRange = new int[]{begin, stop};
    }

    byte[] toByteArray() throws UnsupportedEncodingException {
        byte[] nameBytes = Bytes.stringEncode(name);
        int len = nameBytes.length;
        byte[] result = new byte[len + 18];
        if (isDir) {
            result[0] = 0;
            result[1] = (byte) len;
            System.arraycopy(nameBytes, 0, result, 2, nameBytes.length);
            System.arraycopy(Bytes.longToBytes(childrenRange[0]), 0, result, len + 2, 8);
            System.arraycopy(Bytes.longToBytes(childrenRange[1]), 0, result, len + 10, 8);
        } else {
            result[0] = 1;
            result[1] = (byte) len;
            System.arraycopy(nameBytes, 0, result, 2, nameBytes.length);
            System.arraycopy(Bytes.longToBytes(start), 0, result, len + 2, 8);
            System.arraycopy(Bytes.longToBytes(end), 0, result, len + 10, 8);
        }
        return result;
    }

    boolean isDir() {
        return isDir;
    }

    @Override
    public String toString() {
        if (isDir) return "Dir(" + name + ", " + Arrays.toString(childrenRange) + ")";
        else return "File(" + name + ": " + start + ", " + end + ")";
    }
}


class RootFile extends File {

    private File[] children;

    RootFile(File[] children) {
        super("");
        this.children = children;
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    @Override
    public File[] listFiles() {
        return children;
    }
}
