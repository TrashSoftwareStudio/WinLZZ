package trashsoftware.winBwz.encrypters.bzse;

import trashsoftware.winBwz.encrypters.Encipher;
import trashsoftware.winBwz.packer.PzPacker;

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

    private InputStream is;

    private String password;

    private long length;

    private PzPacker parent;

    private long beforeLength;

    private int updateCount;

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
        double ratio = 1;
        if (parent != null) ratio = (double) beforeLength / parent.getTotalOrigSize();
        int read;
        int remain = 0;
        byte[] buffer = new byte[16];
        while ((read = is.read(buffer)) > 0) {
            if ((length & 0xffff) == 0 && parent != null) updateProgress(ratio);
            length += 16;
            if (read != 16) remain = read;
            BZSEEncoder bzse = new BZSEEncoder(buffer, password);
            byte[] result = bzse.encode();
            out.write(result);
        }
        length += 1;
        out.write((byte) remain);
    }

    private void updateProgress(double ratio) {
        parent.progress.set((long) (parent.progress.get() + 65536 / ratio));
        if (updateCount == 79) {
            double finished = (double) length / beforeLength;
            double rounded = (double) Math.round(finished * 1000) / 10;
            parent.percentage.set(String.valueOf(rounded));
            updateCount = 0;
        } else {
            updateCount += 1;
        }
    }

    /**
     * Returns the stream length after encryption.
     *
     * @return the stream length after encryption
     */
    @Override
    public long encryptedLength() {
        return length;
    }

    /**
     * Sets up the parent {@code Packer} instance.
     *
     * @param parent                 the parent {@code Packer} instance
     * @param lengthBeforeEncryption the stream length before encryption
     */
    @Override
    public void setParent(PzPacker parent, long lengthBeforeEncryption) {
        this.parent = parent;
        this.beforeLength = lengthBeforeEncryption;
    }
}
