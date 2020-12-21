package trashsoftware.winBwz.packer.pzNonSolid;

import trashsoftware.winBwz.packer.CatalogNode;

public class PzNsCatalogNode extends CatalogNode {

    private long posInArchive;
    private long cmpLength;
    private long origLength;
    private boolean isDir = true;

    public PzNsCatalogNode(String path, CatalogNode parent) {
        super(path, parent);
    }

    public void setUp(long posInArchive, long cmpLength, long origLength) {
        this.posInArchive = posInArchive;
        this.cmpLength = cmpLength;
        this.origLength = origLength;
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

    @Override
    public long getCmpSize() {
        return cmpLength;
    }

    @Override
    public boolean hasCmpSize() {
        return true;
    }
}
