package trashsoftware.winBwz.core.fasterLzz;

import trashsoftware.winBwz.utility.FileBitInputStream;

import java.io.IOException;

public class FasterLzzUtil {

    static final int MINIMUM_LENGTH = 4;
    static final int MINIMUM_DISTANCE = 1;

    public static int readDistanceFromStream(FileBitInputStream inputStream) throws IOException {
        int head = inputStream.read(3);
        int bitLength;
        int base;
        switch (head) {
            case 0:
                bitLength = 5;
                base = 0;
                break;
            case 1:
                bitLength = 5;
                base = 32;
                break;
            case 2:
                bitLength = 6;
                base = 64;
                break;
            case 3:
                bitLength = 7;
                base = 128;
                break;
            case 4:
                bitLength = 8;
                base = 256;
                break;
            case 5:
                bitLength = 10;
                base = 512;
                break;
            case 6:
                bitLength = 12;
                base = 1536;
                break;
            case 7:
                bitLength = 16;
                base = 5632;
                break;
            default:
                throw new RuntimeException("Unexpected case of distance");
        }
        int additional = inputStream.read(bitLength);
        return MINIMUM_DISTANCE + base + additional;
    }

    public static int readLengthFromStream(FileBitInputStream inputStream) throws IOException {
        int head = inputStream.read(2);
        int bitLength;
        int base;
        switch (head) {
            case 0:
                bitLength = 0;
                base = 0;
                break;
            case 1:
                bitLength = 2;
                base = 1;
                break;
            case 2:
                bitLength = 4;
                base = 5;
                break;
            case 3:
                bitLength = 8;
                base = 21;
                break;
            default:
                throw new RuntimeException("Unexpected case of length");
        }
        int additional = inputStream.read(bitLength);
        return MINIMUM_LENGTH + base + additional;
    }
}
