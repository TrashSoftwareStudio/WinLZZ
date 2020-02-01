package trashsoftware.win_bwz.gui.graphicUtil;

import java.io.File;

/**
 * An extension of {@code File} object, which has a slightly different {@code toString} method.
 *
 * @author zbh
 * @see java.io.File
 * @since 0.7
 */
public class SpecialFile extends File {

    /**
     * Creates a new {@code SpecialFile} instance.
     *
     * @param path the absolute path of this {@code SpecialFile}
     */
    public SpecialFile(String path) {
        super(path);
    }

    /**
     * Returns a readable {@code String} object.
     * <p>
     * This method overrides the {@code toString} method of the File class.
     * It returns only the native name of this file.
     *
     * @return the native name of this file.
     */
    @Override
    public String toString() {
        String result = super.getName();
        if (result.length() == 0) {
            return super.toString();
        } else if (super.isDirectory()) {
            return result;
        } else {
            return null;
        }
    }
}
