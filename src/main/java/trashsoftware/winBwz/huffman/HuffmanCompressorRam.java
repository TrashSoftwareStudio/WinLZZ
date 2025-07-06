package trashsoftware.winBwz.huffman;

import trashsoftware.winBwz.utility.BitOutputStream;

import java.io.IOException;
import java.io.OutputStream;

public class HuffmanCompressorRam extends HuffmanCompressorRamBase {

    /**
     * Constructor of a new {@code HuffmanCompressor} instance.
     * <p>
     * Creates a new HuffmanCompressor which takes the "inFile" as the file to compress.
     *
     * @param content the byte content to compress.
     */
    public HuffmanCompressorRam(byte[] content) {
        super(content);
    }

    @Override
    protected void generateFreqMap() {
        HuffmanCompressor.addArrayToFreqMap(content, freqMap, content.length);
        inFileLength += content.length;

        lengthRemainder = inFileLength % 256;
    }

    @Override
    protected void compressText(int[] huffmanCode, int[] lengthCode, OutputStream fos) throws IOException {
        BitOutputStream fbo = new BitOutputStream(fos);

        for (byte b : content) {
            int v = b & 0xff;
            int len = lengthCode[v];
            int code = huffmanCode[v];
            if (len == 0) throw new RuntimeException();
            fbo.write(code, len);
        }

        // Deal with the last few bits.
        fbo.flush();
        compressedLength += fbo.getLength();
    }

    @Override
    public void SepCompress(OutputStream out) throws IOException {
        compressText(huffmanCode, lengthCode, out);
    }
}
