package WinLzz.GraphicUtil;

import java.io.File;

public class SpecialFile extends File {


    /**
     * Creates a new instance of a SpecialFile object.
     *
     * @param path the absolute path of this SpecialFile object.
     */
    public SpecialFile(String path) {
        super(path);
    }


    /**
     * Returns a readable String object.
     * <p>
     * This method overrides the "toString" method of the File class. It returns only the native name of this file.
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
