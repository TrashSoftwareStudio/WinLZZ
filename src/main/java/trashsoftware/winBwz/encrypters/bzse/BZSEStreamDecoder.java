package trashsoftware.winBwz.encrypters.bzse;

import trashsoftware.winBwz.encrypters.Decipher;
import trashsoftware.winBwz.packer.pz.PzUnPacker;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An implementation of interface {@code Decipher} that uses BZSE algorithm.
 *
 * @author zbh
 * @see Decipher
 * @see BZSEDecoder
 * @since 0.8
 */
public class BZSEStreamDecoder implements Decipher {

    private final InputStream is;

    private final String password;

    private long length;

    private final long totalLength;

    /**
     * Creates a new {@code BZSEStreamDecoder} instance.
     *
     * @param is              the input stream to be decrypted
     * @param password        the password
     * @param encryptedLength the length of the encrypted text
     */
    public BZSEStreamDecoder(InputStream is, String password, long encryptedLength) {
        this.is = is;
        this.password = password;
        this.length = encryptedLength;
        this.totalLength = encryptedLength;
    }

    @Override
    public long getOutputSize() {
        return totalLength - length - 1;
    }

    /**
     * Writes the decrypted content to the output stream <code>out</code>.
     *
     * @param out the stream to be written
     * @throws IOException if any IO error occurs
     */
    @Override
    public void decrypt(OutputStream out) throws IOException {
        byte[] buffer = new byte[16];
        byte[] decoded = null;
        int read;
//        double ratio = 1;
//        if (parent != null) ratio = parent.getMainRatio();
        while (length > 1) {
//            if (parent != null && (length & 0xffff) == 1) updateInfo(ratio);
            read = is.read(buffer);
            if (read != 16) throw new IOException("Stream error");
            length -= 16;
            if (decoded != null) out.write(decoded);
            decoded = new BZSEDecoder(buffer, password).decode();
        }
        byte[] buffer1 = new byte[1];
        if (is.read(buffer1) != 1) throw new IOException("Stream error");
        int remain = buffer1[0];
        byte[] valid = new byte[remain];
        assert decoded != null;
        System.arraycopy(decoded, 0, valid, 0, remain);
        out.write(valid);
    }

//    private void updateInfo(double ratio) {
//        parent.progress.set((long) (parent.progress.get() + 65536 / ratio));
//        if (updateCount == 79) {
//            double finished = 1 - (double) length / totalLength;
//            double rounded = (double) Math.round(finished * 1000) / 10;
//            parent.percentage.set(String.valueOf(rounded));
//            updateCount = 0;
//        } else {
//            updateCount += 1;
//        }
//    }

    /**
     * Sets up the parent {@code UnPacker} which launched this {@code Decipher} instance.
     *
     * @param parent parent {@code UnPacker} which launched this {@code Decipher} instance
     */
    @Override
    public void setParent(PzUnPacker parent) {
    }
}
