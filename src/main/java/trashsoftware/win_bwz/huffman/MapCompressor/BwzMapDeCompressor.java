package trashsoftware.win_bwz.huffman.MapCompressor;

import trashsoftware.win_bwz.utility.FileBitInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * A decompressor that uncompress the canonical huffman map compressed by a {@code MapCompressor}.
 *
 * @author zbh
 * @since 0.4
 */
public class BwzMapDeCompressor extends MapDecompressorBase {

    /**
     * Creates a new instance of a MapDeCompressor Object.
     *
     * @param csq the compressed text.
     */
    public BwzMapDeCompressor(byte[] csq) throws IOException {
        fbi = new FileBitInputStream(new ByteArrayInputStream(csq));
        cclNum = fbi.read(5);
        lengthRemainder = fbi.read(3);
    }

    protected int getEachCclLength() {
        return 4;
    }

    public static void main(String[] args) throws IOException {
        int[] map = {1, 3, 7, 2, 6, 9, 15, 12, 14, 0, 0, 0, 1, 2, 4, 8, 14};
        BwzMapCompressor mc = new BwzMapCompressor(map);
        byte[] cmp = mc.Compress(32);

        BwzMapDeCompressor mdc = new BwzMapDeCompressor(cmp);
        byte[] r = mdc.Uncompress(64, 32);
        System.out.println(Arrays.toString(r));
    }
}
