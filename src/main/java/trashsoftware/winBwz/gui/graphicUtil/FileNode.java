package trashsoftware.winBwz.gui.graphicUtil;

import trashsoftware.winBwz.packer.CatalogNode;
import trashsoftware.winBwz.packer.PzCatalogNode;
import trashsoftware.winBwz.utility.Util;

import java.io.File;
import java.util.ResourceBundle;

/**
 * A node {@code Object} that represents a file stored in a pz archive, used to show the information of a file on
 * the GUI.
 *
 * @author zbh
 * @since 0.4
 */
public class FileNode {

    private final CatalogNode cn;
    private final String name;
    private final boolean isDir;
    private final ResourceBundle bundle;

    /**
     * Creates a new {@code FileNode} instance.
     *
     * @param cn     the {@code ContextNode} represents this file inside the archive
     * @param bundle the text displaying object
     */
    public FileNode(CatalogNode cn, ResourceBundle bundle) {
        this.cn = cn;
        this.bundle = bundle;
        String fullName = cn.getPath();
        this.name = !fullName.contains(File.separator) ?
                fullName : fullName.substring(fullName.lastIndexOf(File.separator) + 1);
        this.isDir = cn.isDir();
    }

    /**
     * Returns the name of the file.
     *
     * @return the name of the file
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the type of the file.
     *
     * @return the type of the file
     */
    public String getType() {
        if (isDir) {
            return bundle.getString("folder");
        } else {
            String t = "";
            String oName = name;
            if (oName.contains(".")) t = oName.substring(oName.lastIndexOf(".") + 1) + " ";
            return t + bundle.getString("file");
        }
    }

    /**
     * Returns the size of the file, in a human-friendly form.
     *
     * @return the size of the file, in a human-friendly form
     */
    public String getSize() {
        if (isDir) {
            return "";
        } else {
            return Util.sizeToReadable(cn.getSize());
        }
    }

    /**
     * Returns the {@code ContextNode} represents the file of this {@code FileNode}.
     *
     * @return the {@code ContextNode} represents the file of this {@code FileNode}
     */
    public CatalogNode getContextNode() {
        return cn;
    }
}
