package GUI;

import Packer.ContextNode;
import Utility.Util;

import java.io.File;

public class FileNode {

    private ContextNode cn;

    private String name;

    private boolean isDir;

    FileNode(ContextNode cn) {
        this.cn = cn;
        String fullName = cn.getPath();
        if (!fullName.contains(File.separator)) {
            this.name = fullName;
        } else {
            this.name = fullName.substring(fullName.lastIndexOf(File.separator) + 1);
        }
        this.isDir = cn.isDir();
    }

    private static String displayName(String name) {
        return name;
    }

    public String getName() {
        return displayName(name);
    }

    public String getType() {
        if (isDir) {
            return "文件夹";
        } else {
            String t = "";
            String oName = displayName(name);
            if (oName.contains(".")) {
                t = oName.substring(oName.lastIndexOf(".") + 1) + " ";
            }
            return t + "文件";
        }
    }

    public String getSize() {
        if (isDir) {
            return "";
        } else {
            int[] location = cn.getLocation();
            return Util.sizeToReadable(location[1] - location[0]);
        }
    }

    ContextNode getContextNode() {
        return cn;
    }
}
