package ZSE;

import java.io.*;

public class ZSEFileDecoder {

    private FileInputStream fis;

    private String password;

    public ZSEFileDecoder(String inFile, String password) throws Exception {
        this.password = password;
        this.fis = new FileInputStream(inFile);
    }

    public void Decode(OutputStream out) throws IOException {
        int read;
        byte[] block = new byte[ZSEFileEncoder.blockSize];
        while ((read = fis.read(block)) != -1) {
            byte[] validBlock;
            if (read == ZSEFileEncoder.blockSize) {
                validBlock = block;
            } else {
                validBlock = new byte[read];
                System.arraycopy(block, 0, validBlock, 0, read);
            }
            ZSEDecoder decoder = new ZSEDecoder(validBlock, password);
            out.write(decoder.Decode());
        }
        fis.close();
    }
}
