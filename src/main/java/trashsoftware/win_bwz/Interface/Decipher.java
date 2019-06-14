package trashsoftware.win_bwz.Interface;

import trashsoftware.win_bwz.Packer.UnPacker;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A decoder that decrypts a stream into another stream.
 *
 * @author zbh
 * @see trashsoftware.win_bwz.Encrypters.ZSE.ZSEFileDecoder
 * @see trashsoftware.win_bwz.Encrypters.BZSE.BZSEStreamDecoder
 * @since 0.7.4
 */
public interface Decipher {

    /**
     * Decrypts the content into <code>out</code>.
     *
     * @param out the stream to be written
     * @throws IOException if any IO error occurs
     */
    void decrypt(OutputStream out) throws IOException;

    /**
     * Sets up the parent {@code UnPacker}, which launched this {@code Decipher} instance.
     *
     * @param parent parent {@code UnPacker} which launched this {@code Decipher} instance
     */
    void setParent(UnPacker parent);
}
