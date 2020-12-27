package trashsoftware.winBwz.encrypters;

import trashsoftware.winBwz.core.ProgressedStream;
import trashsoftware.winBwz.encrypters.bzse.BZSEStreamEncoder;
import trashsoftware.winBwz.encrypters.zse.ZSEFileEncoder;
import trashsoftware.winBwz.packer.pz.PzPacker;
import trashsoftware.winBwz.packer.pz.PzSolidPacker;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A coder that encrypts a stream into another stream.
 *
 * @author zbh
 * @see ZSEFileEncoder
 * @see BZSEStreamEncoder
 * @since 0.7.4
 */
public interface Encipher extends ProgressedStream {

    /**
     * Encrypts the stream.
     *
     * @param out the output stream to be written
     * @throws IOException if any IO error occurs
     */
    void encrypt(OutputStream out) throws IOException;

    /**
     * Sets up the parent {@code Packer} instance.
     *
     * @param parent                 the parent {@code Packer} instance
     * @param lengthBeforeEncryption the stream length before encryption
     */
    void setParent(PzPacker parent, long lengthBeforeEncryption);
}
