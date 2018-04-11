package Test;

import Huffman.ShortHuffmanCompressor;
import Huffman.ShortHuffmanDeCompressor;

public abstract class ShortHuffmanTest {

    public static void main(String[] args) throws Exception {
        byte[] a = new byte[32];
        ShortHuffmanCompressor shc = new ShortHuffmanCompressor(a);
        byte[] b = shc.Compress();
        ShortHuffmanDeCompressor shd = new ShortHuffmanDeCompressor(b);
        byte[] c = shd.Uncompress();
    }
}
