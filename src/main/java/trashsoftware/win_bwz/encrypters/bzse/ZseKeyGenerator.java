package trashsoftware.win_bwz.encrypters.bzse;

import trashsoftware.win_bwz.utility.Security;

import java.security.NoSuchAlgorithmException;

/**
 * Key generator of a {@code BZSEEncoder}.
 *
 * @author zbh
 * @since 0.8
 */
class ZseKeyGenerator {

    private byte[][] md5Matrix = new byte[4][];

    /**
     * Creates a new {@code ZseKeyGenerator} instance.
     *
     * @param password the byte text of the password
     */
    ZseKeyGenerator(byte[] password) {
        try {
            byte[] md5 = Security.secureHashing(password, "MD5");
            for (int i = 0; i < 4; i++) {
                md5Matrix[i] = new byte[4];
                System.arraycopy(md5, i << 2, md5Matrix[i], 0, 4);
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the permutation of the given round.
     *
     * @param t the count of the calling time, usually equals to {@code BZSEEncoder.round} / 4.
     * @return the permutation of the password
     */
    int[] getPermutation(int t) {
        byte[] row = md5Matrix[t];
        int[] result = new int[4];
        for (int i = 0; i < 4; i++) {
            int count = 0;
            for (int j = 0; j < 4; j++)
                if (row[j] <= row[i])
                    if (row[j] != row[i] || j < i)
                        count++;
            result[i] = count;
        }
        return result;
    }

    /**
     * Returns a value to be used for encryption.
     *
     * @param round the round of encryption
     * @param r     the row index
     * @param c     the column index
     * @return an xor value
     */
    int getXorAt(int round, int r, int c) {
        return md5Matrix[r][c] ^ ((round << 4) | (~round));
    }

    /**
     * Returns the 4x4 matrix which contains the MD5 checksum of the password.
     *
     * @return the 4x4 matrix which contains the MD5 checksum of the password
     */
    byte[][] getMd5Matrix() {
        return md5Matrix;
    }
}
