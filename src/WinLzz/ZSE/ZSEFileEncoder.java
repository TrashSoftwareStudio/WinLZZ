package WinLzz.ZSE;

import WinLzz.Utility.MultipleInputStream;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A ZSE-based encoder that encodes a file.
 *
 * @author zbh
 * @see ZSEEncoder
 * @since 0.4
 */
public class ZSEFileEncoder {

    static final int blockSize = 8192;

    private InputStream fis;

    private String password;

    private long encodeLength;

    /**
     * Creates a new {ZSEFileEncoder} instance.
     * <p>
     * This constructor takes a file as input.
     *
     * @param inFile   the name of the file to be encoded
     * @param password the password
     * @throws IOException if the file is not readable
     */
    public ZSEFileEncoder(String inFile, String password) throws IOException {
        fis = new FileInputStream(inFile);
        this.password = password;
    }

    /**
     * Creates a new {ZSEFileEncoder} instance.
     * <p>
     * This constructor takes a {MultipleInputStream} as input.
     *
     * @param in       the input stream to be encoded
     * @param password the password
     */
    public ZSEFileEncoder(MultipleInputStream in, String password) {
        fis = in;
        this.password = password;
    }

    /**
     * Returns the MD5 checksum of the <code>password</code>.
     *
     * @param password the password to be calculated
     * @return the MD5 checksum
     */
    public static byte[] md5PlainCode(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return md.digest(password.getBytes("utf-8"));
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new RuntimeException("This cannot happen");
        }
    }

    /**
     * Outputs the encoded data into <code>out</code>.
     *
     * @param out the output stream to write data
     * @throws IOException if the output stream is not writable
     */
    public void Encode(OutputStream out) throws IOException {
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
    public long getEncodeLength() {
        return encodeLength;
    }
}
