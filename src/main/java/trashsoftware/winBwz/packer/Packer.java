/*
 * PZ archive packer.
 *
 * Archive header info:
 * 4 bytes: PZ header
 * 2 bytes: version
 * 2 bytes: info bytes
 * 1 byte: window size
 * 4 bytes: time of creation
 * 4 bytes: CRC32 checksum for context
 * 4 bytes: CRC32 checksum for main text
 * 4 bytes: followed context length (n)
 * 2 bytes: extra field length (m)
 * m bytes: extra field
 * n bytes: context
 */

package trashsoftware.winBwz.packer;

import trashsoftware.winBwz.core.bwz.BWZCompressor;
import trashsoftware.winBwz.encrypters.bzse.BZSEStreamEncoder;
import trashsoftware.winBwz.gui.graphicUtil.AnnotationNode;
import trashsoftware.winBwz.core.Compressor;
import trashsoftware.winBwz.encrypters.Encipher;
import trashsoftware.winBwz.core.lzz2.LZZ2Compressor;
import trashsoftware.winBwz.utility.*;
import trashsoftware.winBwz.encrypters.zse.ZSEFileEncoder;
import javafx.beans.property.*;
import trashsoftware.winBwz.core.fastLzz.FastLzzCompressor;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.zip.CRC32;

/**
 * The .pz archive packing program.
 * This program packs multiple files and directories into one .pz archive file.
 *
 * @author zbh
 * @since 0.4
 */
public class Packer {

    /**
     * The primary core version.
     * <p>
     * Any change of this value will result the incompatibility between the program and older archive file.
     */
    public final static byte primaryVersion = 25;

    public static final int FIXED_HEAD_LENGTH = 27;

    /**
     * The signature for a WinLZZ archive (*.pz) file.
     */
    public final static int SIGNATURE = 0x03F92FBD;

    /**
     * The digital signature of a section of a split compress file.
     */
    public final static int PART_SIGNATURE = 0x016416DA;

    /**
     * The default size of sliding window/block for sliding-window-based or block-based compression algorithm.
     * <p>
     * The value will be applied only if the user sets the {@code windowSize} to 0.
     */
    static final int defaultWindowSize = 32768;

    private int fileCount, threads;

    /**
     * The detailed-level of compression.
     * <p>
     * Often specified in each implementation of {@code Compressor}.
     */
    private int cmpLevel;

    /**
     * The encryption level of this archive.
     * <p>
     * 0 for no encryption, 1 for there is encryption for content, 2 for encryption for both content and context map.
     */
    private int encryptLevel;

    /**
     * Total length before compression.
     */
    private long totalLength;

    /**
     * Archive length after compression.
     */
    private long compressedLength;

    /**
     * Maximum length of each archive file. 0 if do not separate.
     */
    private long partSize;

    /**
     * List of {@code IndexNode}'s.
     * <p>
     * This list is order-sensitive. Each node represents an actual file except the root node.
     */
    private ArrayList<IndexNode> indexNodes = new ArrayList<>();

    private File[] inFiles;

    private String password;

    /**
     * The encryption algorithm.
     */
    private String encryption;

    private String passwordAlg;
    private String alg;

    /**
     * The CRC32 checksum generator of the context.
     */
    private CRC32 contextCrc = new CRC32();

    /**
     * List of extra field blocks.
     * <p>
     * Each element in this list is an extra field block, recorded in byte-array form.
     * <p>
     * Standard structure of each block:
     * 0: flag
     * 1: reserved
     * 2, 3: unit length (n)
     * 4 ~ 4 + n: block.
     * <p>
     * Extra field one-byte flags:
     * 0: reserved
     * 1: annotation
     * None: partition info
     */
    private ArrayList<byte[]> extraFields = new ArrayList<>();

    public final ReadOnlyLongWrapper progress = new ReadOnlyLongWrapper();
    public final ReadOnlyStringWrapper percentage = new ReadOnlyStringWrapper();
    public final ReadOnlyStringWrapper ratio = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper step = new ReadOnlyStringWrapper();
    public final ReadOnlyStringWrapper file = new ReadOnlyStringWrapper();
    public final ReadOnlyStringWrapper timeUsed = new ReadOnlyStringWrapper();
    public final ReadOnlyStringWrapper timeExpected = new ReadOnlyStringWrapper();
    public final ReadOnlyStringWrapper passedLength = new ReadOnlyStringWrapper();
    public final ReadOnlyStringWrapper cmpLength = new ReadOnlyStringWrapper();
    public final ReadOnlyStringWrapper currentCmpRatio = new ReadOnlyStringWrapper();

    public long startTime = System.currentTimeMillis();
    public boolean isInterrupted;
    private ResourceBundle bundle;

    public final ReadOnlyIntegerWrapper exitStatus = new ReadOnlyIntegerWrapper();
    public String errorMsg;

    /**
     * Creates a new {@code Packer} instance.
     * <p>
     * All input root files should be under a same path.
     *
     * @param inFiles the input root files.
     */
    public Packer(File[] inFiles) {
        this.inFiles = inFiles;
    }

    /**
     * Builds the file structure.
     */
    public void build() {
        RootFile rf = new RootFile(inFiles);
        IndexNode rootNode = new IndexNode(rf);
        fileCount = 1;
        indexNodes.add(rootNode);
        buildIndexTree(rf, rootNode);
    }

    private void buildIndexTree(File file, IndexNode currentNode) {
        if (isInterrupted) return;
        if (file.isDirectory()) {
            this.file.setValue(file.getAbsolutePath() + "\\");
            File[] sub = file.listFiles();

            int currentCount = fileCount;
            assert sub != null;
            IndexNode[] tempArr = new IndexNode[sub.length];
            int arrIndex = 0;
            for (File f : sub) {
                IndexNode in = new IndexNode(f.getName(), f);
                tempArr[arrIndex++] = in;
                indexNodes.add(in);
                fileCount += 1;
            }
            currentNode.setChildrenRange(currentCount, fileCount);
            for (int i = 0; i < sub.length; i++) if (!sub[i].isDirectory()) buildIndexTree(sub[i], tempArr[i]);
            for (int i = 0; i < sub.length; i++) if (sub[i].isDirectory()) buildIndexTree(sub[i], tempArr[i]);
        } else {
            long start = totalLength;
            totalLength += file.length();
            currentNode.setSize(start, totalLength);
        }
    }

    private void writeMapToStream(OutputStream headBos, LinkedList<File> mainList) throws IOException {
        for (IndexNode in : indexNodes) {
            byte[] array = in.toByteArray();
            contextCrc.update(array, 0, array.length);
            headBos.write(array);
            if (!in.isDir()) mainList.addLast(in.getFile());
        }
    }

    /**
     * Returns the total length of original files.
     *
     * @return {@code totalLength} the total length of original files.
     */
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
//        if (lanLoader != null) step.setValue(lanLoader.get(270));
        if (bundle != null) step.setValue(bundle.getString("createDatabase"));
        percentage.set("0.0");
        OutputStream bos;
        if (partSize == 0) bos = new BufferedOutputStream(new FileOutputStream(outFile));
        else {
            bos = new SeparateOutputStream(outFile, partSize, true, PART_SIGNATURE);
            setPartialInfo();
        }
        bos.write(Bytes.intToBytes32(SIGNATURE));  // Write header : 4 bytes
        bos.write(primaryVersion);  // Write version : 1 byte
        // another 1 byte written after alg

        /*
         * Info:
         * [0:2) encrypt level
         * [2:4) algorithm
         * [5] if separate
         */
        byte inf = (byte) 0;

        /*
        `* Encryption info:
         * [0:2) encryption algorithm
         * [2:5) password encryption algorithm
         */
        byte encInf = (byte) 0;

        switch (encryptLevel) {
            case 0:  // 00
                break;
            case 1:
                inf = (byte) (inf | 0b10000000);  // 10
                break;
            case 2:
                inf = (byte) (inf | 0b01000000);  // 01
                break;
        }

        int algVersion;
        switch (alg) {
            case "lzz2":  // 00
                algVersion = LZZ2Compressor.VERSION;
                break;
            case "bwz":
                inf = (byte) (inf | 0b00100000);  // 10
                algVersion = BWZCompressor.VERSION;
                break;
            case "fastLzz":
                inf = (byte) (inf | 0b00110000);  // 11
                algVersion = FastLzzCompressor.VERSION;
                break;
            default:
                throw new RuntimeException("Unknown algorithm");
        }
        bos.write(algVersion);  // write algorithm version : 1 byte

        if (partSize != 0) inf = (byte) (inf | 0b00001000);  // If compress separately

        if (encryptLevel != 0) {
            switch (encryption) {
                case "zse":
                    break;
                case "bzse":
                    encInf = (byte) (encInf | 0b10000000);
                    break;
            }

            switch (passwordAlg) {
                case "md5":
                    break;  // 000
                case "sha-256":
                    encInf = (byte) (encInf | 0b00001000);  // 001
                    break;
                case "sha-384":
                    encInf = (byte) (encInf | 0b00010000);  // 010
                    break;
                case "sha-512":
                    encInf = (byte) (encInf | 0b00011000);  // 011
                    break;
                case "zha64":
                    encInf = (byte) (encInf | 0b00100000);  // 100
                    break;
            }
        }
        compressedLength = 27;

        bos.write(inf);  // Write info: 1 byte
        bos.write(encInf);  // Write encryption info: 1 byte
        bos.write(Util.windowSizeToByte(windowSize));  // Write window size: 1 byte

        /*
         * Reserved bytes
         * 4 for creation time,
         * 4 for crc32 checksum of context
         * 4 for crc32 checksum of main file
         * 4 for context size
         */
        bos.write(new byte[16]);

        byte[] extraField = new byte[Util.collectionOfArrayLength(extraFields)];  // Extra field
        int i = 0;
        for (byte[] by : extraFields) {
            System.arraycopy(by, 0, extraField, i, by.length);
            i += by.length;
        }
        compressedLength += extraField.length;
        byte[] extraFieldLength = Bytes.intToBytes32(extraField.length);
        byte[] extraFieldLength2 = new byte[2];
        System.arraycopy(extraFieldLength, 2, extraFieldLength2, 0, 2);
        bos.write(extraFieldLength2);  // Extra field length
        bos.write(extraField);

        String tempHeadName = outFile + ".head";
        BufferedOutputStream headBos = new BufferedOutputStream(new FileOutputStream(tempHeadName));

        LinkedList<File> inputStreams = new LinkedList<>();

        writeMapToStream(headBos, inputStreams);  // Traverses the whole directory

        headBos.flush();
        headBos.close();

        // Stores the hashing value of password, with salt
        if (encryptLevel != 0) {
            byte[] hashPassword = Security.secureHashing(password, passwordAlg);
            byte[] salt = Security.generateRandomSequence();
            byte[] saltedPassword = new byte[hashPassword.length + 8];
            System.arraycopy(hashPassword, 0, saltedPassword, 0, hashPassword.length);
            System.arraycopy(salt, 0, saltedPassword, hashPassword.length, 8);
            byte[] saltedHash = Security.secureHashing(saltedPassword, passwordAlg);
            bos.write(salt);
            bos.write(saltedHash);
            compressedLength += (saltedHash.length + 8);
        }

        Compressor headCompressor;
        if (windowSize == 0) {
            switch (alg) {
                case "lzz2":
                    headCompressor = new LZZ2Compressor(tempHeadName, defaultWindowSize, 64);
                    break;
                case "fastLzz":
                    headCompressor = new FastLzzCompressor(tempHeadName, defaultWindowSize, 64);
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
                case "fastLzz":
                    headCompressor = new FastLzzCompressor(tempHeadName, windowSize, bufferSize);
                    headCompressor.setCompressionLevel(cmpLevel);
                    break;
                case "bwz":
                    headCompressor = new BWZCompressor(tempHeadName, windowSize);
                    break;
                default:
                    throw new NoSuchAlgorithmException("No such algorithm");
            }
        }
        long cmpHeadLen;
        if (encryptLevel != 2) {
            headCompressor.compress(bos);
            cmpHeadLen = headCompressor.getCompressedSize();
        } else {
            String encHeadName = outFile + ".head.enc";
            FileOutputStream encFos = new FileOutputStream(encHeadName);
            headCompressor.compress(encFos);
            encFos.flush();
            encFos.close();
            InputStream encHeadIs;
            Encipher encipher;
            switch (encryption) {
                case "zse":
                    encHeadIs = new FileInputStream(encHeadName);
                    encipher = new ZSEFileEncoder(encHeadIs, password);
                    break;
                case "bzse":
                    encHeadIs = new BufferedInputStream(new FileInputStream(encHeadName));
                    encipher = new BZSEStreamEncoder(encHeadIs, password);
                    break;
                default:
                    throw new NoSuchAlgorithmException("No Such Encoding Algorithm");
            }
            encipher.encrypt(bos);
            cmpHeadLen = encipher.encryptedLength();
            encHeadIs.close();
            Util.deleteFile(encHeadName);
        }
        compressedLength += cmpHeadLen;

        Util.deleteFile(tempHeadName);

//        if (lanLoader != null) step.setValue(lanLoader.get(206));
        if (bundle != null) step.setValue(bundle.getString("compressing"));

        String encMainName = outFile + ".enc";

        MultipleInputStream mis;
        if (windowSize == 0) {
            if (encryptLevel == 0) {
                mis = new MultipleInputStream(inputStreams, this, false);
                Util.fileTruncate(mis, bos, 8192, totalLength);
                compressedLength += totalLength;
            } else {
                Encipher encipher;
                switch (encryption) {
                    case "zse":
                        mis = new MultipleInputStream(inputStreams, this, false);
                        encipher = new ZSEFileEncoder(mis, password);
                        break;
                    case "bzse":
                        mis = new MultipleInputStream(inputStreams, this, true);
                        encipher = new BZSEStreamEncoder(mis, password);
                        break;
                    default:
                        throw new NoSuchAlgorithmException("No Such Encoding Algorithm");
                }
                if (bundle != null) step.setValue(bundle.getString("encrypting"));
//                step.setValue(lanLoader.get(271));
                progress.set(1);
                percentage.setValue("0.0");
                encipher.setParent(this, totalLength);
                encipher.encrypt(bos);
                compressedLength += encipher.encryptedLength();
            }
        } else if (totalLength != 0) {
            mis = new MultipleInputStream(inputStreams, this, false);
            Compressor mainCompressor;
            switch (alg) {
                case "lzz2":
                    mainCompressor = new LZZ2Compressor(mis, windowSize, bufferSize, totalLength);
                    break;
                case "fastLzz":
                    mainCompressor = new FastLzzCompressor(mis, windowSize, bufferSize, totalLength);
                    break;
                case "bwz":
                    mainCompressor = new BWZCompressor(mis, windowSize);
                    break;
                default:
                    throw new NoSuchAlgorithmException("No such algorithm");
            }
            mainCompressor.setPacker(this);
            mainCompressor.setCompressionLevel(cmpLevel);
            mainCompressor.setThreads(threads);
            if (encryptLevel == 0) {
                mainCompressor.compress(bos);
            } else {
                FileOutputStream encFos = new FileOutputStream(encMainName);
                mainCompressor.compress(encFos);
                encFos.flush();
                encFos.close();

                InputStream encMainIs;
                Encipher encipher;
                switch (encryption) {
                    case "zse":
                        encMainIs = new FileInputStream(encMainName);
                        encipher = new ZSEFileEncoder(encMainIs, password);
                        break;
                    case "bzse":
                        encMainIs = new BufferedInputStream(new FileInputStream(encMainName));
                        encipher = new BZSEStreamEncoder(encMainIs, password);
                        break;
                    default:
                        throw new NoSuchAlgorithmException("No Such Encoding Algorithm");
                }
                if (bundle != null) step.setValue(bundle.getString("encrypting"));
                file.setValue(outFile);
                progress.set(1);
                percentage.setValue("0.0");
                encipher.setParent(this, mainCompressor.getCompressedSize());
                encipher.encrypt(bos);
                encMainIs.close();
            }
            compressedLength += mainCompressor.getCompressedSize();
        } else {
            mis = new MultipleInputStream();
        }
        long crc32 = mis.getCrc32Checksum();
        byte[] fullBytes = Bytes.longToBytes(crc32);
        byte[] crc32Checksum = new byte[4];
        System.arraycopy(fullBytes, 4, crc32Checksum, 0, 4);

        Util.deleteFile(encMainName);
        bos.flush();
        bos.close();
        mis.close();
        if (isInterrupted) {
            Util.deleteFile(outFile);
            return;
        }

        long contextCrcValue = contextCrc.getValue();
        byte[] contextCrcArray = Arrays.copyOfRange(Bytes.longToBytes(contextCrcValue), 4, 8);

        RandomAccessFile raf;
        if (bos instanceof SeparateOutputStream) {
            raf = new RandomAccessFile(((SeparateOutputStream) bos).getFirstName(), "rw");
        } else raf = new RandomAccessFile(outFile, "rw");

        raf.seek(9);
        int currentTimeInt = Bytes.getCurrentTimeInInt();  // Creation time,
        // rounded to second. Starting from 1999-12-31 19:00
        raf.writeInt(currentTimeInt);
        raf.write(contextCrcArray);
        raf.write(crc32Checksum);
        raf.writeInt((int) cmpHeadLen);

        if (bos instanceof SeparateOutputStream) {
            // If the archive is partially
            int fCount = ((SeparateOutputStream) bos).getCount();
            raf.seek(27);  // Seek to the first extra field block.
            raf.writeInt(fCount);
            raf.writeLong(((SeparateOutputStream) bos).getCumulativeLength());
        }
        raf.close();
    }

    private void setPartialInfo() {
        byte[] block = new byte[12];  // This is not a standard extra block
        // Structure:
        // blocks count: 4 bytes
        // length of the whole archive: 8 bytes
        extraFields.add(0, block);  // To make sure this is the first block, which is because when compresses
        // partially, this block should be in the first archive part.
    }

    /**
     * Sets up the compression algorithm.
     *
     * @param alg the abbreviation of the newly-set compression algorithm.
     */
    public void setAlgorithm(String alg) {
        this.alg = alg;
    }

    /**
     * Returns the length after compression (length of archive).
     *
     * @return the length after compression.
     */
    public long getCompressedLength() {
        return compressedLength;
    }

    /**
     * Sets the compression level.
     *
     * @param cmpLevel the compression level.
     */
    public void setCmpLevel(int cmpLevel) {
        this.cmpLevel = cmpLevel;
    }

    /**
     * Sets up the maximum length of each archive file. 0 if do not separate.
     *
     * @param partSize the maximum length of each archive file
     */
    public void setPartSize(long partSize) {
        this.partSize = partSize;
    }

    /**
     * Sets the encryption parameters of this archive.
     *
     * @param password    the password text.
     * @param level       the {@code encryptLevel}.
     * @param encryption  the encryption algorithm
     * @param passwordAlg the hash algorithm used for encrypting password
     */
    public void setEncrypt(String password, int level, String encryption, String passwordAlg) {
        this.password = password;
        this.encryptLevel = level;
        this.encryption = encryption;
        this.passwordAlg = passwordAlg;
    }

    /**
     * Sets the thread number used for compression.
     *
     * @param threads thread number for compression.
     */
    public void setThreads(int threads) {
        this.threads = threads;
    }

    /**
     * Interrupts the current running packing progress.
     */
    public void interrupt() {
        this.isInterrupted = true;
    }

    /**
     * Add annotation to the package.
     *
     * @param annotation the node contains annotation text and info.
     */
    public void setAnnotation(AnnotationNode annotation) throws IOException {
        byte[] encAnnotation;
        if (encryptLevel == 2) {
            ByteArrayInputStream ais = new ByteArrayInputStream(annotation.getAnnotation());
            BZSEStreamEncoder encoder = new BZSEStreamEncoder(ais, password);
            ByteArrayOutputStream aos = new ByteArrayOutputStream();
            encoder.encrypt(aos);
            encAnnotation = aos.toByteArray();
            ais.close();
            aos.close();
        } else {
            encAnnotation = annotation.getAnnotation();
        }
        short length = (short) encAnnotation.length;
        byte[] result = new byte[length + 4];
        result[0] = 1;
        if (annotation.isCompressed()) result[1] = 1;
        byte[] lengthBytes = Bytes.shortToBytes(length);
        System.arraycopy(lengthBytes, 0, result, 2, 2);
        System.arraycopy(encAnnotation, 0, result, 4, encAnnotation.length);
        extraFields.add(result);
    }

    /**
     * Sets the {@code ResourceBundle} language loader.
     * <p>
     * language loader is used for displaying text in different languages on the GUI.
     *
     * @param bundle the language loader.
     */
    public void setLanLoader(ResourceBundle bundle) {
        this.bundle = bundle;
    }

    public void setError(String msg, int status) {
//        interrupt();
        exitStatus.set(status);
        errorMsg = msg;
    }

    public static String getProgramFullVersion() {
        return String.format("%d.%d.%d.%d",
                Packer.primaryVersion & 0xff,
                BWZCompressor.VERSION,
                LZZ2Compressor.VERSION,
                FastLzzCompressor.VERSION);
    }

    public ReadOnlyLongProperty progressProperty() {
        return progress;
    }

    public ReadOnlyStringProperty stepProperty() {
        return step;
    }

    public ReadOnlyStringProperty fileProperty() {
        return file;
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

    public ReadOnlyIntegerWrapper exitStatusProperty() {
        return exitStatus;
    }
}


/**
 * A class used for recording information of a file in an archive in the time of packing.
 *
 * @author zbh
 * @since 0.4
 */
class IndexNode {

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
     * Whether this IndexNode represents a directory.
     */
    private boolean isDir;

    /**
     * The {@code File} file.
     */
    private File file;

    /**
     * The start and end position of children of this IndexNode in the uncompressed context map.
     */
    private int[] childrenRange;

    /**
     * Creates a new {@code IndexNode} instance for a directory.
     *
     * @param name directory path.
     */
    IndexNode(String name, File file) {
        this.file = file;
        this.name = name;
        isDir = true;
    }

    /**
     * Creates a new {@code IndexNode} instance for the virtual root file <code>rf</code>.
     *
     * @param rf the virtual root file.
     */
    IndexNode(RootFile rf) {
        this.file = rf;
        this.name = "";
        isDir = true;
    }

    /**
     * Returns the actual file, or virtual {@code RootFile} represented by this {@code IndexNode}.
     *
     * @return the actual file, or virtual {@code RootFile} represented by this {@code IndexNode}.
     */
    public File getFile() {
        return file;
    }

    /**
     * Returns the length of the file.
     *
     * @return the length of the file.
     */
    public long getSize() {
        return end - start;
    }

    /**
     * Sets up the start and end position of this file in the uncompressed main part of this archive.
     *
     * @param start the start position.
     * @param end   the end position.
     */
    void setSize(long start, long end) {
        this.start = start;
        this.end = end;
        isDir = false;
    }

    /**
     * Sets up the children {@code IndexNode}'s beginning and stop position in the total {@code IndexNode} list.
     *
     * @param begin the children's beginning position in node list.
     * @param stop  the children's stop position in node list.
     */
    void setChildrenRange(int begin, int stop) {
        childrenRange = new int[]{begin, stop};
    }

    /**
     * Returns the byte array representation of this {@code IndexNode}.
     *
     * @return the byte array representation of this {@code IndexNode}.
     * @throws UnsupportedEncodingException if the file name is too long (>255 bytes) or,
     *                                      the name cannot be encoded.
     */
    byte[] toByteArray() throws UnsupportedEncodingException {
        byte[] nameBytes = Bytes.stringEncode(name);
        int len = nameBytes.length;
        if (len > 255) throw new UnsupportedEncodingException();
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

    /**
     * Returns whether the file represented by this {@code IndexNode} is a directory.
     *
     * @return {@code true} if the file represented by this {@code IndexNode} is a directory.
     */
    boolean isDir() {
        return isDir;
    }

    @Override
    public String toString() {
        if (isDir) return "Dir(" + name + ", " + Arrays.toString(childrenRange) + ")";
        else return "File(" + name + ": " + start + ", " + end + ")";
    }
}


/**
 * A special kind of file which marks the root directory of a archive file.
 * <p>
 * This kind of file does not exist in the disk.
 *
 * @author zbh
 * @since 0.7
 */
class RootFile extends File {

    private File[] children;

    /**
     * Creates a new {@code RootFile} instance.
     *
     * @param children children if this {@code RootFile}.
     */
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
