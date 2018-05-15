package WinLzz.GraphicUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import WinLzz.ResourcesPack.Languages.LanguageLoader;

public class RegularFileNode {

    private File file;
    private boolean isDir;
    private LanguageLoader lanLoader;
    private boolean isRoot;

    public RegularFileNode(File file, LanguageLoader lanLoader) {
        this.file = file;
        this.isDir = file.isDirectory();
        this.lanLoader = lanLoader;
        isRoot = file.getAbsolutePath().length() <= 3;
    }

    public boolean isRoot() {
        return isRoot;
    }

    public File getFile() {
        return file;
    }

    @SuppressWarnings("all")
    public String getType() {
        if (isRoot) return lanLoader.get(26);
        else if (isDir) {
            return lanLoader.get(25);
        } else {
            String t = getExtension();
            if (t.length() > 0) t += " ";
            return t + lanLoader.get(24);
        }
    }

    public String getName() {
        String name = file.getName();
        if (name.length() == 0) return file.getAbsolutePath();
        else return name;
    }

    @SuppressWarnings("all")
    public String getLastModified() {
        if (isRoot) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date date = new Date(file.lastModified());
        return sdf.format(date);
    }

    public ReadableSize getSize() {
        return new ReadableSize(file);
    }

    public String getFullPath() {
        return file.getAbsolutePath();
    }

    public String getExtension() {
        String name = file.getAbsolutePath();
        if (name.contains(".")) return name.substring(name.lastIndexOf(".") + 1);
        else return "";
    }
}


