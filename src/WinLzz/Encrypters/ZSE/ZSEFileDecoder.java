package WinLzz.Encrypters.ZSE;

import WinLzz.Interface.Decipher;
import WinLzz.Packer.UnPacker;

import java.io.*;

/**
 * The decoder of a ZSE-encoded file.
 *
 * @author zbh
 * @see WinLzz.Interface.Decipher
 * @see ZSEDecoder
 * @since 0.4
 */
public class ZSEFileDecoder implements Decipher {

    private InputStream fis;

    private String password;

    private long length;

    /**
     * Creates a new {ZSEFileDecoder} instance.
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
    @Override
    public void decrypt(OutputStream out) throws IOException {
        int read;
        byte[] block = new byte[ZSEFileEncoder.blockSize];
        while (length >= ZSEFileEncoder.blockSize) {
            read = fis.read(block);
            if (read != ZSEFileEncoder.blockSize) throw new IOException("Stream error");
            length -= read;
            ZSEDecoder decoder = new ZSEDecoder(block, password);
            out.write(decoder.Decode());
        }
        byte[] lastBlock = new byte[(int) length];
        read = fis.read(lastBlock);
        length -= read;
        if (length != 0) throw new IOException("Stream error");
        ZSEDecoder decoder = new ZSEDecoder(lastBlock, password);
        out.write(decoder.Decode());
    }

    @Override
    public void setParent(UnPacker parent) {

    }
}
