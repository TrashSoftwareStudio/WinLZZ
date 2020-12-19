package trashsoftware.winBwz.encrypters.zse;

import trashsoftware.winBwz.encrypters.Encipher;
import trashsoftware.winBwz.packer.pz.PzPacker;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A ZSE-based encoder that encodes a file.
 *
 * @author zbh
 * @see ZSEEncoder
 * @since 0.4
 */
public class ZSEFileEncoder implements Encipher {

    static final int blockSize = 8192;

    private InputStream fis;

    private String password;

    private long encodeLength;

    /**
     * Creates a new {ZSEFileEncoder} instance.
     * <p>
     * This constructor takes a {InputStream} as input.
     *
     * @param in       the input stream to be encoded
     * @param password the password
     */
    public ZSEFileEncoder(InputStream in, String password) {
        fis = in;
        this.password = password;
    }

    /**
     * Outputs the encoded data into <code>out</code>.
     *
     * @param out the output stream to write data
     * @throws IOException if the output stream is not writable
     */
    @Override
    public void encrypt(OutputStream out) throws IOException {
        int read;
        byte[] block = new byte[blockSize];
        while ((read = fis.read(block)) != -1) {
            encodeLength += read;
            byte[] validBlock;
            if (read == blockSize) {
                validBlock = block;
            } else {
                validBlock = new byte[read];
                System.arraycopy(block, 0, validBlock, 0, read);
            }
            ZSEEncoder encoder = new ZSEEncoder(validBlock, password);
            out.write(encoder.Encode());
        }
        fis.close();
    }

    /**
     * Returns the length of the data after encoding.
     *
     * @return the length of the data after encoding
     */
    @Override
    public long encryptedLength() {
        return encodeLength;
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
}
