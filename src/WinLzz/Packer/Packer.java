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
import WinLzz.GraphicUtil.AnnotationNode;
import WinLzz.Interface.Compressor;
import WinLzz.LZZ2.LZZ2Compressor;
import WinLzz.ResourcesPack.Languages.LanguageLoader;
import WinLzz.Utility.Bytes;
import WinLzz.Utility.Util;
import WinLzz.ZSE.ZSEEncoder;
import WinLzz.ZSE.ZSEFileEncoder;
import javafx.beans.property.*;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

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
    public final static byte primaryVersion = 23;

    /**
     * The secondary core version.
     */
    public final static byte secondaryVersion = 1;

    /**
     * The signature for a WinLZZ archive (*.pz) file.
     */
    final static int HEADER = 0x03F92FBD;

    /**
     * Set 1999-12-31 19:00 as the initial time.
     */
    final static long dateOffset = 946684800000L;

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
     * List of {@code IndexNode}'s.
     * <p>
     * This list is order-sensitive. Each node represents an actual file except the root node.
     */
    private ArrayList<IndexNode> indexNodes = new ArrayList<>();

    private String password;
    private String alg;

    /**
     * List of extra field blocks.
     * <p>
     * Each element in this list is an extra field block, recorded in byte-array form.
     * <p>
     * Structure of each block:
     * 0: flag
     * 1: reserved
     * 2, 3: unit length (n)
     * 4 ~ 4 + n: block.
     * Extra field one-byte flags:
     * 0: reserved
     * 1: annotation
     */
    private ArrayList<byte[]> extraFields = new ArrayList<>();

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

    /**
     * Creates a new {@code Packer} instance.
     * <p>
     * All input root files should be under a same path.
     *
     * @param inFiles the input root files.
     */
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
        bos.write(Util.windowSizeToByte(windowSize));  // Write window size: 1 byte
        bos.write(new byte[4]);  // Reserved for creation time.
        bos.write(new byte[4]);  // Reserved for crc32 checksum
        bos.write(new byte[4]);  // Reserved for header size.

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
        String tempMainName = outFile + ".main";
        BufferedOutputStream headBos = new BufferedOutputStream(new FileOutputStream(tempHeadName));
        FileOutputStream mainBos = new FileOutputStream(tempMainName);

        writeMapToStream(headBos, mainBos);

        headBos.flush();
        headBos.close();
        mainBos.flush();
        mainBos.close();

        long crc32 = Util.generateCRC32(tempMainName);
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
     * Sets the encryption parameters of this archive.
     *
     * @param password the password text.
     * @param level    the {@code encryptLevel}.
     */
    public void setEncrypt(String password, int level) {
        this.password = password;
        this.encryptLevel = level;
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
    public void setAnnotation(AnnotationNode annotation) {
        try {
            byte[] encAnnotation;
            if (encryptLevel == 2) {
                ZSEEncoder encoder = new ZSEEncoder(annotation.getAnnotation(), password);
                encAnnotation = encoder.Encode();
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
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets the {@code lanLoader} language loader.
     * <p>
     * language loader is used for displaying text in different languages on the GUI.
     *
     * @param lanLoader the language loader.
     */
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
