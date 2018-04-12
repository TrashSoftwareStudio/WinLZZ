package Packer;

import java.util.ArrayList;
import java.util.Arrays;

public class ContextNode {

    private String path;

    private ArrayList<ContextNode> children = new ArrayList<>();

    private boolean isDir = true;

    private int[] location;

    private ContextNode parent;

    ContextNode(String path) {
        this.path = path;
    }

    public void setParent(ContextNode parent) {
        this.parent = parent;
    }

    public ContextNode getParent() {
        return parent;
    }

    void addChild(ContextNode child) {
        children.add(child);
    }

    void setLocation(int start, int end) {
        this.location = new int[]{start, end};
        isDir = false;
    }

    public int[] getLocation() {
        return location;
    }

    public boolean isDir() {
        return isDir;
    }

    public String getPath() {
        return path;
    }

    public ArrayList<ContextNode> getChildren() {
        return children;
    }

    @Override
    public String toString() {
        if (isDir) return "Dir(" + path + ": " + children + ")";
        else return "File(" + path + ": " + Arrays.toString(location) + ")";
    }
}
