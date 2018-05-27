package WinLzz.Packer;

import WinLzz.BWZ.BWZDeCompressor;
import WinLzz.Interface.DeCompressor;
import WinLzz.LZZ2.LZZ2DeCompressor;
import WinLzz.ResourcesPack.Languages.LanguageLoader;
import WinLzz.Utility.Bytes;
import WinLzz.Utility.Util;
import WinLzz.ZSE.WrongPasswordException;
import WinLzz.ZSE.ZSEDecoder;
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

/**
 * The .pz archive unpacking program.
 *
 * @author zbh
 * @since 0.4
 */
public class UnPacker {

    /**
     * Names of temporary files that will be created for unpacking and decompressing.
     */
    private String packName, mapName, cmpMapName, tempName, cmpTempName, encMapName, encMainName;

    /**
     * The input stream of this archive file itself.
     */
    private BufferedInputStream bis;

    /**
     * Primary version of the current opening archive.
     * <p>
     * The archive is available if and only if the primary version of this archive matches the primary version of the
     * decompression program.
     */
    private byte primaryVersion;

    /**
     * Secondary version of the current opening archive.
     */
    private byte secondaryVersion;

    /**
     * List of {@code IndexNodeUnp}'s.
     * <p>
     * This list is order-sensitive. Each node represents an actual file except the root node.
     */
    private ArrayList<IndexNodeUnp> indexNodes = new ArrayList<>();

    /**
     * The root {@code ContextNode} of the archive.
     * <p>
     * This node does not represent an actual directory.
     */
    private ContextNode rootNode;

    private int fileCount, dirCount;

    /**
     * The length of context map after compression.
     */
    private int cmpMapLen;

    /**
     * The user-defined size of sliding window/block of block-based or sliding-window-based compression algorithm.
     * <p>
     * This value is read from archive.
     * This value will be 0 if there is no compression.
     */
    private int windowSize;

    private int encryptLevel, threadNumber;

    /**
     * Length of the main part.
     */
    private long cmpMainLength;

    /**
     * Length of this archive file.
     */
    private long archiveLength;

    /**
     * Length of the original file (uncompressed).
     */
    private long origSize;

    private String password;
    private boolean passwordSet;

    /**
     * 16-byte array representing the md5 checksum of original password, if exists.
     */
    private byte[] origMD5Value;

    private byte[] extraField;

    /**
     * String abbreviation of compression algorithm of this archive.
     */
    private String alg;

    /**
     * String annotation of this archive.
     */
    private String annotation;

    public final ReadOnlyLongWrapper progress = new ReadOnlyLongWrapper();
    public final ReadOnlyStringWrapper percentage = new ReadOnlyStringWrapper();
    public final ReadOnlyStringWrapper ratio = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper step = new ReadOnlyStringWrapper();
    public final ReadOnlyStringWrapper timeUsed = new ReadOnlyStringWrapper();
    public final ReadOnlyStringWrapper timeExpected = new ReadOnlyStringWrapper();
    public final ReadOnlyStringWrapper passedLength = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper currentFile = new ReadOnlyStringWrapper();

    public long startTime;
    private long creationTime, crc32Checksum;

    public boolean isInterrupted;
    private LanguageLoader languageLoader;

    /**
     * Creates a new UnPacker instance, with <code>packName</code> as the input archive name.
     *
     * @param packName the name/path of the input archive file.
     */
    public UnPacker(String packName) {
        this.packName = packName;
        mapName = packName + ".map.temp";
        cmpMapName = packName + ".map.cmp";
        tempName = packName + ".temp";
        cmpTempName = packName + ".cmp";
        encMapName = packName + ".map.enc";
        encMainName = packName + ".enc";
    }

    /**
     * Reads the archive information from the archive file to this {@code UnPacker} instance.
     *
     * @throws IOException         if the archive file is not readable,
     *                             or any error occurs during reading.
     * @throws NotAPzFileException if the archive file is not a pz archive,
     *                             or the primary version of this archive file is older than 20.
     */
    public void readInfo() throws IOException, NotAPzFileException {
        File f = new File(packName);
        archiveLength = f.length();

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
        byte[] extraFieldLength = new byte[4];
        System.arraycopy(buffer2, 0, extraFieldLength, 2, 2);
        int extraFieldLen = Bytes.bytesToInt32(extraFieldLength);

        extraField = new byte[extraFieldLen];
        if (bis.read(extraField) != extraFieldLen) throw new IOException("Error occurs while reading");

        cmpMainLength = archiveLength - cmpMapLen - extraFieldLen - 22;

        String info = Bytes.byteToBitString(infoByte[0]);
        String enc = info.substring(0, 2);  // The encrypt level of this archive.
        switch (enc) {
            case "00":
                encryptLevel = 0;
                passwordSet = true;
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

    /**
     * Reads the context map and extra field from archive file to this {@code UnPacker} instance.
     * <p>
     * The context map records the path structure of the file stored in archive.
     * The context map is compressed by some algorithm.
     *
     * @throws Exception if any error occurs during reading.
     */
    public void readMap() throws Exception {
        unCompressMap(cmpMapLen);
        readContext();
        deleteTemp();

        rootNode = new ContextNode(indexNodes.get(0).getName());
        buildContextTree(rootNode, indexNodes.get(0));

        interpretExtraField();
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
                    mapDec = new BWZDeCompressor(cmpMapName, Packer.defaultWindowSize, 0);
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
                    mapDec = new BWZDeCompressor(cmpMapName, windowSize, 0);
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

    private void interpretExtraField() {
        int i = 0;
        while (i < extraField.length) {
            byte flag = extraField[i];
            byte info = extraField[i + 1];
            byte[] lengthBytes = new byte[]{extraField[i + 2], extraField[i + 3]};
            short length = Bytes.bytesToShort(lengthBytes);
            i += 4;
            byte[] field = new byte[length];
            System.arraycopy(extraField, i, field, 0, length);
            i += length;

            switch (flag) {
                case 1:
                    boolean compressed = info == 1;
                    readAnnotation(field, compressed);
                    break;
                default:
                    break;
            }
        }
    }

    private void readAnnotation(byte[] byteText, boolean compressed) {
        try {
            byte[] decodeText;
            if (encryptLevel == 2) decodeText = new ZSEDecoder(byteText, password).Decode();
            else decodeText = byteText;

            byte[] uncompressedText;
            if (compressed) {
                String temp = "ann.temp";
                FileOutputStream fos = new FileOutputStream(temp);
                fos.write(decodeText);
                fos.flush();
                fos.close();

                try {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    BWZDeCompressor deCompressor = new BWZDeCompressor(temp, 32768, 0);
                    deCompressor.Uncompress(out);
                    uncompressedText = out.toByteArray();
                } catch (Exception e) {
                    Util.deleteFile(temp);
                    throw e;
                }
                Util.deleteFile(temp);
            } else {
                uncompressedText = decodeText;
            }
            annotation = new String(uncompressedText, "utf-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the total directory count stored in this archive.
     * <p>
     * The return value is always 1 more than the actual directory count in the original file/directory, since there is
     * a virtual {@code rootNode}.
     *
     * @return the total directory count stored in this archive.
     */
    public int getDirCount() {
        return dirCount;
    }

    /**
     * Returns the total file count stored in this archive.
     *
     * @return the total file count stored in this archive.
     */
    public int getFileCount() {
        return fileCount;
    }

    /**
     * Returns the window size.
     *
     * @return {@code windowSize} the window size.
     */
    public int getWindowSize() {
        return windowSize;
    }

    /**
     * Returns the root node.
     *
     * @return {@code rootNode} the root node.
     */
    public ContextNode getRootNode() {
        return rootNode;
    }

    /**
     * Returns the primary version needed for decompression program to uncompress this archive.
     *
     * @return {@code primaryVersion} the primary version.
     */
    public byte versionNeeded() {
        return primaryVersion;
    }

    private void unCompressMainPureBWZ() throws Exception {
        BWZDeCompressor mainDec = new BWZDeCompressor(packName, windowSize, archiveLength - cmpMainLength);
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
            bis.close();
        }

        if (isInterrupted) return;
        long currentCRC32 = Util.generateCRC32(tempName);
        if (currentCRC32 != crc32Checksum) throw new Exception("CRC32 Checksum does not match");

    }

    private void unCompressMain() throws Exception {
        if (origSize == 0) {
            File f = new File(tempName);
            if (!f.createNewFile()) System.out.println("Creation failed");
        } else if (!isUnCompressed()) {
            if (encryptLevel != 0) {
                FileOutputStream fos = new FileOutputStream(cmpTempName);
                ZSEFileDecoder zfd = new ZSEFileDecoder(bis, password, cmpMainLength);
                zfd.Decode(fos);
                fos.flush();
                fos.close();
            } else if (windowSize != 0 && alg.equals("bwz")) {
                unCompressMainPureBWZ();
                return;
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
                        mainDec = new BWZDeCompressor(cmpTempName, windowSize, 0);
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
        long currentCRC32 = Util.generateCRC32(tempName);
        if (currentCRC32 != crc32Checksum) throw new Exception("CRC32 Checksum does not match");
    }


    /**
     * Extracts and uncompress all files and directories to the path <code>targetDir</code>, recursively.
     * <p>
     * Original path structure will be kept.
     *
     * @param targetDir path to extract contents.
     * @throws Exception if any failure happens during extraction and decompression.
     */
    public void unCompressAll(String targetDir) throws Exception {
        for (ContextNode cn : rootNode.getChildren()) unCompressFrom(targetDir, cn);
    }


    /**
     * Extracts and uncompress all files and directories which are under <code>cn</code> to the path
     * <code>targetDir</code>, recursively.
     * <p>
     * * Original path structure will be kept.
     *
     * @param targetDir path to extract contents.
     * @param cn        the ContextNode to start extraction.
     * @throws Exception if any failure happens during extraction and decompression.
     */
    public void unCompressFrom(String targetDir, ContextNode cn) throws Exception {
        startTime = System.currentTimeMillis();
        if (languageLoader != null) step.set(languageLoader.get(504));
        progress.set(1);
        unCompressMain();
        if (isInterrupted) return;
        RandomAccessFile raf = new RandomAccessFile(tempName, "r");
        String dirOffset;
        if (!cn.getPath().contains(File.separator)) dirOffset = "";
        else dirOffset = cn.getPath().substring(0, cn.getPath().lastIndexOf(File.separator));
        step.setValue(languageLoader.get(507));
        traversalExtract(targetDir, cn, raf, dirOffset);
        raf.close();
    }

    private void traversalExtract(String targetDir, ContextNode cn, RandomAccessFile raf, String dirOffset) throws IOException {
        String path = targetDir + File.separator + cn.getPath().substring(dirOffset.length());
        File f = new File(path);
        if (cn.isDir()) {
            if (!f.exists()) {
                if (!f.mkdirs()) System.out.println("Failed to create directory");
            }
            for (ContextNode scn : cn.getChildren()) traversalExtract(targetDir, scn, raf, dirOffset);
        } else {
            currentFile.setValue(cn.getPath().substring(1));
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
        if (languageLoader != null) step.set(languageLoader.get(506));
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

    /**
     * Sets the thread number used for decompression.
     *
     * @param threads thread number for decompression.
     */
    public void setThreads(int threads) {
        this.threadNumber = threads;
    }

    /**
     * Returns the abbreviation of compression algorithm of this archive.
     *
     * @return the abbreviation of compression algorithm of this archive.
     */
    public String getAlg() {
        return alg;
    }

    /**
     * Returns {@code true} if the archive is encrypted and the password is properly set.
     *
     * @return {@code true} if the password is properly set.
     */
    public boolean isPasswordSet() {
        return passwordSet;
    }

    /**
     * Returns the encryption level of this archive.
     * <p>
     * 0 for no encryption, 1 for there is encryption for content, 2 for encryption for both content and context map.
     *
     * @return the encryption level of this archive.
     */
    public int getEncryptLevel() {
        return encryptLevel;
    }

    /**
     * Returns the annotation text of this archive.
     *
     * @return the annotation text of this archive.
     */
    public String getAnnotation() {
        return annotation;
    }

    /**
     * Returns the total length of original files.
     *
     * @return {@code origSize} the total length of original files.
     */
    public long getTotalOrigSize() {
        return origSize;
    }

    /**
     * Sets the {@code languageLoader} language loader.
     * <p>
     * language loader is used for displaying text in different languages on the GUI.
     *
     * @param languageLoader the language loader.
     */
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

    /**
     * Returns the creation time of this archive, in mills.
     *
     * @return creation time of this archive, in mills.
     */
    public long getCreationTime() {
        return creationTime;
    }

    /**
     * Returns the crc32 checksum of original files.
     *
     * @return the crc32 checksum of original files.
     */
    public long getCrc32Checksum() {
        return crc32Checksum;
    }

    /**
     * Returns the primary version of this archive in unsigned integer form.
     *
     * @return the unsigned integer representing primary version.
     */
    public int getPrimaryVersionInt() {
        return primaryVersion & 0xff;
    }

    /**
     * Returns the secondary version of this archive in unsigned integer form.
     *
     * @return the unsigned integer representing secondary version.
     */
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

    public ReadOnlyStringProperty stepProperty() {
        return step;
    }

    public ReadOnlyStringProperty fileProperty() {
        return currentFile;
    }
}


/**
 * A class used for recording information of a file in an archive in the time of extraction.
 *
 * @author zbh
 * @since 0.4
 */
class IndexNodeUnp {

    private String name;

    /**
     * The start position of this file in the uncompressed main part of archive.
     */
    private long start;

    /**
     * The end position of this file in the uncompressed main part of archive.
     */
    private long end;

    /**
     * Whether this IndexNodeUnp represents a directory.
     */
    private boolean isDir;

    /**
     * The start and end position of children of this IndexNodeUnp in the uncompressed context map.
     */
    private int[] childrenRange;

    /**
     * Creates a new {@code IndexNodeUnp} instance.
     *
     * @param name name of file which is represented by this IndexNodeUnp.
     */
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


