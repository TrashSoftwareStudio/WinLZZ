package trashsoftware.win_bwz.GraphicUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import trashsoftware.win_bwz.ResourcesPack.Languages.LanguageLoader;

/**
 * A node {@code Object} that represents an actual file in the file system, used to show the information of a file on
 * the GUI.
 *
 * @author zbh
 * @since 0.7
 */
public class RegularFileNode {

    private File file;
    private boolean isDir;
    private LanguageLoader lanLoader;
    private boolean isRoot;

    /**
     * Creates a new {@code RegularFileNode} instance.
     *
     * @param file      the file
     * @param lanLoader the text displaying object
     */
    public RegularFileNode(File file, LanguageLoader lanLoader) {
        this.file = file;
        this.isDir = file.isDirectory();
        this.lanLoader = lanLoader;
        isRoot = file.getAbsolutePath().length() <= 3;
    }

    /**
     * Return whether this {@code RegularFileNode} represents the Windows system root directory.
     *
     * @return {@code true} if this {@code RegularFileNode} represents the Windows system root directory,
     * {@code false} otherwise
     */
    public boolean isRoot() {
        return isRoot;
    }

    /**
     * Returns the file.
     *
     * @return the file
     */
    public File getFile() {
        return file;
    }

    /**
     * Returns the file type to be displayed on the screen.
     *
     * @return the file type to be displayed on the screen
     */
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

    /**
     * Returns the name of the file.
     *
     * @return the name of the file
     */
    public String getName() {
        String name = file.getName();
        if (name.length() == 0) return file.getAbsolutePath();
        else return name;
    }

    /**
     * Returns the {@code String} represents the last modified time of this file.
     *
     * @return the {@code String} represents the last modified time of this file
     */
    @SuppressWarnings("all")
    public String getLastModified() {
        if (isRoot) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date date = new Date(file.lastModified());
        return sdf.format(date);
    }

    /**
     * Returns the @code String} represents the size of this file.
     *
     * @return the @code String} represents the size of this file
     */
    public ReadableSize getSize() {
        return new ReadableSize(file);
    }

    /**
     * Returns the full path of the file.
     *
     * @return the full path of the file
     */
    public String getFullPath() {
        return file.getAbsolutePath();
    }

    /**
     * Returns the suffix of ths file.
     *
     * @return the suffix of this file
     */
    public String getExtension() {
        String name = file.getAbsolutePath();
        if (name.contains(".")) return name.substring(name.lastIndexOf(".") + 1);
        else return "";
    }
}


