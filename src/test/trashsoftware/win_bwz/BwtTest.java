package trashsoftware.win_bwz;

import trashsoftware.win_bwz.core.bwz.bwt.BWTDecoder;
import trashsoftware.win_bwz.core.bwz.bwt.BWTEncoder;

import java.util.Arrays;

public class BwtTest {

    public static void main(String[] args) {
        String s = "mississippi";
        byte[] b = s.getBytes();
//        b[0] = (byte) 255;

//        SuffixArrayDoubling.allocateArrays(s.length(), 1);
        BWTEncoder bwtEncoder = new BWTEncoder(b, 0, b.length, false, 0);
        int[] trans = bwtEncoder.Transform();
        System.out.println(Arrays.toString(trans));

        BWTDecoder bwtDecoder = new BWTDecoder(trans);
        byte[] inv = bwtDecoder.Decode();

        String sv = new String(inv);
        System.out.println(sv);
    }
}
