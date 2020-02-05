package trashsoftware.win_bwz.huffman;

import trashsoftware.win_bwz.utility.FileBitOutputStream;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class HuffmanCompressorTwoBytes extends HuffmanCompressorBase {

    public static final int END_SIG = 256;

    /**
     * Constructor of a new {@code HuffmanCompressor} instance.
     * <p>
     * Creates a new HuffmanCompressor which takes the "inFile" as the file to compress.
     *
     * @param inFile the file to compress.
     */
    public HuffmanCompressorTwoBytes(String inFile) {
        super(inFile);
    }

    @Override
    protected void generateFreqMap() throws IOException {
        FileInputStream bis = new FileInputStream(inFile);
        byte[] buffer = new byte[bufferSize];

        // read from file
        int read;
        while ((read = bis.read(buffer, 0, bufferSize)) != -1) {
            addArrayToFreqMap(buffer, freqMap, read);
            inFileLength += read;
        }
        lengthRemainder = inFileLength % 256;
        freqMap[END_SIG] = 1;
        bis.close();
    }

    private static void addArrayToFreqMap(byte[] array, int[] freqMap, int range) {
        for (int i = 0; i < range; i+=2) {
            int b = ((array[i] & 0xff) << 8) | (array[i + 1] & 0xff);
            freqMap[b] += 1;
        }
    }

    @Override
    protected void compressText(int[] huffmanCode, int[] lengthCode, OutputStream fos) throws IOException {
        FileInputStream bis = new FileInputStream(inFile);

        byte[] buffer = new byte[bufferSize];

        FileBitOutputStream fbo = new FileBitOutputStream(fos);

        int read;
        while ((read = bis.read(buffer, 0, bufferSize)) > 0) {
            if ((read & 1) == 1) throw new RuntimeException();
            for (int i = 0; i < read; i += 2) {
                int v = ((buffer[i] & 0xff) << 8) | (buffer[i + 1] & 0xff);
                int len = lengthCode[v];
                int code = huffmanCode[v];
                if (len == 0) throw new RuntimeException();
                fbo.write(code, len);
            }
        }
        int endSigLen = lengthCode[END_SIG];
        int endSigCode = huffmanCode[END_SIG];
        if (endSigLen == 0) throw new RuntimeException();
        fbo.write(endSigCode, endSigLen);

        // Deal with the last few bits.
        fbo.flush();
        compressedLength += fbo.getLength();

        bis.close();
    }

    @Override
    public void SepCompress(OutputStream out) throws IOException {
        compressText(huffmanCode, lengthCode, out);
    }
}
