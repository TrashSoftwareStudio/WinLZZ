package trashsoftware.winBwz.packer.pz;

import trashsoftware.winBwz.core.Compressor;
import trashsoftware.winBwz.core.bwz.BWZCompressor;
import trashsoftware.winBwz.core.deflate.DeflateCompressor;
import trashsoftware.winBwz.core.fastLzz.FastLzzCompressor;
import trashsoftware.winBwz.core.lzz2.LZZ2Compressor;
import trashsoftware.winBwz.encrypters.Encipher;
import trashsoftware.winBwz.encrypters.bzse.BZSEStreamEncoder;
import trashsoftware.winBwz.encrypters.zse.ZSEFileEncoder;
import trashsoftware.winBwz.gui.graphicUtil.AnnotationNode;
import trashsoftware.winBwz.packer.Packer;
import trashsoftware.winBwz.packer.SeparateException;
import trashsoftware.winBwz.utility.Bytes;
import trashsoftware.winBwz.utility.Security;
import trashsoftware.winBwz.utility.SeparateOutputStream;
import trashsoftware.winBwz.utility.Util;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.zip.CRC32;

public abstract class PzPacker extends Packer {

    /**
     * The primary core version.
     * <p>
     * Any change of this value will result the incompatibility between the program and older archive file.
     */
    public final static byte primaryVersion = 28;

    public static final int FIXED_HEAD_LENGTH = 27;

    /**
     * The signature for a solid WinLZZ archive (*.pz) file.
     */
    public final static int SIGNATURE = 0x03F92FBD;

    /**
     * The signature for a non-solid WinLZZ archive (*.pz) file.
     */
    public final static int SIGNATURE_NS = 0x03F92FBE;

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
    public final long startTime = System.currentTimeMillis();
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
    protected final ArrayList<byte[]> extraFields = new ArrayList<>();
    protected long fileStructurePos;

    /**
     * List of {@code IndexNode}'s.
     * <p>
     * This list is order-sensitive. Each node represents an actual file except the root node.
     */
    protected final List<IndexNode> indexNodes = new ArrayList<>();
    /**
     * The CRC32 checksum generator of the context.
     */
    protected final CRC32 contextCrc = new CRC32();
    public boolean isInterrupted;
    protected String password;
    /**
     * The encryption algorithm.
     */
    protected String encryption;
    protected String passwordAlg;
    protected String alg;
    /**
     * Maximum length of each archive file. 0 if do not separate.
     */
    protected long partSize;
    protected int fileCount, threads;
    /**
     * The detailed-level of compression.
     * <p>
     * Often specified in each implementation of {@code Compressor}.
     */
    protected int cmpLevel;
    /**
     * The encryption level of this archive.
     * <p>
     * 0 for no encryption, 1 for there is encryption for content, 2 for encryption for both content and context map.
     */
    protected int encryptLevel;

    public PzPacker(File[] inFiles) {
        super(inFiles);
    }

    protected abstract int getSignature();

    /**
     * Writes or compresses the main part and returns the crc32 checksum
     *
     * @param outFile      name of archive file
     * @param bos          output stream of archive
     * @param inputStreams input files
     * @param windowSize   window size
     * @param bufferSize   buffer size
     * @return the CRC32 checksum
     * @throws Exception if any exception occurs
     */
    protected abstract long writeBody(String outFile,
                                      OutputStream bos,
                                      Deque<File> inputStreams,
                                      int windowSize,
                                      int bufferSize) throws Exception;

    protected void writeInfoHead(OutputStream bos,
                                 int windowSize)
            throws IOException {
        bos.write(Bytes.intToBytes32(getSignature()));  // Write signature : 4 bytes
        bos.write(primaryVersion);  // Write version : 1 byte
        // another 1 byte written after alg

        /*
         * Info:
         * [0:2) encrypt level
         * [2:6) algorithm
         * [7] if separate
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
            case "deflate":
                inf = (byte) (inf | 0b00010000);  // 01
                algVersion = 0;
                break;
            default:
                throw new RuntimeException("Unknown algorithm");
        }
        bos.write(algVersion);  // write algorithm version : 1 byte

        if (partSize != 0) inf = (byte) (inf | 0b00000001);  // If compress separately

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
         * 4 for crc32 checksum of main file, or nothing
         * 4 for file structure size
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

//        BufferedOutputStream headBos = new BufferedOutputStream(new FileOutputStream(tempHeadName));
//
//        writeMapToStream(headBos, inputStreams);  // Traverses the whole directory
//
//        headBos.flush();
//        headBos.close();
    }

    protected void writeCmpMapToTemp(String tempHeadName, Deque<File> inputStreams) throws IOException {
        BufferedOutputStream headBos = new BufferedOutputStream(new FileOutputStream(tempHeadName));

        writeMapToStream(headBos, inputStreams);  // Traverses the whole directory

        headBos.flush();
        headBos.close();
    }

    protected long writeCmpHead(String outFile,
                                String tempHeadName,
                                OutputStream bos,
                                int windowSize,
                                int bufferSize) throws Exception {
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
                case "deflate":
                    headCompressor = new DeflateCompressor(tempHeadName, 6);
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
                case "deflate":
                    headCompressor = new DeflateCompressor(tempHeadName, cmpLevel);
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

        return cmpHeadLen;
    }

    protected void writeInfoToFirstFile(RandomAccessFile rafOfFirst) throws IOException {
    }

    /**
     * Pack the directory into a *.pz file.
     *
     * @param outFile    the output package file.
     * @param windowSize size of sliding window.
     * @param bufferSize size of look ahead buffer (if algorithm supported).
     * @throws Exception if any IO error occurs.
     */
    @Override
    public void pack(String outFile, int windowSize, int bufferSize) throws Exception {
        if (bundle != null) step.setValue(bundle.getString("createDatabase"));
        percentage.set("0.0");
        OutputStream bos;
        if (partSize == 0) bos = new BufferedOutputStream(new FileOutputStream(outFile));
        else {
            bos = new SeparateOutputStream(outFile, partSize, true, PART_SIGNATURE);
            setPartialInfo();
        }

        String tempHeadName = outFile + ".head";
        Deque<File> inputStreams = new LinkedList<>();
        writeInfoHead(bos, windowSize);
        writeCmpMapToTemp(tempHeadName, inputStreams);

//        fileStructurePos = compressedLength;

        long cmpHeadLen = writeCmpHead(outFile, tempHeadName, bos, windowSize, bufferSize);

        if (bos instanceof SeparateOutputStream && ((SeparateOutputStream) bos).getCount() != 1) {
            bos.flush();
            bos.close();
            throw new SeparateException(
                    "First part of this archive does not have enough space to contain the file structure",
                    ((SeparateOutputStream) bos).getCumulativeLength());
        }

        if (bundle != null) step.setValue(bundle.getString("compressing"));

        long bodyCrc32 = writeBody(outFile, bos, inputStreams, windowSize, bufferSize);

        bos.flush();
        bos.close();

        byte[] fullBytes = Bytes.longToBytes(bodyCrc32);
        byte[] crc32Checksum = new byte[4];
        System.arraycopy(fullBytes, 4, crc32Checksum, 0, 4);

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
        writeInfoToFirstFile(raf);
        raf.close();
    }

    protected void setPartialInfo() {
        byte[] block = new byte[12];  // This is not a standard extra block
        // Structure:
        // blocks count: 4 bytes
        // length of the whole archive: 8 bytes
        extraFields.add(0, block);  // To make sure this is the first block, which is because when compresses
        // partially, this block should be in the first archive part.
    }

    protected abstract void writeMapToStream(OutputStream headBos, Deque<File> mainList) throws IOException;

    /**
     * Sets up the compression algorithm.
     *
     * @param alg the abbreviation of the newly-set compression algorithm.
     */
    @Override
    public void setAlgorithm(String alg) {
        this.alg = alg;
    }

    /**
     * Sets the compression level.
     *
     * @param cmpLevel the compression level.
     */
    @Override
    public void setCmpLevel(int cmpLevel) {
        this.cmpLevel = cmpLevel;
    }

    /**
     * Sets up the maximum length of each archive file. 0 if do not separate.
     *
     * @param partSize the maximum length of each archive file
     */
    @Override
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
    @Override
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
    @Override
    public void setThreads(int threads) {
        this.threads = threads;
    }

    /**
     * Interrupts the current running packing progress.
     */
    @Override
    public void interrupt() {
        this.isInterrupted = true;
    }

    /**
     * Add annotation to the package.
     *
     * @param annotation the node contains annotation text and info.
     */
    @Override
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

    public abstract static class IndexNode {
        protected final String name;
        /**
         * The {@code File} file.
         */
        protected final File file;
        /**
         * Whether this IndexNode represents a directory.
         */
        protected boolean isDir;
        /**
         * The start and end position of children of this IndexNode in the uncompressed context map.
         */
        protected int[] childrenRange;

        public IndexNode(String name, File file) {
            this.name = name;
            this.file = file;
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
         * Sets up the children {@code IndexNode}'s beginning and stop position in the total {@code IndexNode} list.
         *
         * @param begin the children's beginning position in node list.
         * @param stop  the children's stop position in node list.
         */
        public void setChildrenRange(int begin, int stop) {
            childrenRange = new int[]{begin, stop};
        }

        /**
         * Returns whether the file represented by this {@code IndexNode} is a directory.
         *
         * @return {@code true} if the file represented by this {@code IndexNode} is a directory.
         */
        public boolean isDir() {
            return isDir;
        }

        public abstract byte[] toByteArray() throws UnsupportedEncodingException;
    }
}

