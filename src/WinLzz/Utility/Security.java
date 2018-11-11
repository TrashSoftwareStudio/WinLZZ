package WinLzz.Utility;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.zip.CRC32;

/**
 * A class that consists of security methods.
 *
 * @since 0.7.4
 */
public abstract class Security {

    /**
     * Generates the CRC32 checksum of the file which has name or path <code>fileName</code>.
     *
     * @param fileName the name or path of the file to be calculated
     * @return the CRC32 checksum of the file which has name or path <code>fileName</code>
     * @throws IOException if the file which has name or path <code>fileName</code> is not readable
     */
    public static long generateCRC32(String fileName) throws IOException {
        CRC32 crc = new CRC32();
        FileChannel fc = new FileInputStream(fileName).getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        int read;
        while ((read = fc.read(buffer)) > 0) {
            buffer.flip();
            crc.update(buffer.array(), 0, read);
            buffer.clear();
        }
        fc.close();
        return crc.getValue();
    }

    /**
     * Returns a byte array contains the hash result of the password.
     *
     * @param password the string password to be calculated
     * @param alg      the name of the hash algorithm, not case-sensitive
     * @return the secure hash result
     * @throws NoSuchAlgorithmException if there is no such algorithm named <code>alg</code>
     */
    public static byte[] secureHashing(String password, String alg) throws NoSuchAlgorithmException {
        return secureHashing(password.getBytes(), alg);
    }

    /**
     * Returns a byte array contains the hash result of the password.
     *
     * @param password the password to be calculated
     * @param alg      the name of the hash algorithm, not case-sensitive
     * @return the secure hash result
     * @throws NoSuchAlgorithmException if there is no such algorithm named <code>alg</code>
     */
    public static byte[] secureHashing(byte[] password, String alg) throws NoSuchAlgorithmException {
        String upperCase = alg.toUpperCase();
        MessageDigest md = MessageDigest.getInstance(upperCase);
        md.update(password);
        return md.digest();
    }

    /**
     * Returns a 8-byte random sequence.
     *
     * @return 8-byte random sequence
     */
    public static byte[] generateRandomSequence() {
        double rand1 = Math.random();
        double rand2 = Math.random();
        long l1 = (long) (rand1 * Long.MAX_VALUE);
        long l2 = (long) (rand2 * Long.MAX_VALUE);
        byte[] b1 = Arrays.copyOfRange(Bytes.longToBytes(l1), 2, 6);
        byte[] b2 = Arrays.copyOfRange(Bytes.longToBytes(l2), 2, 6);
        byte[] result = new byte[8];
        System.arraycopy(b1, 0, result, 0, 4);
        System.arraycopy(b2, 0, result, 4, 4);
        return result;
    }
}
