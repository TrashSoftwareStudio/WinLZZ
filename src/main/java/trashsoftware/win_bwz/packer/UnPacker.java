package trashsoftware.win_bwz.packer;

import trashsoftware.win_bwz.core.bwz.BWZCompressor;
import trashsoftware.win_bwz.core.bwz.BWZDeCompressor;
import trashsoftware.win_bwz.core.fastLzz.FastLzzCompressor;
import trashsoftware.win_bwz.core.lzz2.LZZ2Compressor;
import trashsoftware.win_bwz.encrypters.bzse.BZSEStreamDecoder;
import trashsoftware.win_bwz.core.DeCompressor;
import trashsoftware.win_bwz.encrypters.Decipher;
import trashsoftware.win_bwz.core.lzz2.LZZ2DeCompressor;
import trashsoftware.win_bwz.core.fastLzz.FastLzzDecompressor;
import trashsoftware.win_bwz.utility.*;
import trashsoftware.win_bwz.encrypters.WrongPasswordException;
import trashsoftware.win_bwz.encrypters.zse.ZSEFileDecoder;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.ReadOnlyLongWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.zip.CRC32;

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
    private String packName, mapName, cmpMapName, tempName, cmpTempName, encMapName, encMainName, combineName;

    /**
     * The input stream of this archive file itself.
     */
    private InputStream bis;

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
    private byte algVersion;

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

    private boolean isSeparated;

    private int partCount;

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

    private String encryption = "bzse";

    private String passwordAlg = "sha-256";

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

    private boolean isTest;

    /**
     * 16-byte array representing the checksum of original password, if exists.
     */
    private byte[] origPasswordChecksum;

    private byte[] passwordSalt;

    private byte[] extraField;

    /**
     * String abbreviation of compression algorithm of this archive.
     */
    private String alg;

    /**
     * String annotation of this archive.
     */
    private String annotation;

    private String failInfo;

    public final ReadOnlyLongWrapper progress = new ReadOnlyLongWrapper();
    public final ReadOnlyStringWrapper percentage = new ReadOnlyStringWrapper();
    public final ReadOnlyStringWrapper ratio = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper step = new ReadOnlyStringWrapper();
    public final ReadOnlyStringWrapper timeUsed = new ReadOnlyStringWrapper();
    public final ReadOnlyStringWrapper timeExpected = new ReadOnlyStringWrapper();
    public final ReadOnlyStringWrapper passedLength = new ReadOnlyStringWrapper();
    public final ReadOnlyStringWrapper currentFile = new ReadOnlyStringWrapper();

    public long startTime;
    private long creationTime, crc32Checksum, crc32Context;

    public boolean isInterrupted;
    private ResourceBundle bundle;

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
     * @throws IOException if the archive file is not readable,
     *                     or any error occurs during reading.
     *                     //     * @throws NotAPzFileException if the archive file is not a pz archive,
     *                     //     *                             or the primary version of this archive file is older than 20.
     */
    public void readInfo() throws IOException {

        bis = new BufferedInputStream(new FileInputStream(packName));

        byte[] buffer2 = new byte[2];
        byte[] buffer4 = new byte[4];

        if (bis.skip(4) != 4) throw new IOException("Error occurs while reading");
//        int headerInt = Bytes.bytesToInt32(buffer4);
//        if (headerInt != Packer.SIGNATURE) throw new NotAPzFileException("Not a PZ archive");

        if (bis.read(buffer2) != 2) throw new IOException("Error occurs while reading");
        primaryVersion = buffer2[0];
        algVersion = buffer2[1];
        if (primaryVersion != Packer.primaryVersion)
            throw new UnsupportedVersionException("Unsupported file version");

        byte[] infoBytes = new byte[2];
        if (bis.read(infoBytes) != 2) throw new IOException("Error occurs while reading");

        String info = Bytes.byteToBitString(infoBytes[0]);
        String encInfo = Bytes.byteToBitString(infoBytes[1]);
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
        int programAlgVersion;
        switch (algCode) {
            case "00":
                alg = "lzz2";
                programAlgVersion = LZZ2Compressor.VERSION;
                break;
            case "10":
                alg = "bwz";
                programAlgVersion = BWZCompressor.VERSION;
                break;
            case "11":
                alg = "fastLzz";
                programAlgVersion = FastLzzCompressor.VERSION;
                break;
            default:
                throw new RuntimeException("Unknown algorithm");
        }
        if (programAlgVersion != algVersion)
            throw new UnsupportedVersionException("Unsupported algorithm version");

        char sepRep = info.charAt(4);
        isSeparated = sepRep == '1';

        String encAlgName = encInfo.substring(0, 2);
        switch (encAlgName) {
            case "00":
                encryption = "zse";
                break;
            case "10":
                encryption = "bzse";
                break;
        }

        String passAlgName = encInfo.substring(2, 5);
        int passwordPlainLength = 0;
        switch (passAlgName) {
            case "000":
                passwordAlg = "md5";
                passwordPlainLength = 16;
                break;
            case "001":
                passwordAlg = "sha-256";
                passwordPlainLength = 32;
                break;
            case "010":
                passwordAlg = "sha-384";
                passwordPlainLength = 48;
                break;
            case "011":
                passwordAlg = "sha-512";
                passwordPlainLength = 64;
                break;
            case "100":
                passwordAlg = "zha64";
                passwordPlainLength = 8;
                break;
        }

        byte[] windowSizeByte = new byte[1];
        if (bis.read(windowSizeByte) != 1) throw new IOException("Error occurs while reading");
        if ((windowSizeByte[0] & 0xff) == 0) windowSize = 0;
        else windowSize = (int) Math.pow(2, windowSizeByte[0] & 0xff);

        byte[] creationTimeByte = new byte[4];
        if (bis.read(creationTimeByte) != 4) throw new IOException("Error occurs while reading");
        creationTime = (long) Bytes.bytesToInt32(creationTimeByte) * 1000 + Packer.dateOffset;

        if (bis.read(buffer4) != 4) throw new IOException("Error occurs while reading");
        byte[] fullCRC32Bytes1 = new byte[8];
        System.arraycopy(buffer4, 0, fullCRC32Bytes1, 4, 4);
        crc32Context = Bytes.bytesToLong(fullCRC32Bytes1);

        if (bis.read(buffer4) != 4) throw new IOException("Error occurs while reading");
        byte[] fullCRC32Bytes2 = new byte[8];
        System.arraycopy(buffer4, 0, fullCRC32Bytes2, 4, 4);
        crc32Checksum = Bytes.bytesToLong(fullCRC32Bytes2);

        if (bis.read(buffer4) != 4) throw new IOException("Error occurs while reading");
        cmpMapLen = Bytes.bytesToInt32(buffer4);

        if (bis.read(buffer2) != 2) throw new IOException("Error occurs while reading");
        byte[] extraFieldLength = new byte[4];
        System.arraycopy(buffer2, 0, extraFieldLength, 2, 2);
        int extraFieldLen = Bytes.bytesToInt32(extraFieldLength);

        int extraLen;
        if (isSeparated) {
            if (bis.read(buffer4) != 4) throw new IOException("Error occurs while reading");
            partCount = Bytes.bytesToInt32(buffer4);
            byte[] buffer8 = new byte[8];
            if (bis.read(buffer8) != 8) throw new IOException("Error occurs while reading");
            archiveLength = Bytes.bytesToLong(buffer8);
            extraFieldLen -= 12;
            extraField = new byte[extraFieldLen];
            String prefixName1 = packName.substring(0, packName.lastIndexOf("."));
            String prefixName = prefixName1.substring(0, prefixName1.lastIndexOf("."));
            String suffixName = packName.substring(packName.lastIndexOf("."));
            bis.close();
            bis = SeparateInputStream.createNew(prefixName, suffixName, partCount, this, Packer.PART_SIGNATURE);
            ((SeparateInputStream) bis).setLanLoader(bundle);
            if (bis.skip(39) != 39) throw new IOException("Error occurs while reading");
            extraLen = 12;
        } else {
            File f = new File(packName);
            archiveLength = f.length();
            extraField = new byte[extraFieldLen];
            extraLen = 0;
        }

        if (bis.read(extraField) != extraFieldLen) throw new IOException("Error occurs while reading");

        cmpMainLength = archiveLength - cmpMapLen - extraFieldLen - extraLen - 27;

        if (encryptLevel != 0) {
            cmpMainLength -= (passwordPlainLength + 8);
            passwordSalt = new byte[8];
            if (bis.read(passwordSalt) != 8) throw new IOException("Error occurs while reading");
            origPasswordChecksum = new byte[passwordPlainLength];
            if (bis.read(origPasswordChecksum) != passwordPlainLength)
                throw new IOException("Error occurs while reading");
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
        unCompressMap();
        readContext();
        deleteTemp();

        rootNode = new ContextNode(indexNodes.get(0).getName());
        buildContextTree(rootNode, indexNodes.get(0));
        interpretExtraField();
    }

    private void unCompressMap() throws Exception {
        if (encryptLevel == 2) {
            FileOutputStream fos = new FileOutputStream(cmpMapName);
            Decipher decipher;
            switch (encryption) {
                case "zse":
                    decipher = new ZSEFileDecoder(bis, password, cmpMapLen);
                    break;
                case "bzse":
                    decipher = new BZSEStreamDecoder(bis, password, cmpMapLen);
                    break;
                default:
                    throw new NoSuchAlgorithmException("No Such Decoding Algorithm");
            }
            decipher.decrypt(fos);
            fos.flush();
            fos.close();
        } else {
            Util.fileTruncate(bis, cmpMapName, 8192, cmpMapLen);
        }

        DeCompressor mapDec;
        if (windowSize == 0) {
//            switch (alg) {
//                case "lzz2":
//                    mapDec = new LZZ2DeCompressor(cmpMapName, Packer.defaultWindowSize);
//                    break;
//                case "fastLzz":
//                    mapDec = new FastLzzDecompressor(cmpMapName, Packer.defaultWindowSize);
//                    break;
//                case "bwz":
//                    mapDec = new BWZDeCompressor(cmpMapName, Packer.defaultWindowSize, 0);
//                    break;
//                default:
//                    throw new NoSuchAlgorithmException("No such algorithm");
//            }
            mapDec = getDeCompressor(cmpMapName, Packer.defaultWindowSize);
        } else {
            mapDec = getDeCompressor(cmpMapName, windowSize);
        }
        FileOutputStream fos = new FileOutputStream(mapName);
        try {
            mapDec.uncompress(fos);
        } catch (Exception e) {
            fos.close();
            deleteTemp();
            mapDec.deleteCache();
            throw e;
        }
        fos.close();
    }

    private void readContext() throws IOException, ChecksumDoesNotMatchException {
        BufferedInputStream mapInputStream = new BufferedInputStream(new FileInputStream(mapName));
        CRC32 contextChecker = new CRC32();
        while (true) {
            byte[] flag = new byte[2];
            if (mapInputStream.read(flag) != 2) break;
            contextChecker.update(flag, 0, flag.length);
            boolean isDir = (flag[0] & 0xff) == 0;
            int nameLen = flag[1] & 0xff;
            byte[] nameBytes = new byte[nameLen];
            if (mapInputStream.read(nameBytes) != nameLen) throw new IOException("Error occurs while reading");
            contextChecker.update(nameBytes, 0, nameBytes.length);
            String name = Bytes.stringDecode(nameBytes);
            IndexNodeUnp inu = new IndexNodeUnp(name);

            byte[] buffer8 = new byte[8];
            if (mapInputStream.read(buffer8) != 8) throw new IOException("Error occurs while reading");
            contextChecker.update(buffer8, 0, buffer8.length);
            long start = Bytes.bytesToLong(buffer8);
            if (mapInputStream.read(buffer8) != 8) throw new IOException("Error occurs while reading");
            contextChecker.update(buffer8, 0, buffer8.length);
            long end = Bytes.bytesToLong(buffer8);
            if (isDir) {
                inu.setChildrenRange((int) start, (int) end);
                dirCount += 1;
            } else {
                inu.setScale(start, end);
                origSize = end;
                fileCount += 1;
            }
            indexNodes.add(inu);
        }
        mapInputStream.close();

        if (contextChecker.getValue() != crc32Context) {
//            throw new ChecksumDoesNotMatchException("Context damaged");
            System.err.println("CRC32 Checksum does not match");
        }

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
            if (encryptLevel == 2) {
                ByteArrayInputStream ais = new ByteArrayInputStream(byteText);
                BZSEStreamDecoder decoder = new BZSEStreamDecoder(ais, password, byteText.length);
                ByteArrayOutputStream aos = new ByteArrayOutputStream();
                decoder.decrypt(aos);
                decodeText = aos.toByteArray();
                ais.close();
                aos.close();
            } else decodeText = byteText;

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
                    deCompressor.uncompress(out);
                    uncompressedText = out.toByteArray();
                    out.close();
                } catch (Exception e) {
                    Util.deleteFile(temp);
                    throw e;
                }
                Util.deleteFile(temp);
            } else {
                uncompressedText = decodeText;
            }
            annotation = new String(uncompressedText, StandardCharsets.UTF_8);
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
        String name;
        long startPos;
        if (isSeparated) {
            step.setValue(bundle.getString("merging"));
            combineName = packName + ".comb";
            name = combineName;
            try {
                combineSplitBwz();
            } catch (IOInterruptedException e) {
                Util.deleteFile(cmpTempName);
                Util.deleteFile(combineName);
                return;
            }
            startPos = 0;
        } else {
            name = packName;
            startPos = archiveLength - cmpMainLength;
        }
        if (isTest) step.setValue(bundle.getString("testing"));
        else step.setValue(bundle.getString("uncIng"));

        BWZDeCompressor mainDec = new BWZDeCompressor(name, windowSize, startPos);
        mainDec.setParent(this);
        mainDec.setThreads(threadNumber);
        FileOutputStream mainFos = new FileOutputStream(tempName);
        try {
            mainDec.uncompress(mainFos);
            mainFos.close();
        } catch (Exception e) {
            mainDec.deleteCache();
            throw e;
        } finally {
            bis.close();
            Util.deleteFile(combineName);
            Util.deleteFile(cmpTempName);
            deleteTemp();
        }

//        if (isInterrupted) return;
//        long currentCRC32 = Security.generateCRC32(tempName);
//        if (currentCRC32 != crc32Checksum) throw new ChecksumDoesNotMatchException("CRC32 Checksum does not match");
    }

    private void combineSplitBwz() throws IOException {
        Util.fileTruncate(bis, combineName, 8192, cmpMainLength);
    }

    private void unCompressMain() throws Exception {
        if (origSize == 0) {
            File f = new File(tempName);
            if (!f.createNewFile()) System.out.println("Creation failed");
        } else if (!isUnCompressed()) {
            if (encryptLevel != 0) {
                step.setValue(bundle.getString("decrypting"));
                Decipher decipher;
                FileOutputStream fos = new FileOutputStream(cmpTempName);
                switch (encryption) {
                    case "zse":
                        decipher = new ZSEFileDecoder(bis, password, cmpMainLength);
                        break;
                    case "bzse":
                        decipher = new BZSEStreamDecoder(bis, password, cmpMainLength);
                        break;
                    default:
                        throw new NoSuchAlgorithmException("No such algorithm");
                }
                decipher.setParent(this);
                decipher.decrypt(fos);
                fos.flush();
                fos.close();
            } else if (windowSize != 0 && alg.equals("bwz")) {
                unCompressMainPureBWZ();
                return;
            } else {
                try {
                    Util.fileTruncate(bis, cmpTempName, 8192, cmpMainLength);
                } catch (IOInterruptedException e) {
                    Util.deleteFile(cmpTempName);
                    return;
                }
            }

            if (windowSize == 0) {
                tempName = cmpTempName;
            } else {
                if (isTest) step.setValue(bundle.getString("testing"));
                else step.setValue(bundle.getString("uncIng"));

                DeCompressor mainDec;
                mainDec = getDeCompressor(cmpTempName, windowSize);
                mainDec.setParent(this);
                mainDec.setThreads(threadNumber);
                FileOutputStream mainFos = new FileOutputStream(tempName);
                try {
                    mainDec.uncompress(mainFos);
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
    }

    private DeCompressor getDeCompressor(String cmpTempName, int windowSize) throws IOException, NoSuchAlgorithmException {
        DeCompressor mainDec;
        switch (alg) {
            case "lzz2":
                mainDec = new LZZ2DeCompressor(cmpTempName, windowSize);
                break;
            case "fastLzz":
                mainDec = new FastLzzDecompressor(cmpTempName, windowSize);
                break;
            case "bwz":
                mainDec = new BWZDeCompressor(cmpTempName, windowSize, 0);
                break;
            default:
                throw new NoSuchAlgorithmException("No such algorithm");
        }
        return mainDec;
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
        isTest = false;
        startTime = System.currentTimeMillis();
        progress.set(1);
        unCompressMain();

        if (isInterrupted) {
            failInfo = bundle.getString("operationInterrupted");
            throw new IOInterruptedException();
        }

        step.setValue(bundle.getString("verifying"));
        long currentCRC32 = Security.generateCRC32(tempName);
        if (currentCRC32 != crc32Checksum) {
            failInfo = bundle.getString("fileDamaged");
//            throw new ChecksumDoesNotMatchException("CRC32 Checksum does not match");
            System.err.println("CRC32 Checksum does not match");
        }

        RandomAccessFile raf = new RandomAccessFile(tempName, "r");
        String dirOffset;
        if (!cn.getPath().contains(File.separator)) dirOffset = "";
        else dirOffset = cn.getPath().substring(0, cn.getPath().lastIndexOf(File.separator));
        step.setValue(bundle.getString("extracting"));
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
     * @return true if the file can be uncompress successfully and the uncompressed file is the same as the
     * original file
     */
    public boolean TestPack() {
        isTest = true;
        startTime = System.currentTimeMillis();
        try {
            unCompressMain();
            if (isInterrupted) return false;
            step.setValue(bundle.getString("verifying"));
            long currentCRC32 = Security.generateCRC32(tempName);
            return currentCRC32 == crc32Checksum;
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
        byte[] checksum = Security.secureHashing(password, passwordAlg);
        byte[] saltedChecksum = new byte[checksum.length + 8];
        System.arraycopy(checksum, 0, saltedChecksum, 0, checksum.length);
        System.arraycopy(passwordSalt, 0, saltedChecksum, checksum.length, 8);
        byte[] saltedHash = Security.secureHashing(saltedChecksum, passwordAlg);

        if (!Arrays.equals(saltedHash, origPasswordChecksum)) throw new WrongPasswordException();
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
     * Returns {@code true} if and only if this archive is a split archive.
     *
     * @return {@code true} if and only if this archive is a split archive}
     */
    public boolean isSeparated() {
        return isSeparated;
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

    public long getContextLength() {
        return cmpMapLen;
    }

    public long getCmpMainLength() {
        return cmpMainLength;
    }

    public long getOtherInfoLength() {
        return extraField.length;
    }

    /**
     * Sets the {@code languageLoader} language loader.
     * <p>
     * language loader is used for displaying text in different languages on the GUI.
     *
     * @param bundle the language loader.
     */
    public void setLanguageLoader(ResourceBundle bundle) {
        this.bundle = bundle;
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
     * Returns the name of the encryption algorithm.
     *
     * @return the name of the encryption algorithm
     */
    public String getEncryption() {
        return encryption;
    }

    /**
     * Returns the name of the secret key algorithm.
     *
     * @return the name of the secret key algorithm
     */
    public String getPasswordAlg() {
        return passwordAlg;
    }

    /**
     * Returns the true ratio of compression, which is the ratio between the length of the main part in the archive
     * and the original file size.
     *
     * @return the compression ratio of main part
     */
    public double getMainRatio() {
        return (double) cmpMainLength / origSize;
    }

    /**
     * Returns the total length of the archive(s), used for displaying.
     *
     * @return the total length of the archive(s), used for displaying.
     */
    public long getDisplayArchiveLength() {
        if (isSeparated) {
            return archiveLength + (partCount - 1) * 4;
        } else {
            return archiveLength;
        }
    }

    private boolean isUnCompressed() {
        File f = new File(tempName);
        return f.exists() && f.length() == origSize;
    }

    /**
     * Checks the signature of the archive file.
     *
     * @return {@code 0} if this archive is a WinLZZ archive,
     * {@code 1} if it is a WinLZZ archive section,
     * {@code 2} if unrecognizable.
     */
    public static int checkSignature(String path) {
        try {
            FileInputStream fis = new FileInputStream(path);
            byte[] sigBytes = new byte[4];
            if (fis.read(sigBytes) != 4) {
                fis.close();
                return 2;
            }
            fis.close();
            int signature = Bytes.bytesToInt32(sigBytes);
            if (signature == Packer.SIGNATURE) return 0;
            else if (signature == Packer.PART_SIGNATURE) return 1;
            else return 2;
        } catch (IOException e) {
            return 2;
        }
    }

    public String getArchiveFullVersion() {
        String primary = String.valueOf(primaryVersion & 0xff);
        switch (alg) {
            case "bwz":
                return primary + String.format(".%d.%d.%d", algVersion, LZZ2Compressor.VERSION, FastLzzCompressor.VERSION);
            case "lzz2":
                return primary + String.format(".%d.%d.%d", BWZCompressor.VERSION, algVersion, FastLzzCompressor.VERSION);
            case "fastLzz":
                return primary + String.format(".%d.%d.%d", BWZCompressor.VERSION, LZZ2Compressor.VERSION, algVersion);
            default:
                throw new RuntimeException("Unknown algorithm");
        }
    }

    /**
     * Returns the information of a decompression failure, which would be set when fail.
     *
     * @return the information of a decompression failure, which would be set when fail
     */
    public String getFailInfo() {
        return failInfo;
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


