package trashsoftware.winBwz.huffman.MapCompressor;

import trashsoftware.winBwz.utility.FileBitInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * A decompressor that uncompress the canonical huffman map compressed by a {@code MapCompressor}.
 *
 * @author zbh
 * @since 0.4
 */
public class MapDeCompressor extends MapDecompressorBase {

    /**
     * Creates a new instance of a MapDeCompressor Object.
     *
     * @param csq the compressed text.
     */
    public MapDeCompressor(byte[] csq) throws IOException {
        fbi = new FileBitInputStream(new ByteArrayInputStream(csq));
        cclNum = fbi.read(4) + 4;
        lengthRemainder = fbi.read(3);
    }

    protected int getEachCclLength() {
        return 3;
    }

//    private int[] swapCCl(int[] ccl) {
//        int[] origCCL = new int[19];
//        for (int i = 0; i < 19; i++) origCCL[MapCompressor.positions[i]] = ccl[i];
//        return origCCL;
//    }

    public static void main(String[] args) throws IOException {
        int[] map = {1, 3, 7, 2, 6, 9, 15, 12, 14, 0, 0, 0, 1, 2, 4, 8, 14};
        MapCompressor mc = new MapCompressor(map);
        byte[] cmp = mc.Compress(19);

        MapDeCompressor mdc = new MapDeCompressor(cmp);
        byte[] r = mdc.Uncompress(64, 19);
        System.out.println(Arrays.toString(r));
    }
}
