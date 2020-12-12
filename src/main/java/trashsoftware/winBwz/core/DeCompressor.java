package trashsoftware.winBwz.core;

import trashsoftware.winBwz.core.fastLzz.FastLzzDecompressor;
import trashsoftware.winBwz.packer.UnPacker;

import java.io.OutputStream;

/**
 * A decoder that uncompress a file or an input stream into an output stream.
 * <p>
 * The {@code DeCompressor} interface is typically launched by a {@code UnPacker}.
 *
 * @author zbh
 * @see trashsoftware.winBwz.core.lzz2.LZZ2DeCompressor
 * @see trashsoftware.winBwz.core.bwz.BWZDeCompressor
 * @see FastLzzDecompressor
 * @since 0.5
 */
public interface DeCompressor {

    /**
     * Uncompress the data into <code>out</code>.
     *
     * @param out the target output stream.
     * @throws Exception if any error occurs.
     */
    void uncompress(OutputStream out) throws Exception;

    /**
     * Sets up the parent {@code UnPacker}, which launched this {@code DeCompressor}.
     *
     * @param unPacker parent {@code UnPacker} which launched this {@code DeCompressor}.
     */
    void setUnPacker(UnPacker unPacker);

    /**
     * Deletes all temp files created by this {@code DeCompressor}.
     */
    void deleteCache();

    /**
     * Sets up the thread number used for decompression.
     *
     * @param threads the thread number used for decompression.
     */
    void setThreads(int threads);
}
