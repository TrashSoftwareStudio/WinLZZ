package WinLzz.ZSE;

import java.io.*;

/**
 * The decoder of a ZSE-encoded file.
 *
 * @author zbh
 * @see ZSEDecoder
 * @since 0.4
 */
public class ZSEFileDecoder {

    private InputStream fis;

    private String password;

    private long length;

    private boolean nativeStream;

    /**
     * Creates a new {ZSEFileDecoder} instance.
     * <p>
     * This constructor takes a file as input.
     *
     * @param inFile   the file to be decoded
     * @param password the password
     * @throws FileNotFoundException if the file does not exist
     */
    public ZSEFileDecoder(String inFile, String password) throws FileNotFoundException {
        this.password = password;
        this.fis = new FileInputStream(inFile);
        this.length = new File(inFile).length();
        nativeStream = true;
    }

    /**
     * Creates a new {ZSEFileDecoder} instance.
     * <p>
     * This constructor takes an {@code InputStream} as input.
     *
     * @param fis      the input stream to be decoded
     * @param password the password
     */
    public ZSEFileDecoder(InputStream fis, String password, long readLength) {
        this.password = password;
        this.fis = fis;
        this.length = readLength;
    }

    /**
     * Outputs the decoded data into <code>out</code>.
     *
     * @param out the output stream to be written
     * @throws IOException if <code>out</code> is not writable
     */
    public void Decode(OutputStream out) throws IOException {
        int read;
        byte[] block = new byte[ZSEFileEncoder.blockSize];
        while ((read = fis.read(block)) != -1) {
            byte[] validBlock;
            if (read == ZSEFileEncoder.blockSize) {
                validBlock = block;
                length -= read;
            } else {
                validBlock = new byte[(int) length];
                System.arraycopy(block, 0, validBlock, 0, read);
            }
            ZSEDecoder decoder = new ZSEDecoder(validBlock, password);
            out.write(decoder.Decode());
        }
        if (nativeStream) fis.close();
    }
}
