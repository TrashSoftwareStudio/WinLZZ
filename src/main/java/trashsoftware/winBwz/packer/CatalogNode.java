package trashsoftware.winBwz.packer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class CatalogNode {

    protected final String path;
    /**
     * The {@code CatalogNode} which has this {@code CatalogNode} as a first-level child, in file system field.
     */
    protected CatalogNode parent;
    protected final List<CatalogNode> children = new ArrayList<>();

    public CatalogNode(String path) {
        this.path = path;
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
     * Sets up the parent {@code ContextNode}.
     *
     * @param parent the parent {@code ContextNode}.
     */
    public void setParent(CatalogNode parent) {
        this.parent = parent;
    }

    /**
     * Adds a {@code ContextNode} instance as a child of this {@code ContextNode}.
     *
     * @param child the {@code ContextNode} that will be add to this {@code ContextNode} as a child.
     */
    void addChild(CatalogNode child) {
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
}
