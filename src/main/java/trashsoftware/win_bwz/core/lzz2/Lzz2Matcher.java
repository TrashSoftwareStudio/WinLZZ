package trashsoftware.win_bwz.core.lzz2;

import trashsoftware.win_bwz.utility.FileInputBufferArray;

import java.io.IOException;

public interface Lzz2Matcher {

    void fillSlider(long prevPos, long currentPos, FileInputBufferArray inputBufferArray) throws IOException;

    /**
     * Returns the skip length.
     * <p>
     * This method should set up the distance and length
     *
     * @param inputBufferArray the input buffer
     * @param position         the current position
     * @return the skip bytes
     * @throws IOException if the input buffer array is not readable
     */
    int search(FileInputBufferArray inputBufferArray, long position) throws IOException;

    int getLength();

    int getDistance();
}
