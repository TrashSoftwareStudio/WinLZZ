package WinLzz.GraphicUtil;

import WinLzz.Packer.ContextNode;
import WinLzz.ResourcesPack.Languages.LanguageLoader;
import WinLzz.Utility.Util;

import java.io.File;

public class FileNode {

    private ContextNode cn;
    private String name;
    private boolean isDir;
    private LanguageLoader languageLoader;

    public FileNode(ContextNode cn, LanguageLoader languageLoader) {
        this.cn = cn;
        this.languageLoader = languageLoader;
        String fullName = cn.getPath();
        this.name = !fullName.contains(File.separator) ?
                fullName : fullName.substring(fullName.lastIndexOf(File.separator) + 1);
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
            return languageLoader.get(25);
        } else {
            String t = "";
            String oName = displayName(name);
            if (oName.contains(".")) t = oName.substring(oName.lastIndexOf(".") + 1) + " ";
            return t + languageLoader.get(24);
        }
    }

    public String getSize() {
        if (isDir) {
            return "";
        } else {
            long[] location = cn.getLocation();
            return Util.sizeToReadable(location[1] - location[0]);
        }
    }

    public ContextNode getContextNode() {
        return cn;
    }
}
