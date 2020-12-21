package trashsoftware.winBwz.packer.pz;

import trashsoftware.winBwz.packer.CatalogNode;

import java.util.Arrays;

/**
 * A tree-kind data structure node, representing a file or a directory.
 * <p>
 * A {@code ContextNode} represents a file if it is a leaf node, otherwise it is an internal node.
 *
 * @author zbh
 * @since 0.4
 */
public class PzSolidCatalogNode extends CatalogNode {

    private boolean isDir = true;
    private long[] location;

    /**
     * Creates a new {@code ContextNode} instance.
     *
     * @param path   the relative path of the file represented by this {@code ContextNode} in the archive.
     * @param parent the parent dir
     */
    PzSolidCatalogNode(String path, PzSolidCatalogNode parent) {
        super(path, parent);
    }

    /**
     * Sets up the starting and ending position of the file represents by this {@code ContextNode}.
     *
     * @param start the start position.
     * @param end   the end position.
     */
    void setLocation(long start, long end) {
        this.location = new long[]{start, end};
        isDir = false;
    }

    /**
     * Returns the starting and ending position of the file represents by this {@code ContextNode}.
     *
     * @return int array of {start position, end position}.
     */
    public long[] getLocation() {
        return location;
    }

    /**
     * Returns whether the file represents by this {@code ContextNode} is a directory.
     *
     * @return {@code true} if the file represents by this {@code ContextNode} is a directory.
     */
    public boolean isDir() {
        return isDir;
    }

    @Override
    public long getSize() {
        long[] location = getLocation();
        return location[1] - location[0];
    }

    @Override
    public String toString() {
        if (isDir) return "Dir(" + path + ": " + children + ")";
        else return "File(" + path + ": " + Arrays.toString(location) + ")";
    }
}
