package trashsoftware.winBwz.packer.zip;

import trashsoftware.winBwz.packer.CatalogNode;

import java.util.zip.ZipEntry;

public class ZipCatalogNode extends CatalogNode {

    private final ZipEntry entry;

    public ZipCatalogNode(String path, ZipCatalogNode parent, ZipEntry entry) {
        super(path, parent);

        this.entry = entry;
    }

    public ZipCatalogNode(String path, ZipCatalogNode parent) {
        this(path, parent, null);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj.getClass() != this.getClass()) return false;

        ZipCatalogNode zcn = (ZipCatalogNode) obj;
        return path.equals(zcn.path);
    }

    public ZipEntry getEntry() {
        return entry;
    }

    @Override
    public boolean isDir() {
        return entry == null || entry.isDirectory();
    }

    @Override
    public long getSize() {
        return entry.getSize();
    }

    @Override
    public boolean hasCmpSize() {
        return true;
    }

    @Override
    public long getCmpSize() {
        return entry.getCompressedSize();
    }
}
