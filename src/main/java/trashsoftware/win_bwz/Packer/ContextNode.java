package trashsoftware.win_bwz.Packer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * A tree-kind data structure node, representing a file or a directory.
 * <p>
 * A {@code ContextNode} represents a file if it is a leaf node, otherwise it is an internal node.
 *
 * @author zbh
 * @since 0.4
 */
public class ContextNode {

    private String path;
    private ArrayList<ContextNode> children = new ArrayList<>();
    private boolean isDir = true;
    private long[] location;

    /**
     * The {@code ContextNode} which has this {@code ContextNode} as a first-level child, in file system field.
     */
    private ContextNode parent;

    /**
     * Creates a new {@code ContextNode} instance.
     *
     * @param path the relative path of the file represented by this {@code ContextNode} in the archive.
     */
    ContextNode(String path) {
        this.path = path;
    }

    /**
     * Sets up the parent {@code ContextNode}.
     *
     * @param parent the parent {@code ContextNode}.
     */
    public void setParent(ContextNode parent) {
        this.parent = parent;
    }

    /**
     * Returns the parent {@code ContextNode}.
     *
     * @return the parent {@code ContextNode}.
     */
    public ContextNode getParent() {
        return parent;
    }

    /**
     * Adds a {@code ContextNode} instance as a child of this {@code ContextNode}.
     *
     * @param child the {@code ContextNode} that will be add to this {@code ContextNode} as a child.
     */
    void addChild(ContextNode child) {
        children.add(child);
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

    /**
     * Returns the relative path of the file represents by this {@code ContextNode}, related to the root directory.
     *
     * @return the relative path of the file represents by this {@code ContextNode}.
     */
    public String getPath() {
        return path;
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

    /**
     * Returns an {@code ArrayList} containing all children of this {@code ContextNode}.
     *
     * @return an {@code ArrayList} containing all children of this {@code ContextNode}.
     */
    public ArrayList<ContextNode> getChildren() {
        return children;
    }

    @Override
    public String toString() {
        if (isDir) return "Dir(" + path + ": " + children + ")";
        else return "File(" + path + ": " + Arrays.toString(location) + ")";
    }
}
