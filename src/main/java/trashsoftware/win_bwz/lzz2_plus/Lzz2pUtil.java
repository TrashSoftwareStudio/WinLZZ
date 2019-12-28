package trashsoftware.win_bwz.lzz2_plus;

import trashsoftware.win_bwz.utility.FileBitInputStream;
import trashsoftware.win_bwz.utility.FileBitOutputStream;

import java.io.IOException;

public class Lzz2pUtil {

    private static final int MINIMUM_LENGTH = 3;
    private static final int MINIMUM_DISTANCE = 1;

    public static void writeDistanceToStream(int distance, FileBitOutputStream outputStream) throws IOException {
        int head;
        int content;
        int contentBitLength;
        int outstandingDistance = distance - MINIMUM_DISTANCE;
        if (outstandingDistance < 32) {
            head = 0;
            content = outstandingDistance;
            contentBitLength = 5;
        } else if (outstandingDistance < 64) {
            head = 1;
            content = outstandingDistance - 32;
            contentBitLength = 5;
        } else if (outstandingDistance < 128) {
            head = 2;
            content = outstandingDistance - 64;
            contentBitLength = 6;
        } else if (outstandingDistance < 256) {
            head = 3;
            content = outstandingDistance - 128;
            contentBitLength = 7;
        } else if (outstandingDistance < 512) {
            head = 4;
            content = outstandingDistance - 256;
            contentBitLength = 8;
        } else if (outstandingDistance < 1536) {
            head = 5;
            content = outstandingDistance - 512;
            contentBitLength = 10;
        } else if (outstandingDistance < 5632) {
            head = 6;
            content = outstandingDistance - 1536;
            contentBitLength = 12;
        } else {
            head = 7;
            content = outstandingDistance - 5632;
            contentBitLength = 16;
            // maximum 71168 + MINIMUM
        }
        outputStream.write(head, 3);
        outputStream.write(content, contentBitLength);
    }

    public static void writeLengthToStream(int length, FileBitOutputStream outputStream) throws IOException {
        int head;
        int content = 0;
        int contentBitLength = 0;
        int outstandingLength = length - MINIMUM_LENGTH;
        if (outstandingLength == 0) {
            head = 0;
        } else if (outstandingLength < 5) {
            head = 1;
            content = outstandingLength - 1;
            contentBitLength = 2;
        } else if (outstandingLength < 21) {
            head = 2;
            content = outstandingLength - 5;
            contentBitLength = 4;
        } else {
            head = 3;
            content = outstandingLength - 21;
            contentBitLength = 8;
        }
        outputStream.write(head, 2);
        outputStream.write(content, contentBitLength);
    }

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
