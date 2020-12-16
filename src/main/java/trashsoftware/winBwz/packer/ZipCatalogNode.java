package trashsoftware.winBwz.packer;

public class ZipCatalogNode extends CatalogNode {

    private final boolean isDir;

    public ZipCatalogNode(String path, boolean isDir) {
        super(path);

        this.isDir = isDir;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj.getClass() != this.getClass()) return false;

        ZipCatalogNode zcn = (ZipCatalogNode) obj;
        return path.equals(zcn.path);
    }

    @Override
    public boolean isDir() {
        return isDir;
    }

    @Override
    public long getSize() {
        return 0;
    }
}
