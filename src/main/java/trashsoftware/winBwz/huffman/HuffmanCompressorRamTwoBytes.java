package trashsoftware.winBwz.huffman;

import trashsoftware.winBwz.utility.BitOutputStream;

import java.io.IOException;
import java.io.OutputStream;

public class HuffmanCompressorRamTwoBytes extends HuffmanCompressorRamBase {

    public static final int END_SIG = 256;

    public HuffmanCompressorRamTwoBytes(byte[] content) {
        super(content);
    }

    @Override
    protected void generateFreqMap() {
        HuffmanCompressorTwoBytes.addArrayToFreqMap(content, freqMap, content.length);
        inFileLength += content.length;

        lengthRemainder = inFileLength % 256;
        freqMap[END_SIG] = 1;
    }

    @Override
    protected void compressText(int[] huffmanCode, int[] lengthCode, OutputStream fos) throws IOException {
        BitOutputStream fbo = new BitOutputStream(fos);

        for (int i = 0; i < content.length; i += 2) {
            int v = ((content[i] & 0xff) << 8) | (content[i + 1] & 0xff);
            int len = lengthCode[v];
            int code = huffmanCode[v];
            if (len == 0) throw new RuntimeException();
            fbo.write(code, len);
            
        }
        int endSigLen = lengthCode[END_SIG];
        int endSigCode = huffmanCode[END_SIG];
        if (endSigLen == 0) throw new RuntimeException();
        fbo.write(endSigCode, endSigLen);

        // Deal with the last few bits.
        fbo.flush();
        compressedLength += fbo.getLength();
    }

    @Override
    public void SepCompress(OutputStream out) throws IOException {
        compressText(huffmanCode, lengthCode, out);
    }
}
