package trashsoftware.winBwz.packer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class CatalogNode {

    protected final String path;
    /**
     * The {@code CatalogNode} which has this {@code CatalogNode} as a first-level child, in file system field.
     */
    protected final CatalogNode parent;
    protected final List<CatalogNode> children = new ArrayList<>();

    public CatalogNode(String path, CatalogNode parent) {
        this.path = path;
        this.parent = parent;
    }

    /**
     * Returns the relative path of the file represents by this {@code ContextNode}, related to the root directory.
     *
     * @return the relative path of the file represents by this {@code ContextNode}.
     */
    public String getPath() {
        return path;
    }

    /**
     * Returns the parent {@code ContextNode}.
     *
     * @return the parent {@code ContextNode}.
     */
    public CatalogNode getParent() {
        return parent;
    }

    /**
     * Adds a {@code ContextNode} instance as a child of this {@code ContextNode}.
     *
     * @param child the {@code ContextNode} that will be add to this {@code ContextNode} as a child.
     */
    public void addChild(CatalogNode child) {
        children.add(child);
    }

    /**
     * Returns the name of the file represents by this {@code ContextNode}.
     *
     * @return the name of the file represents by this {@code ContextNode}.
     */
    public String getName() {
        if (path.contains(File.separator)) return path.substring(path.lastIndexOf(File.separator) + 1);
        else return path;
    }

    public List<CatalogNode> getChildren() {
        return children;
    }

    public abstract boolean isDir();

    public abstract long getSize();

    public long getCmpSize() {
        return 0;
    }

    public boolean hasCmpSize() {
        return false;
    }
}
