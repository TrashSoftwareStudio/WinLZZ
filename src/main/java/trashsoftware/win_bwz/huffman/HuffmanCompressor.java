package trashsoftware.win_bwz.huffman;

import trashsoftware.win_bwz.utility.FileBitOutputStream;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class HuffmanCompressor extends HuffmanCompressorBase {
    /**
     * Constructor of a new {@code HuffmanCompressor} instance.
     * <p>
     * Creates a new HuffmanCompressor which takes the "inFile" as the file to compress.
     *
     * @param inFile the file to compress.
     */
    public HuffmanCompressor(String inFile) {
        super(inFile);
    }

    /**
     * Generates the frequency map.
     *
     * @throws IOException If file cannot be open.
     */
    protected void generateFreqMap() throws IOException {

        // Create a buffered input stream
        FileInputStream bis = new FileInputStream(inFile);
        byte[] buffer = new byte[bufferSize];

        // read from file
        int read;
        while ((read = bis.read(buffer, 0, bufferSize)) != -1) {
            addArrayToFreqMap(buffer, freqMap, read);
            inFileLength += read;
        }
        lengthRemainder = inFileLength % 256;
        bis.close();
    }

    public static void addArrayToFreqMap(byte[] array, int[] freqMap, int range) {
        for (int i = 0; i < range; i++) {
            int b = array[i] & 0xff;
            freqMap[b] += 1;
        }
    }

    public void SepCompress(OutputStream out) throws IOException {
//        compressedLength = 1;
//        out.write((byte) lengthRemainder);
        compressText(huffmanCode, lengthCode, out);
    }

    protected void compressText(int[] huffmanCode, int[] lengthCode, OutputStream fos) throws IOException {
        FileInputStream bis = new FileInputStream(inFile);

        byte[] buffer = new byte[bufferSize];

        FileBitOutputStream fbo = new FileBitOutputStream(fos);

        int read;
        while ((read = bis.read(buffer, 0, bufferSize)) > 0) {
            for (int i = 0; i < read; ++i) {
                int v = buffer[i] & 0xff;
                int len = lengthCode[v];
                int code = huffmanCode[v];
                if (len == 0) throw new RuntimeException();
                fbo.write(code, len);
            }
        }
        // Deal with the last few bits.
        fbo.flush();
        compressedLength += fbo.getLength();

        bis.close();
    }
}
