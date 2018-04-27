package WinLzz.ZSE;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ZSEFileEncoder {

    static final int blockSize = 8192;

    private FileInputStream fis;

    private String password;

    private long encodeLength;

    public ZSEFileEncoder(String inFile, String password) throws IOException {
        fis = new FileInputStream(inFile);
        this.password = password;
    }

    public static byte[] md5PlainCode(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        return md.digest(password.getBytes("utf-8"));
    }

    public void Encode(OutputStream out) throws Exception {
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

    public long getEncodeLength() {
        return encodeLength;
    }
}
