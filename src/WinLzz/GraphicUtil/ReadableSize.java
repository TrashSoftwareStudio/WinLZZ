package WinLzz.GraphicUtil;

import WinLzz.Utility.Util;

import java.io.File;

public class ReadableSize implements Comparable<ReadableSize> {

    private long size;

    ReadableSize(File file) {
        if (file.isFile()) size = file.length();
    }

    @Override
    public String toString() {
        if (size == 0) return "";
        else return Util.sizeToReadable(size);
    }

    @Override
    public int compareTo(ReadableSize o) {
        return Long.compare(size, o.size);
    }
}
