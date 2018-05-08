package WinLzz.Packer;

import WinLzz.BWZ.BWZDeCompressor;
import WinLzz.Interface.DeCompressor;
import WinLzz.LZZ2.LZZ2DeCompressor;
import WinLzz.ResourcesPack.Languages.LanguageLoader;
import WinLzz.Utility.Bytes;
import WinLzz.Utility.CRC32Generator;
import WinLzz.Utility.Util;
import WinLzz.ZSE.WrongPasswordException;
import WinLzz.ZSE.ZSEFileDecoder;
import WinLzz.ZSE.ZSEFileEncoder;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.ReadOnlyLongWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UnPacker {

    private String packName, mapName, cmpMapName, tempName, cmpTempName, encMapName, encMainName;

    private BufferedInputStream bis;
    private byte primaryVersion, secondaryVersion;
    private ArrayList<IndexNodeUnp> indexNodes = new ArrayList<>();
    private ContextNode rootNode;
    private int fileCount, dirCount, cmpMapLen;
    private int windowSize, encryptLevel, threadNumber;
    private long cmpMainLength, origSize;

    private String password;
    private boolean passwordSet;
    private byte[] origMD5Value;
    private String alg;

    public final ReadOnlyLongWrapper progress = new ReadOnlyLongWrapper();
    public final ReadOnlyStringWrapper percentage = new ReadOnlyStringWrapper();
    public final ReadOnlyStringWrapper ratio = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper step = new ReadOnlyStringWrapper();
    public final ReadOnlyStringWrapper timeUsed = new ReadOnlyStringWrapper();
    public final ReadOnlyStringWrapper timeExpected = new ReadOnlyStringWrapper();
    public final ReadOnlyStringWrapper passedLength = new ReadOnlyStringWrapper();

    public long startTime;
    private long creationTime, crc32Checksum;

    public boolean isInterrupted;
    private LanguageLoader languageLoader;

    public UnPacker(String packName) {
        this.packName = packName;
        mapName = packName + ".map.temp";
        cmpMapName = packName + ".map.cmp";
        tempName = packName + ".temp";
        cmpTempName = packName + ".cmp";
        encMapName = packName + ".map.enc";
        encMainName = packName + ".enc";
    }

    public void readInfo() throws Exception {
        File f = new File(packName);
        int totalCmpLength = (int) f.length();

        bis = new BufferedInputStream(new FileInputStream(packName));

        byte[] buffer2 = new byte[2];
        byte[] buffer4 = new byte[4];

        if (bis.read(buffer4) != 4) throw new IOException("Error occurs while reading");
        int headerInt = Bytes.bytesToInt32(buffer4);
        if (headerInt != Packer.HEADER) throw new NotAPzFileException("Not a PZ archive");

        if (bis.read(buffer2) != 2) throw new IOException("Error occurs while reading");
        primaryVersion = buffer2[0];
        secondaryVersion = buffer2[1];
        if (primaryVersion != Packer.primaryVersion) throw new UnsupportedVersionException("Unsupported file version");

        byte[] infoByte = new byte[1];
        if (bis.read(infoByte) != 1) throw new IOException("Error occurs while reading");

        byte[] windowSizeByte = new byte[1];
        if (bis.read(windowSizeByte) != 1) throw new IOException("Error occurs while reading");
        if ((windowSizeByte[0] & 0xff) == 0) windowSize = 0;
        else windowSize = (int) Math.pow(2, windowSizeByte[0] & 0xff);

        byte[] creationTimeByte = new byte[4];
        if (bis.read(creationTimeByte) != 4) throw new IOException("Error occurs while reading");
        creationTime = (long) Bytes.bytesToInt32(creationTimeByte) * 1000 + Packer.dateOffset;

        if (bis.read(buffer4) != 4) throw new IOException("Error occurs while reading");
        byte[] fullCRC32Bytes = new byte[8];
        System.arraycopy(buffer4, 0, fullCRC32Bytes, 4, 4);
        crc32Checksum = Bytes.bytesToLong(fullCRC32Bytes);

        if (bis.read(buffer4) != 4) throw new IOException("Error occurs while reading");
        cmpMapLen = Bytes.bytesToInt32(buffer4);

        if (bis.read(buffer2) != 2) throw new IOException("Error occurs while reading");
        int extraFieldLen = Bytes.bytesToShort(buffer2);

        byte[] extraField = new byte[extraFieldLen];
        if (bis.read(extraField) != extraFieldLen) throw new IOException("Error occurs while reading");

        cmpMainLength = totalCmpLength - cmpMapLen - extraFieldLen - 22;

        String info = Bytes.byteToBitString(infoByte[0]);
        String enc = info.substring(0, 2);
        switch (enc) {
            case "00":
                encryptLevel = 0;
                break;
            case "10":
                encryptLevel = 1;
                break;
            case "01":
                encryptLevel = 2;
                break;
        }

        String algCode = info.substring(2, 4);
        switch (algCode) {
            case "00":
                alg = "lzz2";
                break;
            case "10":
                alg = "bwz";
                break;
        }

        if (encryptLevel != 0) {
            cmpMainLength -= 16;
            origMD5Value = new byte[16];
            if (bis.read(origMD5Value) != 16) throw new IOException("Error occurs while reading");
        }
    }

    public void readMap() throws Exception {
        unCompressMap(cmpMapLen);
        readContext();
        deleteTemp();

        rootNode = new ContextNode(indexNodes.get(0).getName());
        buildContextTree(rootNode, indexNodes.get(0));
    }

    private void unCompressMap(int cmpMapLen) throws Exception {
        if (encryptLevel == 2) {
            Util.fileTruncate(bis, encMapName, 8192, cmpMapLen);
            FileOutputStream fos = new FileOutputStream(cmpMapName);
            ZSEFileDecoder zfd = new ZSEFileDecoder(encMapName, password);
            zfd.Decode(fos);
            fos.flush();
            fos.close();
        } else {
            Util.fileTruncate(bis, cmpMapName, 8192, cmpMapLen);
        }

        DeCompressor mapDec;
        if (windowSize == 0) {
            switch (alg) {
                case "lzz2":
                    mapDec = new LZZ2DeCompressor(cmpMapName, Packer.defaultWindowSize);
                    break;
                case "bwz":
                    mapDec = new BWZDeCompressor(cmpMapName, Packer.defaultWindowSize);
                    break;
                default:
                    throw new NoSuchAlgorithmException("No such algorithm");
            }
        } else {
            switch (alg) {
                case "lzz2":
                    mapDec = new LZZ2DeCompressor(cmpMapName, windowSize);
                    break;
                case "bwz":
                    mapDec = new BWZDeCompressor(cmpMapName, windowSize);
                    break;
                default:
                    throw new NoSuchAlgorithmException("No such algorithm");
            }
        }
        FileOutputStream fos = new FileOutputStream(mapName);
        try {
            mapDec.Uncompress(fos);
        } catch (Exception e) {
            fos.close();
            deleteTemp();
            mapDec.deleteCache();
            throw e;
        }
        fos.close();
    }

    private void readContext() throws IOException {
        BufferedInputStream mapInputStream = new BufferedInputStream(new FileInputStream(mapName));
        while (true) {
            byte[] flag = new byte[1];
            if (mapInputStream.read(flag) != 1) break;
            boolean isDir = (flag[0] & 0xff) == 0;
            if (mapInputStream.read(flag) != 1) throw new IOException("Error occurs while reading");
            int nameLen = flag[0] & 0xff;
            byte[] nameBytes = new byte[nameLen];
            if (mapInputStream.read(nameBytes) != nameLen) throw new IOException("Error occurs while reading");
            String name = Bytes.stringDecode(nameBytes);
            IndexNodeUnp inu = new IndexNodeUnp(name);
            if (isDir) {
                byte[] numberBytes = new byte[8];
                if (mapInputStream.read(numberBytes) != 8) throw new IOException("Error occurs while reading");
                long begin = Bytes.bytesToLong(numberBytes);
                if (mapInputStream.read(numberBytes) != 8) throw new IOException("Error occurs while reading");
                inu.setChildrenRange((int) begin, (int) Bytes.bytesToLong(numberBytes));
                dirCount += 1;
            } else {
                byte[] sizeByte = new byte[8];
                if (mapInputStream.read(sizeByte) != 8) throw new IOException("Error occurs while reading");
                long start = Bytes.bytesToLong(sizeByte);
                if (mapInputStream.read(sizeByte) != 8) throw new IOException("Error occurs while reading");
                long end = Bytes.bytesToLong(sizeByte);
                inu.setScale(start, end);
                origSize = end;
                fileCount += 1;
            }
            indexNodes.add(inu);
        }
        mapInputStream.close();
    }

    private void buildContextTree(ContextNode node, IndexNodeUnp inu) {
        if (inu.isDir()) {
            List<IndexNodeUnp> children = indexNodes.subList(inu.getChildrenRange()[0], inu.getChildrenRange()[1]);
            String path = node.getPath();
            for (IndexNodeUnp n : children) {
                ContextNode cn = new ContextNode(path + File.separator + n.getName());
                cn.setParent(node);
                node.addChild(cn);
            }
            for (int i = 0; i < children.size(); i++) buildContextTree(node.getChildren().get(i), children.get(i));
        } else {
            node.setLocation(inu.getStart(), inu.getEnd());
        }
    }

    public int getDirCount() {
        return dirCount;
    }

    public int getFileCount() {
        return fileCount;
    }

    public int getWindowSize() {
        return windowSize;
    }

    public ContextNode getRootNode() {
        return rootNode;
    }

    public byte versionNeeded() {
        return primaryVersion;
    }

    private void unCompressMain() throws Exception {
        if (origSize == 0) {
            File f = new File(tempName);
            if (!f.createNewFile()) System.out.println("Creation failed");
        } else if (!isUnCompressed()) {
            if (encryptLevel != 0) {
                Util.fileTruncate(bis, encMainName, 8192, cmpMainLength);
                FileOutputStream fos = new FileOutputStream(cmpTempName);
                ZSEFileDecoder zfd = new ZSEFileDecoder(encMainName, password);
                zfd.Decode(fos);
                fos.flush();
                fos.close();
            } else {
                Util.fileTruncate(bis, cmpTempName, 8192, cmpMainLength);
            }
            if (windowSize == 0) {
                tempName = cmpTempName;
            } else {
                DeCompressor mainDec;
                switch (alg) {
                    case "lzz2":
                        mainDec = new LZZ2DeCompressor(cmpTempName, windowSize);
                        break;
                    case "bwz":
                        mainDec = new BWZDeCompressor(cmpTempName, windowSize);
                        break;
                    default:
                        throw new NoSuchAlgorithmException("No such algorithm");
                }
                mainDec.setParent(this);
                mainDec.setThreads(threadNumber);
                FileOutputStream mainFos = new FileOutputStream(tempName);
                try {
                    mainDec.Uncompress(mainFos);
                    mainFos.close();
                } catch (Exception e) {
                    mainDec.deleteCache();
                    throw e;
                } finally {
                    Util.deleteFile(cmpTempName);
                    bis.close();
                }
            }
        }
        if (isInterrupted) return;
        long currentCRC32 = CRC32Generator.generateCRC32(tempName);
        if (currentCRC32 != crc32Checksum) throw new Exception("CRC32 Checksum does not match");
    }

    public void unCompressAll(String targetDir) throws Exception {
        for (ContextNode cn : rootNode.getChildren()) unCompressFrom(targetDir, cn);
    }

    public void unCompressFrom(String targetDir, ContextNode cn) throws Exception {
        startTime = System.currentTimeMillis();
        if (languageLoader != null) step.set(languageLoader.get(350));
        progress.set(1);
        unCompressMain();
        if (isInterrupted) return;
        RandomAccessFile raf = new RandomAccessFile(tempName, "r");
        String dirOffset;
        if (!cn.getPath().contains(File.separator)) dirOffset = "";
        else dirOffset = cn.getPath().substring(0, cn.getPath().lastIndexOf(File.separator));
        traverseUncompress(targetDir, cn, raf, dirOffset);
        raf.close();
    }

    private void traverseUncompress(String targetDir, ContextNode cn, RandomAccessFile raf, String dirOffset) throws IOException {
        String path = targetDir + File.separator + cn.getPath().substring(dirOffset.length());
        File f = new File(path);
        if (cn.isDir()) {
            if (!f.exists()) {
                if (!f.mkdirs()) System.out.println("Failed to create directory");
            }
            for (ContextNode scn : cn.getChildren()) traverseUncompress(targetDir, scn, raf, dirOffset);
        } else {
            long[] location = cn.getLocation();
            raf.seek(location[0]);
            Util.fileTruncate(raf, path, 8192, location[1] - location[0]);
        }
    }

    private void deleteTemp() {
        Util.deleteFile(encMainName);
        Util.deleteFile(mapName);
        Util.deleteFile(cmpMapName);
        Util.deleteFile(encMapName);
    }


    /**
     * Returns whether the compressed file is undamaged.
     *
     * @return true if the file can be uncompress successfully and the total length is equal to the original length.
     */
    public boolean TestPack() {
        if (languageLoader != null) step.set(languageLoader.get(350));
        try {
            unCompressMain();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * Sets up decompressing password.
     *
     * @param password password.
     * @throws Exception if password does not match the previous set password.
     */
    public void setPassword(String password) throws Exception {
        this.password = password;
        byte[] md5Value = ZSEFileEncoder.md5PlainCode(password);
        if (!Arrays.equals(md5Value, origMD5Value)) throw new WrongPasswordException();
        else passwordSet = true;
    }

    public void setThreads(int threads) {
        this.threadNumber = threads;
    }

    public String getAlg() {
        return alg;
    }

    public boolean isPasswordSet() {
        return passwordSet;
    }

    public int getEncryptLevel() {
        return encryptLevel;
    }

    @Deprecated
    public void UncompressAll(String targetDir) throws Exception {
        unCompressFrom(targetDir, rootNode);
    }

    public long getTotalOrigSize() {
        return origSize;
    }


    public void setLanguageLoader(LanguageLoader languageLoader) {
        this.languageLoader = languageLoader;
    }

    /**
     * Closes the decompressor and delete temp files.
     */
    public void close() throws IOException {
        bis.close();
        Util.deleteFile(tempName);
    }


    /**
     * Interrupts the current decompression thread.
     */
    public void interrupt() {
        this.isInterrupted = true;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public long getCrc32Checksum() {
        return crc32Checksum;
    }

    public int getPrimaryVersionInt() {
        return primaryVersion & 0xff;
    }

    public int getSecondaryVersionInt() {
        return secondaryVersion & 0xff;
    }

    private boolean isUnCompressed() {
        File f = new File(tempName);
        return f.exists() && f.length() == origSize;
    }

    public ReadOnlyLongProperty progressProperty() {
        return progress;
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
}

class IndexNodeUnp {

    private String name;
    private long start;
    private long end;
    private boolean isDir;
    private int[] childrenRange;

    IndexNodeUnp(String name) {
        this.name = name;
        isDir = true;
    }

    void setScale(long start, long end) {
        this.start = start;
        this.end = end;
        isDir = false;
    }

    void setChildrenRange(int begin, int stop) {
        childrenRange = new int[]{begin, stop};
    }

    boolean isDir() {
        return isDir;
    }

    int[] getChildrenRange() {
        return childrenRange;
    }

    public long getStart() {
        return start;
    }

    long getEnd() {
        return end;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        if (isDir) return "Dir(" + name + ", " + Arrays.toString(childrenRange) + ")";
        else return "File(" + name + ": " + start + ", " + end + ")";
    }
}


