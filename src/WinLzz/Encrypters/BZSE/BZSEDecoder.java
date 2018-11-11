package WinLzz.Encrypters.BZSE;

/**
 * Decryption algorithm of {@code BZSEEncoder}.
 * <p>
 * "BZSE" stands for Block-wise Zbh Symmetric Encryption.
 *
 * @author zbh
 * @since 0.8
 */
class BZSEDecoder {

    private byte[][] matrix = new byte[4][];

    private ZseKeyGenerator keyGenerator;

    /**
     * Creates a new {@code BZSEDecoder} instance.
     *
     * @param encText  the encoded text
     * @param password the password
     */
    BZSEDecoder(byte[] encText, String password) {
        byte[] temp;
        if (encText.length == 16) {
            temp = encText;
        } else {
            temp = new byte[16];
            System.arraycopy(encText, 0, temp, 0, encText.length);
        }
        for (int i = 0; i < 4; i++) {
            matrix[i] = new byte[4];
            System.arraycopy(temp, i << 2, matrix[i], 0, 4);
        }
        this.keyGenerator = new ZseKeyGenerator(password.getBytes());
    }

    private void columnPermutation(int t) {
        int[] permutation = keyGenerator.getPermutation(t);
        byte[][] newMatrix = new byte[4][4];
        for (int i = 0; i < 4; i++) {
            int col = permutation[i];
            for (int j = 0; j < 4; j++) newMatrix[j][col] = matrix[j][i];
        }
        matrix = newMatrix;
    }

    private void swapRow() {
        for (int i = 1; i < 4; i++)
            for (int j = 0; j < i; j++) {
                byte temp = matrix[0][i];
                for (int k = 0; k < 3; k++) matrix[k][i] = matrix[k + 1][i];
                matrix[3][i] = temp;
            }
    }

    private void byteReplace() {
        byte[][] md5Matrix = keyGenerator.getMd5Matrix();
        for (int i = 0; i < 4; i++) for (int j = 0; j < 4; j++) matrix[i][j] -= md5Matrix[i][j];
    }

    private void rShiftXor() {
        for (int i = 0; i < 4; i++) {
            byte pivot = matrix[i][3];
            for (int j = 3; j >= 1; j--) matrix[i][j] = (byte) (matrix[i][j - 1] ^ pivot);
            matrix[i][0] = pivot;
        }
    }

    private void xor(int round) {
        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 4; j++) matrix[i][j] = (byte) (matrix[i][j] ^ keyGenerator.getXorAt(round, i, j));
    }

    /**
     * Returns the decoded text.
     *
     * @return the decoded text
     */
    byte[] decode() {
        for (int round = 15; round >= 0; round--) {
            if ((round & 3) == 0) columnPermutation(round & 3);
            xor(round);
            rShiftXor();
            swapRow();
            byteReplace();
        }
        byte[] result = new byte[16];
        for (int i = 0; i < 4; i++) System.arraycopy(matrix[i], 0, result, i << 2, 4);
        return result;
    }
}
