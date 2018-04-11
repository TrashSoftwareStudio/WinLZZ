package Test;

import Huffman.RLCCoder.RLCCoder;
import Huffman.RLCCoder.RLCDecoder;

import java.util.Arrays;

public class RLCTest {

    public static void main(String[] args) {
        byte[] b = new byte[512];
        b[0] = 5;
        b[1] = 3;
        for (int i = 2; i < 25; i++) {
            b[i] = 7;
        }
        RLCCoder rlc = new RLCCoder(b);
        rlc.Encode();
        byte[] c = rlc.getMainResult();
        String s = rlc.getRlcBits();

        RLCDecoder rld = new RLCDecoder(c, s);
        byte[] r = rld.Decode();
        System.out.println(Arrays.toString(b));
        System.out.println(Arrays.toString(r));
    }
}
