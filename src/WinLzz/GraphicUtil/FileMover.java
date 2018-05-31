package WinLzz.GraphicUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * An object that represents a file, and can be moved, or copied to another existing directory.
 * <p>
 * The cut mode can only be pasted once.
 *
 * @author zbh
 * @since 0.7.3
 */
public class FileMover {

    private File file;

    private boolean isCopy;

    /**
     * Creates a new {@code FileMover} instance.
     *
     * @param file   the source file
     * @param isCopy {@code true} for the copy mode, {@code false} for the cut mode
     */
    public FileMover(File file, boolean isCopy) {
        this.file = file;
        this.isCopy = isCopy;
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
     * Returns the destination file to be pasted.
     *
     * @param parentDir the destination directory {@code File}
     * @return the destination file to be pasted
     */
    public File getDestFile(File parentDir) {
        return new File(String.format("%s%s%s", parentDir.getAbsolutePath(), File.separator, file.getName()));
    }

    /**
     * Pastes the file as <code>destFile</code> and returns whether the paste was successful.
     *
     * @param destFile the destination file
     * @return {@code true} if succeed, {@code false} otherwise
     */
    @SuppressWarnings("all")
    public boolean pasteTo(File destFile) {
        if (isCopy) return copyTo(destFile);
        else return moveTo(destFile);
    }

    /**
     * Returns {@code true} if this {@code FileMover} works in copy mode, {@code false} otherwise.
     *
     * @return {@code true} if this {@code FileMover} works in copy mode, {@code false} otherwise
     */
    public boolean isCopy() {
        return isCopy;
    }

    private boolean moveTo(File destFile) {
        boolean b = file.renameTo(destFile);
        file = destFile;
        return b;
    }

    private boolean copyTo(File destFile) {
        try {
            FileChannel in = new FileInputStream(file).getChannel();
            FileChannel out = new FileOutputStream(destFile).getChannel();
            in.transferTo(0, in.size(), out);
            in.close();
            out.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
