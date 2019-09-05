package trashsoftware.win_bwz.interfaces;

import trashsoftware.win_bwz.packer.Packer;

import java.io.OutputStream;

/**
 * A Coder that compresses a file or an input stream into an output stream.
 * <p>
 * The {@code Compressor} interface is typically launched by a {@code Packer}.
 *
 * @author zbh
 * @see trashsoftware.win_bwz.lzz2.LZZ2Compressor
 * @see trashsoftware.win_bwz.bwz.BWZCompressor
 * @since 0.5
 */
public interface Compressor {

    /**
     * Compresses the data into <code>out</code>.
     *
     * @param out the target output stream.
     * @throws Exception if any error occurs.
     */
    void compress(OutputStream out) throws Exception;

    /**
     * Sets up the parent {@code Packer}, which launched this {@code Compressor}.
     *
     * @param parent parent {@code Packer} which launched this {@code Compressor}.
     */
    void setParent(Packer parent);

    /**
     * Sets up the thread number used for compress.
     *
     * @param threads the thread number.
     */
    void setThreads(int threads);

    /**
     * Sets up the compression level.
     * <p>
     * Mostly, higher compression level results a higher compression ratio, but slower speed.
     *
     * @param level the compression level.
     */
    void setCompressionLevel(int level);

    /**
     * Returns the file length after compression.
     *
     * @return the file length after compression.
     */
    long getCompressedSize();
}
