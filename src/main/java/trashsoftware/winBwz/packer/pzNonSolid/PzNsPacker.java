package trashsoftware.winBwz.packer.pzNonSolid;

import trashsoftware.winBwz.core.Compressor;
import trashsoftware.winBwz.core.bwz.BWZCompressor;
import trashsoftware.winBwz.core.deflate.DeflateCompressor;
import trashsoftware.winBwz.core.fastLzz.FastLzzCompressor;
import trashsoftware.winBwz.core.lzz2.LZZ2Compressor;
import trashsoftware.winBwz.encrypters.Encipher;
import trashsoftware.winBwz.encrypters.bzse.BZSEStreamEncoder;
import trashsoftware.winBwz.encrypters.zse.ZSEFileEncoder;
import trashsoftware.winBwz.packer.pz.PzPacker;
import trashsoftware.winBwz.packer.pz.RootFile;
import trashsoftware.winBwz.utility.Bytes;
import trashsoftware.winBwz.utility.SeparateOutputStream;
import trashsoftware.winBwz.utility.Util;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class PzNsPacker extends PzPacker {

    private final List<long[]> compressedPosLen = new ArrayList<>();

    /**
     * Creates a new non solid {@code PzNsPacker} instance.
     * <p>
     * All input root files should be under a same path.
     *
     * @param inFiles the input root files.
     */
    public PzNsPacker(File[] inFiles) {
        super(inFiles);
    }

    @Override
    protected int getSignature() {
        return SIGNATURE_NS;
    }

    @Override
    protected long writeBody(String outFile,
                             OutputStream bos,
                             Deque<File> inputStreams,
                             int windowSize,
                             int bufferSize) throws Exception {
        long diff = compressedLength;
        String encName = outFile + ".enc";
        for (File file : inputStreams) {
            long startPos = compressedLength - diff;
            long fileLen = file.length();
            this.file.setValue(file.getAbsolutePath());
            InputStream fis = new FileInputStream(file);
            if (windowSize == 0) {
                if (encryptLevel == 0) {
                    Util.fileTruncate(fis, bos, fileLen);
                    compressedPosLen.add(new long[]{startPos, fileLen});
                    compressedLength += fileLen;
                } else {
                    Encipher encipher;
                    switch (encryption) {
                        case "zse":
                            encipher = new ZSEFileEncoder(fis, password);
                            break;
                        case "bzse":
                            encipher = new BZSEStreamEncoder(fis, password);
                            break;
                        default:
                            throw new NoSuchAlgorithmException("No Such Encoding Algorithm");
                    }
                    if (bundle != null) step.setValue(bundle.getString("encrypting"));
                    progress.set(1);
                    percentage.setValue("0.0");
                    encipher.setParent(this, fileLen);
                    encipher.encrypt(bos);
                    long encLen = encipher.encryptedLength();
                    compressedPosLen.add(new long[]{startPos, encLen});
                    compressedLength += encLen;
                }
            } else if (fileLen != 0) {
                Compressor mainCompressor;
                switch (alg) {
                    case "lzz2":
                        mainCompressor = new LZZ2Compressor(fis, windowSize, bufferSize, fileLen);
                        break;
                    case "fastLzz":
                        mainCompressor = new FastLzzCompressor(fis, windowSize, bufferSize, fileLen);
                        break;
                    case "bwz":
                        mainCompressor = new BWZCompressor(fis, windowSize);
                        break;
                    case "deflate":
                        mainCompressor = new DeflateCompressor(fis, cmpLevel, fileLen);
                        break;
                    default:
                        throw new NoSuchAlgorithmException("No such algorithm");
                }
                mainCompressor.setPacker(this);
                mainCompressor.setCompressionLevel(cmpLevel);
                mainCompressor.setThreads(threads);
                if (encryptLevel == 0) {
                    mainCompressor.compress(bos);
                    long cmpLen = mainCompressor.getCompressedSize();
                    compressedPosLen.add(new long[]{startPos, cmpLen});
                    compressedLength += cmpLen;
                } else {
                    OutputStream encFos = new FileOutputStream(encName);
                    mainCompressor.compress(encFos);
                    encFos.flush();
                    encFos.close();

                    InputStream encMainIs;
                    Encipher encipher;
                    switch (encryption) {
                        case "zse":
                            encMainIs = new FileInputStream(encName);
                            encipher = new ZSEFileEncoder(encMainIs, password);
                            break;
                        case "bzse":
                            encMainIs = new BufferedInputStream(new FileInputStream(encName));
                            encipher = new BZSEStreamEncoder(encMainIs, password);
                            break;
                        default:
                            throw new NoSuchAlgorithmException("No Such Encoding Algorithm");
                    }
                    if (bundle != null) step.setValue(bundle.getString("encrypting"));
                    progress.set(1);
                    percentage.setValue("0.0");
                    encipher.setParent(this, mainCompressor.getCompressedSize());
                    encipher.encrypt(bos);
                    encMainIs.close();
                    Util.deleteFile(encName);

                    long encLen = encipher.encryptedLength();
                    compressedPosLen.add(new long[]{startPos, encLen});
                    compressedLength += encLen;
                }
            } else {
                compressedPosLen.add(new long[]{startPos, 0});
            }
            fis.close();
        }
        return 0;
    }

    @Override
    protected void writeInfoToFirstFile(RandomAccessFile rafOfFirst) throws IOException {
//        rafOfFirst.seek(fileStructurePos);
    }

    @Override
    public void build() {
        RootFile rf = new RootFile(inFiles);
        IndexNodeNs rootNode = new IndexNodeNs("", rf);
        fileCount = 1;
        indexNodes.add(rootNode);
        buildIndexTree(rf, rootNode);
        totalOrigLengthWrapper.set(totalLength);
    }

    private void generateFileList(Deque<File> mainList) {
        for (IndexNode in : indexNodes) {
            if (!in.isDir()) mainList.addLast(in.getFile());
        }
    }

    private void updateFileStructure() {
        int fileIndex = 0;
        for (IndexNode indexNode : indexNodes) {
            IndexNodeNs inn = (IndexNodeNs) indexNode;
            if (!inn.isDir()) {
                long[] posSize = compressedPosLen.get(fileIndex++);
                inn.setCmpSize(posSize[0] + fileStructurePos, posSize[1]);
            }
        }
//        System.out.println(fileIndex + " " + compressedPosLen.size());
//        System.out.println(indexNodes.size());
    }

    @Override
    protected void writeMapToStream(OutputStream headBos, Deque<File> mainList) throws IOException {
        for (IndexNode in : indexNodes) {
            byte[] array = in.toByteArray();
            contextCrc.update(array);
            headBos.write(array);
//            if (!in.isDir()) mainList.addLast(in.getFile());
        }
    }

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
        generateFileList(inputStreams);

//        System.out.println(contextCrc.getValue());

        fileStructurePos = compressedLength;
//        System.out.println(compressedLength);

//        long cmpHeadLen = writeCmpHead(outFile, tempHeadName, bos, windowSize, bufferSize);
        String mainOutName = outFile + ".main";
        OutputStream mainOut = new FileOutputStream(mainOutName);

//        if (bos instanceof SeparateOutputStream && ((SeparateOutputStream) bos).getCount() != 1) {
//            bos.flush();
//            bos.close();
//            throw new SeparateException(
//                    "First part of this archive does not have enough space to contain the file structure",
//                    ((SeparateOutputStream) bos).getCumulativeLength());
//        }

        if (bundle != null) step.setValue(bundle.getString("compressing"));

//        long len = compressedLength;
        writeBody(outFile, mainOut, inputStreams, windowSize, bufferSize);
//        long mainLen = compressedLength - len;

        mainOut.flush();
        mainOut.close();

//        byte[] fullBytes = Bytes.longToBytes(bodyCrc32);
//        byte[] crc32Checksum = new byte[4];
//        System.arraycopy(fullBytes, 4, crc32Checksum, 0, 4);

        if (bundle != null) step.setValue(bundle.getString("integrating"));

//        System.out.println(contextCrc.getValue());
        updateFileStructure();
        writeCmpMapToTemp(tempHeadName, inputStreams);
//        System.out.println(contextCrc.getValue());
        long cmpHeadLen = writeCmpHead(outFile, tempHeadName, bos, windowSize, bufferSize);
//        System.out.println(cmpHeadLen);
//        System.out.println(inputStreams);
//        System.out.println(indexNodes);

        long contextCrcValue = contextCrc.getValue();
        byte[] contextCrcArray = Arrays.copyOfRange(Bytes.longToBytes(contextCrcValue), 4, 8);

//        System.out.println(mainLen + " " + new File(mainOutName).length());
        InputStream mainIn = new FileInputStream(mainOutName);
        Util.fileTruncate(mainIn, bos, new File(mainOutName).length());
        mainIn.close();
        Util.deleteFile(mainOutName);
//        System.out.println(mainLenW);

        bos.flush();
        bos.close();

        RandomAccessFile raf;
        if (bos instanceof SeparateOutputStream) {
            raf = new RandomAccessFile(((SeparateOutputStream) bos).getFirstName(), "rw");
        } else raf = new RandomAccessFile(outFile, "rw");

        raf.seek(9);
        int currentTimeInt = Bytes.getCurrentTimeInInt();  // Creation time,
        // rounded to second. Starting from 1999-12-31 19:00
        raf.writeInt(currentTimeInt);
        raf.write(contextCrcArray);
        raf.write(new byte[4]);
        raf.writeInt((int) cmpHeadLen);

        if (bos instanceof SeparateOutputStream) {
            // If the archive is partially
            int fCount = ((SeparateOutputStream) bos).getCount();
            raf.seek(27);  // Seek to the first extra field block.
            raf.writeInt(fCount);
            raf.writeLong(((SeparateOutputStream) bos).getCumulativeLength());
        }
        writeInfoToFirstFile(raf);
//        if (compressedLength != raf.length()) {
//            System.out.println("Failed! " + compressedLength + " " + raf.length());
//        }
        raf.close();
    }

    private void buildIndexTree(File file, IndexNodeNs currentNode) {
        if (isInterrupted) return;
        if (file.isDirectory()) {
            this.file.setValue(file.getAbsolutePath() + "\\");
            File[] sub = file.listFiles();

            int currentCount = fileCount;
            assert sub != null;
            IndexNodeNs[] tempArr = new IndexNodeNs[sub.length];
            int arrIndex = 0;
            for (File f : sub) {
                IndexNodeNs in = new IndexNodeNs(f.getName(), f);
                tempArr[arrIndex++] = in;
                indexNodes.add(in);
                fileCount += 1;
            }
            currentNode.setChildrenRange(currentCount, fileCount);
            for (int i = 0; i < sub.length; i++) if (!sub[i].isDirectory()) buildIndexTree(sub[i], tempArr[i]);
            for (int i = 0; i < sub.length; i++) if (sub[i].isDirectory()) buildIndexTree(sub[i], tempArr[i]);
        } else {
            long fileLen = file.length();
            totalLength += fileLen;
            currentNode.setSize(fileLen);
        }
    }

    static class IndexNodeNs extends IndexNode {

        private long posInArchive;
        private long cmpSize;
        private long size;

        public IndexNodeNs(String name, File file) {
            super(name, file);
        }

        @Override
        public byte[] toByteArray() throws UnsupportedEncodingException {
            byte[] nameBytes = Bytes.stringEncode(name);
            int len = nameBytes.length;
            if (len > 255) throw new UnsupportedEncodingException();
            byte[] result;
            if (isDir) {
                result = new byte[len + 18];
                result[0] = 0;
                result[1] = (byte) len;
                System.arraycopy(Bytes.longToBytes(childrenRange[0]), 0, result, 2, 8);
                System.arraycopy(Bytes.longToBytes(childrenRange[1]), 0, result, 10, 8);
                System.arraycopy(nameBytes, 0, result, 18, nameBytes.length);
            } else {
                result = new byte[len + 26];
                result[0] = 1;
                result[1] = (byte) len;
                // 2 reserved for position in archive
                // 10 reserved for compressed length in archive
                System.arraycopy(Bytes.longToBytes(posInArchive), 0, result, 2, 8);
                System.arraycopy(Bytes.longToBytes(cmpSize), 0, result, 10, 8);

                System.arraycopy(Bytes.longToBytes(size), 0, result, 18, 8);
                System.arraycopy(nameBytes, 0, result, 26, nameBytes.length);
            }
            return result;
        }

        public void setCmpSize(long posInArchive, long cmpSize) {
            this.posInArchive = posInArchive;
            this.cmpSize = cmpSize;
        }

        public long getSize() {
            return size;
        }

        /**
         * Sets the original size of this file
         *
         * @param size the original size
         */
        public void setSize(long size) {
            this.size = size;
            this.isDir = false;
        }

        @Override
        public String toString() {
            return "IndexNodeNs{" +
                    "name=" + name +
                    ", isDir=" + isDir +
                    ", posInArchive=" + posInArchive +
                    ", cmpSize=" + cmpSize +
                    ", size=" + size +
                    '}';
        }
    }
}
