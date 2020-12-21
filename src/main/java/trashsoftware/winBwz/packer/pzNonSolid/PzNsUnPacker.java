package trashsoftware.winBwz.packer.pzNonSolid;

import trashsoftware.winBwz.packer.CatalogNode;
import trashsoftware.winBwz.packer.pz.PzSolidCatalogNode;
import trashsoftware.winBwz.packer.pz.PzUnPacker;
import trashsoftware.winBwz.utility.Bytes;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.zip.CRC32;

public class PzNsUnPacker extends PzUnPacker {

    private PzNsCatalogNode rootNode;
    private final List<IndexNodeUnpNs> indexNodes = new ArrayList<>();

    /**
     * Creates a new UnPacker instance, with <code>packName</code> as the input archive name.
     *
     * @param packName       the name/path of the input archive file.
     * @param resourceBundle resources bundle
     */
    public PzNsUnPacker(String packName, ResourceBundle resourceBundle) {
        super(packName, resourceBundle);
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
            node.setUp(inuFile.posInArchive, inuFile.cmpLength, inuFile.origLength);
        }
    }

    @Override
    protected void readStructure() throws IOException {
        BufferedInputStream mapInputStream = new BufferedInputStream(new FileInputStream(mapName));
        CRC32 contextChecker = new CRC32();

        byte[] flag = new byte[2];
        byte[] buffer8 = new byte[8];
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
                if (mapInputStream.read(buffer8) != 8) throw new IOException("Error occurs while reading");
                contextChecker.update(buffer8);
                long origLen = Bytes.bytesToLong(buffer8);

                inu = new InuFile(posInArchive, cmpLen, origLen);
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

    @Override
    protected boolean isUnCompressed() {
        return false;
    }

    @Override
    public boolean testPack() {
        return false;
    }

    @Override
    public void unCompressAll(String targetDir) throws Exception {

    }

    @Override
    public void unCompressFrom(String targetDir, CatalogNode node) throws Exception {

    }

    @Override
    public CatalogNode getRootNode() {
        return rootNode;
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

        InuFile(long posInArchive, long cmpLength, long origLength) {
            super();

            this.posInArchive = posInArchive;
            this.cmpLength = cmpLength;
            this.origLength = origLength;
        }

        @Override
        boolean isDir() {
            return false;
        }
    }
}
