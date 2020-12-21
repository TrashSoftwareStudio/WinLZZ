package trashsoftware.winBwz.packer.pz;

import java.io.File;

/**
 * A special kind of file which marks the root directory of a archive file.
 * <p>
 * This kind of file does not exist on the disk.
 *
 * @author zbh
 * @since 0.7
 */
public class RootFile extends File {

    private final File[] children;

    /**
     * Creates a new {@code RootFile} instance.
     *
     * @param children children if this {@code RootFile}.
     */
    public RootFile(File[] children) {
        super("");
        this.children = children;
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    @Override
    public File[] listFiles() {
        return children;
    }
}
