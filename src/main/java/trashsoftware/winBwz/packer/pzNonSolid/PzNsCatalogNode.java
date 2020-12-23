package trashsoftware.winBwz.packer.pzNonSolid;

import trashsoftware.winBwz.packer.CatalogNode;

public class PzNsCatalogNode extends CatalogNode {

    private long posInArchive;
    private long cmpLength;
    private long origLength;
    private long crc32;
    private boolean isDir = true;

    public PzNsCatalogNode(String path, CatalogNode parent) {
        super(path, parent);
    }

    public void setUp(long posInArchive, long cmpLength, long origLength, long crc32) {
        this.posInArchive = posInArchive;
        this.cmpLength = cmpLength;
        this.origLength = origLength;
        this.crc32 = crc32;
        isDir = false;
    }

    @Override
    public boolean isDir() {
        return isDir;
    }

    @Override
    public long getSize() {
        return origLength;
    }

    public long getCrc32() {
        return crc32;
    }

    @Override
    public long getCmpSize() {
        return cmpLength;
    }

    @Override
    public boolean hasCmpSize() {
        return true;
    }

    public long getPosInArchive() {
        return posInArchive;
    }
}
