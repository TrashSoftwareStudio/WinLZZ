package trashsoftware.winBwz.packer.pzNonSolid;

import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.ReadOnlyLongWrapper;
import trashsoftware.winBwz.core.Constants;
import trashsoftware.winBwz.core.DeCompressor;
import trashsoftware.winBwz.core.bwz.BWZDeCompressor;
import trashsoftware.winBwz.core.deflate.DeflateDeCompressor;
import trashsoftware.winBwz.core.fastLzz.FastLzzDecompressor;
import trashsoftware.winBwz.core.lzz2.LZZ2DeCompressor;
import trashsoftware.winBwz.packer.CatalogNode;
import trashsoftware.winBwz.packer.pz.PzUnPacker;
import trashsoftware.winBwz.utility.Bytes;
import trashsoftware.winBwz.utility.Security;
import trashsoftware.winBwz.utility.Util;

import java.io.*;
import java.nio.channels.FileChannel;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.zip.CRC32;

public class PzNsUnPacker extends PzUnPacker {

    private final List<IndexNodeUnpNs> indexNodes = new ArrayList<>();
    private final String testTempName;
    private final ReadOnlyLongWrapper secondaryProgress = new ReadOnlyLongWrapper();
    private final ReadOnlyLongWrapper secondaryTotalProgress = new ReadOnlyLongWrapper();
    private PzNsCatalogNode rootNode;

    private long totalLenOfProcessedFiles;

    /**
     * Creates a new UnPacker instance, with <code>packName</code> as the input archive name.
     *
     * @param packName       the name/path of the input archive file.
     * @param resourceBundle resources bundle
     */
    public PzNsUnPacker(String packName, ResourceBundle resourceBundle) {
        super(packName, resourceBundle);

        testTempName = packName + ".pt";
    }

    @Override
    public void readFileStructure() throws Exception {
        unCompressMap();
        readStructure();
        deleteTemp();

        rootNode = new PzNsCatalogNode(indexNodes.get(0).getName(), null);
        buildContextTree(rootNode, indexNodes.get(0));
        interpretExtraField();
    }

    private void buildContextTree(PzNsCatalogNode node, IndexNodeUnpNs inu) {
        if (inu.isDir()) {
            InuDir inuDir = (InuDir) inu;
            List<IndexNodeUnpNs> children = indexNodes.subList(inuDir.childrenStart,
                    inuDir.childrenEnd);
            String path = node.getPath();
            for (IndexNodeUnpNs n : children) {
                PzNsCatalogNode cn = new PzNsCatalogNode(path + File.separator + n.getName(), node);
                node.addChild(cn);
            }
            for (int i = 0; i < children.size(); i++)
                buildContextTree((PzNsCatalogNode) node.getChildren().get(i), children.get(i));
        } else {
            InuFile inuFile = (InuFile) inu;
//            System.out.println(inuFile.getName() + inuFile.cmpLength);
            node.setUp(inuFile.posInArchive, inuFile.cmpLength, inuFile.origLength, inuFile.crc32);
        }
    }

    @Override
    protected void readStructure() throws IOException {
        BufferedInputStream mapInputStream = new BufferedInputStream(new FileInputStream(mapName));
        CRC32 contextChecker = new CRC32();

        byte[] flag = new byte[2];
        byte[] buffer4 = new byte[4];
        byte[] buffer8 = new byte[8];
        byte[] crcBuffer = new byte[8];
        while (mapInputStream.read(flag) == 2) {
            contextChecker.update(flag);
            boolean isDir = (flag[0] & 0xff) == 0;
            int nameLen = flag[1] & 0xff;

            IndexNodeUnpNs inu;

            if (isDir) {
                if (mapInputStream.read(buffer8) != 8) throw new IOException("Error occurs while reading");
                contextChecker.update(buffer8);
                long childrenStart = Bytes.bytesToLong(buffer8);
                if (mapInputStream.read(buffer8) != 8) throw new IOException("Error occurs while reading");
                contextChecker.update(buffer8);
                long childrenEnd = Bytes.bytesToLong(buffer8);

                inu = new InuDir((int) childrenStart, (int) childrenEnd);
                this.dirCount++;
            } else {
                if (mapInputStream.read(buffer8) != 8) throw new IOException("Error occurs while reading");
                contextChecker.update(buffer8);
                long posInArchive = Bytes.bytesToLong(buffer8);
                if (mapInputStream.read(buffer8) != 8) throw new IOException("Error occurs while reading");
                contextChecker.update(buffer8);
                long cmpLen = Bytes.bytesToLong(buffer8);
                if (mapInputStream.read(buffer4) != 4) throw new IOException("Error occurs while reading");
                contextChecker.update(buffer4);
                System.arraycopy(buffer4, 0, crcBuffer, 4, 4);
                long crc32 = Bytes.bytesToLong(crcBuffer);
                if (mapInputStream.read(buffer8) != 8) throw new IOException("Error occurs while reading");
                contextChecker.update(buffer8);
                long origLen = Bytes.bytesToLong(buffer8);

                inu = new InuFile(posInArchive, cmpLen, origLen, crc32);
                this.fileCount++;
                this.origSize += origLen;
            }
            byte[] nameBytes = new byte[nameLen];
            if (mapInputStream.read(nameBytes) != nameLen) throw new IOException("Error occurs while reading");
            contextChecker.update(nameBytes);
            String name = Bytes.stringDecode(nameBytes);
            inu.setName(name);

            indexNodes.add(inu);
        }
        mapInputStream.close();
        totalProgress.set(origSize);

        if (contextChecker.getValue() != crc32Context) {
//            throw new ChecksumDoesNotMatchException("Context damaged");
            System.err.println("CRC32 Checksum does not match");
        }
    }

    private void uncompressSingle(PzNsCatalogNode node,
                                  String outName,
                                  FileChannel fc,
                                  InputStream stream,
                                  UnpTimerTask utt)
            throws Exception {
        fc.position(node.getPosInArchive() + mainStartPos);
//        System.out.println(node.getPath() + " " + node.getSize() + " " + node.getCmpSize());
        currentFile.set(node.getPath());
        secondaryTotalProgress.set(node.getSize());
//        System.out.println(node.getName() + " " + node.getPosInArchive() + " " + node.getCmpSize());
        if (encryptLevel != 0) {

        } else if (node.getSize() == 0) {
            File f = new File(outName);
            if (!f.createNewFile()) {
                System.out.println("Failed to create " + outName);
            }
        } else if (windowSize != 0) {
            String tempOut = outName + ".temp";
            OutputStream temp = new FileOutputStream(tempOut);
            Util.fileTruncate(stream, temp, node.getCmpSize());
            temp.flush();
            temp.close();

            DeCompressor deCompressor;
            switch (alg) {
                case "bwz":
                    deCompressor = new BWZDeCompressor(tempOut, windowSize, 0);
                    break;
                case "lzz2":
                    deCompressor = new LZZ2DeCompressor(tempOut, windowSize);
                    break;
                case "fastLzz":
                    deCompressor = new FastLzzDecompressor(tempOut, windowSize);
                    break;
                case "deflate":
                    deCompressor = new DeflateDeCompressor(tempOut);
                    break;
                default:
                    throw new NoSuchAlgorithmException("No such algorithm");
            }
            utt.setDeCompressor(deCompressor);
            OutputStream out = new FileOutputStream(outName);
            deCompressor.uncompress(out);
            out.flush();
            out.close();

            Util.deleteFile(tempOut);
        } else {
            OutputStream out = new FileOutputStream(outName);

            Util.fileTruncate(stream, out, node.getCmpSize());

            out.flush();
            out.close();
        }
        totalLenOfProcessedFiles += node.getSize();
    }

    private boolean testEntry(PzNsCatalogNode node, FileChannel fc, InputStream stream, UnpTimerTask utt)
            throws Exception {
        if (node.isDir()) {
            for (CatalogNode child : node.getChildren()) {
                if (!testEntry((PzNsCatalogNode) child, fc, stream, utt)) return false;
            }
            return true;
        } else {
            long crc = node.getCrc32();
            uncompressSingle(node, testTempName, fc, stream, utt);
            long realCrc = Security.generateCRC32(testTempName);
            Util.deleteFile(testTempName);
            return crc == realCrc;
        }
    }

    @Override
    protected boolean isUnCompressed() {
        return false;
    }

    @Override
    public boolean testPack() {
        step.setValue(bundle.getString("testing"));
        try {
            bis.close();
        } catch (IOException e) {
            System.out.println("stream already closed");
        }

        System.out.println();
        UnpTimerTask utt = new DeCompTimerTask();
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(utt, 0, 1000 / Constants.GUI_UPDATES_PER_S);
        totalProgress.set(origSize);
        progress.set(0);
        totalLenOfProcessedFiles = 0;

        try {
            if (isSeparated) {
//            bis = SeparateInputStream.createNew();
            } else {
                FileInputStream fis = new FileInputStream(packName);
                FileChannel fc = fis.getChannel();

                boolean res = testEntry(rootNode, fc, fis, utt);
                fc.close();
                return res;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            timer.cancel();
            Util.deleteFile(testTempName);
        }
        return false;
    }

    @Override
    public void unCompressFrom(String targetDir, CatalogNode node) throws Exception {
        String dirOffset;
        if (!node.getPath().contains(File.separator)) dirOffset = "";
        else dirOffset = node.getPath().substring(0, node.getPath().lastIndexOf(File.separator));
        step.setValue(bundle.getString("uncIng"));
        totalProgress.set(origSizeFrom((PzNsCatalogNode) node));
        progress.set(0);
        totalLenOfProcessedFiles = 0;

        try {
            bis.close();
        } catch (IOException e) {
            System.out.println("stream already closed");
        }

        UnpTimerTask utt = new DeCompTimerTask();
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(utt, 0, 1000 / Constants.GUI_UPDATES_PER_S);

        try {
            FileInputStream fis = new FileInputStream(packName);
            FileChannel fc = fis.getChannel();
            traversalExtract(targetDir, (PzNsCatalogNode) node, dirOffset, fc, fis, utt);
            fc.close();
        } finally {
            timer.cancel();
        }
    }

    private long origSizeFrom(PzNsCatalogNode node) {
        if (node.isDir()) {
            long total = 0;
            for (CatalogNode cn : node.getChildren()) {
                total += origSizeFrom((PzNsCatalogNode) cn);
            }
            return total;
        } else {
            return node.getSize();
        }
    }

    private void traversalExtract(String targetDir,
                                  PzNsCatalogNode node,
                                  String dirOffset,
                                  FileChannel fc,
                                  InputStream stream,
                                  UnpTimerTask utt) throws Exception {
        String path = targetDir + File.separator + node.getPath().substring(dirOffset.length());
        File f = new File(path);
        if (node.isDir()) {
            if (!f.exists()) {
                if (!f.mkdirs()) System.out.println("Failed to create directory " + f.getAbsolutePath());
            }
            for (CatalogNode cn : node.getChildren()) {
                traversalExtract(targetDir, (PzNsCatalogNode) cn, dirOffset, fc, stream, utt);
            }
        } else {
            uncompressSingle(node, path, fc, stream, utt);
        }
    }

    @Override
    public CatalogNode getRootNode() {
        return rootNode;
    }

    @Override
    public boolean hasSecondaryProgress() {
        return true;
    }

    @Override
    public ReadOnlyLongProperty secondaryProgressProperty() {
        return secondaryProgress;
    }

    @Override
    public ReadOnlyLongProperty secondaryTotalProgressProperty() {
        return secondaryTotalProgress;
    }

    abstract static class IndexNodeUnpNs {

        private String name;

        IndexNodeUnpNs() {
        }

        String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        abstract boolean isDir();
    }

    static class InuDir extends IndexNodeUnpNs {

        private final int childrenStart;
        private final int childrenEnd;

        InuDir(int childrenStart, int childrenEnd) {
            super();

            this.childrenStart = childrenStart;
            this.childrenEnd = childrenEnd;
        }

        @Override
        boolean isDir() {
            return true;
        }
    }

    static class InuFile extends IndexNodeUnpNs {

        private final long posInArchive;
        private final long cmpLength;
        private final long origLength;
        private final long crc32;

        InuFile(long posInArchive, long cmpLength, long origLength, long crc32) {
            super();

            this.posInArchive = posInArchive;
            this.cmpLength = cmpLength;
            this.origLength = origLength;
            this.crc32 = crc32;
        }

        @Override
        boolean isDir() {
            return false;
        }
    }

    private class DeCompTimerTask extends UnpTimerTask {
        @Override
        public void run() {
            update();
        }

        private synchronized void update() {
            accumulator++;
            if (deCompressor == null) return;
            long secPos = deCompressor.getUncompressedLength();
            long position = secPos + totalLenOfProcessedFiles;
            progress.set(position);
            secondaryProgress.set(secPos);
            if (accumulator % Constants.GUI_UPDATES_PER_S == 0) {
                updateTimer(position);


            }
        }
    }
}
