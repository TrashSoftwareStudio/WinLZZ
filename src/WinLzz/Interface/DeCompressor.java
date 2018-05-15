package WinLzz.Interface;

import WinLzz.Packer.UnPacker;

import java.io.OutputStream;

/**
 * A decoder that uncompress a file or an input stream into an output stream.
 * <p>
 * The {@code DeCompressor} interface is typically launched by a {@code UnPacker}.
 *
 * @author zbh
 * @see WinLzz.LZZ2.LZZ2DeCompressor
 * @see WinLzz.BWZ.BWZDeCompressor
 * @since 0.5
 */
public interface DeCompressor {

    /**
     * Uncompress the data into <code>out</code>.
     *
     * @param out the target output stream.
     * @throws Exception if any error occurs.
     */
    void Uncompress(OutputStream out) throws Exception;

    /**
     * Sets up the parent {@code UnPacker}, which launched this {@code DeCompressor}.
     *
     * @param parent parent {@code UnPacker} which launched this {@code DeCompressor}.
     */
    void setParent(UnPacker parent);

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
