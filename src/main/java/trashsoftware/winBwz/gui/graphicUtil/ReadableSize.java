package trashsoftware.winBwz.gui.graphicUtil;

import trashsoftware.winBwz.utility.Util;

import java.io.File;
import java.util.ResourceBundle;

/**
 * A representation of the size of a file, which is both comparable and easy to read.
 *
 * @author zbh
 * @see java.lang.Comparable
 * @since 0.7.0
 */
public class ReadableSize implements Comparable<ReadableSize> {

    private long size;

    /**
     * Creates a new {@code ReadableSize} instance.
     *
     * @param file the file
     */
    ReadableSize(File file) {
        if (file.isFile()) size = file.length();
    }

    /**
     * Returns a more readable text of the size.
     *
     * @return the size string
     */
    @Override
    public String toString() {
        if (size == 0) return "";
        else return Util.sizeToReadable(size);
    }

    /**
     * Returns the long comparision result between the size of this {@code ReadableSize} and the other one.
     *
     * @param o the other {@code ReadableSize} instance to be compare with this {@code ReadableSize} instance
     * @return the comparison result
     * @see Long
     */
    @Override
    public int compareTo(ReadableSize o) {
        return Long.compare(size, o.size);
    }

    public String getSizeInByte(ResourceBundle bundle) {
        return Util.numToReadable2Decimal(size) + " " + bundle.getString("byte");
    }
}
