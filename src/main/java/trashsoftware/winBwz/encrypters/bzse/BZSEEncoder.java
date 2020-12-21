package trashsoftware.winBwz.encrypters.bzse;

/**
 * A symmetric encryption algorithm.
 * <p>
 * "BZSE" stands for Block-wise Zbh Symmetric Encryption.
 *
 * @author zbh
 * @since 0.8
 */
class BZSEEncoder {

    private byte[][] matrix = new byte[4][];

    private final ZseKeyGenerator keyGenerator;

    /**
     * Creates a new {@code BZSEEncoder} instance.
     *
     * @param text     the text to be encrypted
     * @param password the password
     */
    BZSEEncoder(byte[] text, String password) {
        byte[] temp;
        if (text.length == 16) {
            temp = text;
        } else {
            temp = new byte[16];
            System.arraycopy(text, 0, temp, 0, text.length);
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
            for (int j = 0; j < 4; j++) newMatrix[j][i] = matrix[j][col];
        }
        matrix = newMatrix;
    }

    private void byteReplace() {
        byte[][] md5Matrix = keyGenerator.getMd5Matrix();
        for (int i = 0; i < 4; i++) for (int j = 0; j < 4; j++) matrix[i][j] += md5Matrix[i][j];
    }

    private void swapRow() {
        for (int i = 1; i < 4; i++)
            for (int j = 0; j < i; j++) {
                byte temp = matrix[3][i];
                for (int k = 3; k > 0; k--) matrix[k][i] = matrix[k - 1][i];
                matrix[0][i] = temp;
            }
    }

    /**
     * Left shift every position by one and XOR it by the leftmost item in this row and itself.
     */
    private void lShiftXor() {
        for (int i = 0; i < 4; i++) {
            byte pivot = matrix[i][0];
            for (int j = 0; j < 3; j++) matrix[i][j] = (byte) (matrix[i][j + 1] ^ pivot);
            matrix[i][3] = pivot;
        }
    }

    private void xor(int round) {
        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 4; j++) matrix[i][j] = (byte) (matrix[i][j] ^ keyGenerator.getXorAt(round, i, j));
    }

    /**
     * Returns the encoded text.
     *
     * @return the encoded text
     */
    byte[] encode() {
        for (int round = 0; round < 16; round++) {
            byteReplace();
            swapRow();
            lShiftXor();
            xor(round);
            if ((round & 3) == 0) columnPermutation(round & 3);
        }
        byte[] result = new byte[16];
        for (int i = 0; i < 4; i++) System.arraycopy(matrix[i], 0, result, i << 2, 4);
        return result;
    }
}
