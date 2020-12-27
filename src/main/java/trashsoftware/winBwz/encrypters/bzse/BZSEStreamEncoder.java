package trashsoftware.winBwz.encrypters.bzse;

import trashsoftware.winBwz.encrypters.Encipher;
import trashsoftware.winBwz.packer.pz.PzPacker;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An implementation of interface {@code Encipher} that uses BZSE algorithm.
 *
 * @author zbh
 * @see Encipher
 * @see BZSEEncoder
 * @since 0.8
 */
public class BZSEStreamEncoder implements Encipher {

    private final InputStream is;

    private final String password;

    private long length;

    /**
     * Creates a new {@code BZSEStreamEncoder} instance.
     *
     * @param input    the input stream to be encrypted
     * @param password the password
     */
    public BZSEStreamEncoder(InputStream input, String password) {
        this.is = input;
        this.password = password;
    }

    /**
     * Writes the encrypted content to the output stream <code>out</code>.
     *
     * @param out the output stream to be written
     * @throws IOException if any IO error occurs
     */
    @Override
    public void encrypt(OutputStream out) throws IOException {
        int read;
        int remain = 0;
        byte[] buffer = new byte[16];
        while ((read = is.read(buffer)) > 0) {
            length += 16;
            if (read != 16) remain = read;
            BZSEEncoder bzse = new BZSEEncoder(buffer, password);
            byte[] result = bzse.encode();
            out.write(result);
        }
        length += 1;
        out.write((byte) remain);
    }

    /**
     * Sets up the parent {@code Packer} instance.
     *
     * @param parent                 the parent {@code Packer} instance
     * @param lengthBeforeEncryption the stream length before encryption
     */
    @Override
    public void setParent(PzPacker parent, long lengthBeforeEncryption) {
    }

    @Override
    public long getInputSize() {
        return length;
    }

    @Override
    public long getOutputSize() {
        return length;
    }
}
