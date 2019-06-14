package trashsoftware.win_bwz.LZZ2.Util;

import trashsoftware.win_bwz.Utility.FileBitInputStream;
import trashsoftware.win_bwz.Utility.FileBitOutputStream;

import java.io.BufferedOutputStream;
import java.io.IOException;

/**
 * Utility function serve for LZZ2 compression and decompression programs.
 *
 * @since 0.4
 */
public abstract class LZZ2Util {

    /**
     * Writes the length to two output streams in specified format.
     *
     * @param distance the length
     * @param minimum  the minimum matching length
     * @param bos      the head output stream
     * @param fbo      the extension bits output stream
     * @throws IOException if any of the two streams is not writable
     */
    public static void addLength(int distance, int minimum, BufferedOutputStream bos, FileBitOutputStream fbo)
            throws IOException {
        // In this case, "distance" means length
        int bits = 0;
        int bitLength = 0;

        if (distance == minimum) {
            // 0 bits
            bos.write((byte) 0);
        } else if (distance == minimum + 1) {
            // 0 bits
            bos.write((byte) 1);
        } else if (distance == minimum + 2) {
            // 0 bits
            bos.write((byte) 2);
        } else if (distance == minimum + 3) {
            // 0 bits
            bos.write((byte) 3);
        } else if (distance == minimum + 4) {
            // 0 bits
            bos.write((byte) 4);
        } else if (distance == minimum + 5) {
            // 0 bits
            bos.write((byte) 5);
        } else if (distance < minimum + 8) {
            // 1 bits
            bos.write((byte) 6);
            bits = distance - minimum - 6;
            bitLength = 1;
        } else if (distance < minimum + 10) {
            // 1 bits
            bos.write((byte) 7);
            bits = distance - minimum - 8;
            bitLength = 1;
        } else if (distance < minimum + 12) {
            // 1 bits
            bos.write((byte) 8);
            bits = distance - minimum - 10;
            bitLength = 1;
        } else if (distance < minimum + 14) {
            // 1 bits
            bos.write((byte) 9);
            bits = distance - minimum - 12;
            bitLength = 1;
        } else if (distance < minimum + 18) {
            // 2 bits
            bos.write((byte) 10);
            bits = distance - minimum - 14;
            bitLength = 2;
        } else if (distance < minimum + 22) {
            // 2 bits
            bos.write((byte) 11);
            bits = distance - minimum - 18;
            bitLength = 2;
        } else if (distance < minimum + 26) {
            // 2 bits
            bos.write((byte) 12);
            bits = distance - minimum - 22;
            bitLength = 2;
        } else if (distance < minimum + 30) {
            // 2 bits
            bos.write((byte) 13);
            bits = distance - minimum - 26;
            bitLength = 2;
        } else if (distance < minimum + 38) {
            // 3 bits
            bos.write((byte) 14);
            bits = distance - minimum - 30;
            bitLength = 3;
        } else if (distance < minimum + 46) {
            // 3 bits
            bos.write((byte) 15);
            bits = distance - minimum - 38;
            bitLength = 3;
        } else if (distance < minimum + 54) {
            // 3 bits
            bos.write((byte) 16);
            bits = distance - minimum - 46;
            bitLength = 3;
        } else if (distance < minimum + 62) {
            // 3 bits
            bos.write((byte) 17);
            bits = distance - minimum - 54;
            bitLength = 3;
        } else if (distance < minimum + 78) {
            // 4 bits
            bos.write((byte) 18);
            bits = distance - minimum - 62;
            bitLength = 4;
        } else if (distance < minimum + 94) {
            // 4 bits
            bos.write((byte) 19);
            bits = distance - minimum - 78;
            bitLength = 4;
        } else if (distance < minimum + 110) {
            // 4 bits
            bos.write((byte) 20);
            bits = distance - minimum - 94;
            bitLength = 4;
        } else if (distance < minimum + 126) {
            // 4 bits
            bos.write((byte) 21);
            bits = distance - minimum - 110;
            bitLength = 4;
        } else if (distance < minimum + 158) {
            // 5 bits
            bos.write((byte) 22);
            bits = distance - minimum - 126;
            bitLength = 5;
        } else if (distance < minimum + 190) {
            // 5 bits
            bos.write((byte) 23);
            bits = distance - minimum - 158;
            bitLength = 5;
        } else if (distance < minimum + 222) {
            // 5 bits
            bos.write((byte) 24);
            bits = distance - minimum - 190;
            bitLength = 5;
        } else if (distance < minimum + 254) {
            // 5 bits
            bos.write((byte) 25);
            bits = distance - minimum - 222;
            bitLength = 5;
        } else if (distance < minimum + 286) {
            // 5 bits
            bos.write((byte) 26);
            bits = distance - minimum - 254;
            bitLength = 5;
        } else {
            // 0 bits
            // minimum + 286
            bos.write((byte) 27);
        }

        fbo.write(bits, bitLength);
    }

    /**
     * Writes the distance to two output streams in specified format.
     *
     * @param distance the distance
     * @param minimum  the minimum matching length
     * @param bos      the head output stream
     * @param fbo      the extension bits output stream
     * @throws IOException if any of the two streams is not writable
     */
    public static void addDistance(int distance, int minimum, BufferedOutputStream bos, FileBitOutputStream fbo) throws IOException {
        int bits = 0;
        int bitLength = 0;

        // 0, 1, 2, 3 reserved for repeated distances.
        if (distance == minimum) {  // Currently unused since distance will not equal to 0.
            // 0 bits
            throw new IndexOutOfBoundsException();
//            bos.write((byte) 4);
        } else if (distance == minimum + 1) {
            // 0 bits
            bos.write((byte) 5);
        } else if (distance == minimum + 2) {
            // 0 bits
            bos.write((byte) 6);
        } else if (distance == minimum + 3) {
            // 0 bits
            bos.write((byte) 7);
        } else if (distance < minimum + 6) {
            // 4 - 5
            // 1 bit
            bos.write((byte) 8);
            bits = distance - minimum - 4;
            bitLength = 1;
        } else if (distance < minimum + 8) {
            // 6 - 7
            // 1 bit
            bos.write((byte) 9);
            bits = distance - minimum - 6;
            bitLength = 1;
        } else if (distance < minimum + 12) {
            // 8 - 11
            // 2 bits
            bos.write((byte) 10);
            bits = distance - minimum - 8;
            bitLength = 2;
        } else if (distance < minimum + 16) {
            // 2 bits
            bos.write((byte) 11);
            bits = distance - minimum - 12;
            bitLength = 2;
        } else if (distance < minimum + 24) {
            // 3 bits
            bos.write((byte) 12);
            bits = distance - minimum - 16;
            bitLength = 3;
        } else if (distance < minimum + 32) {
            // 3 bits
            bos.write((byte) 13);
            bits = distance - minimum - 24;
            bitLength = 3;
        } else if (distance < minimum + 48) {
            // 4 bits
            bos.write((byte) 14);
            bits = distance - minimum - 32;
            bitLength = 4;
        } else if (distance < minimum + 64) {
            // 4 bits
            bos.write((byte) 15);
            bits = distance - minimum - 48;
            bitLength = 4;
        } else if (distance < minimum + 96) {
            // 5 bits
            bos.write((byte) 16);
            bits = distance - minimum - 64;
            bitLength = 5;
        } else if (distance < minimum + 128) {
            // 5 bits
            bos.write((byte) 17);
            bits = distance - minimum - 96;
            bitLength = 5;
        } else if (distance < minimum + 192) {
            // 6 bits
            bos.write((byte) 18);
            bits = distance - minimum - 128;
            bitLength = 6;
        } else if (distance < minimum + 256) {
            // 6 bits
            bos.write((byte) 19);
            bits = distance - minimum - 192;
            bitLength = 6;
        } else if (distance < minimum + 384) {
            // 7 bits
            bos.write((byte) 20);
            bits = distance - minimum - 256;
            bitLength = 7;
        } else if (distance < minimum + 512) {
            // 7 bits
            bos.write((byte) 21);
            bits = distance - minimum - 384;
            bitLength = 7;
        } else if (distance < minimum + 768) {
            // 8 bits
            bos.write((byte) 22);
            bits = distance - minimum - 512;
            bitLength = 8;
        } else if (distance < minimum + 1024) {
            // 8 bits
            bos.write((byte) 23);
            bits = distance - minimum - 768;
            bitLength = 8;
        } else if (distance < minimum + 1536) {
            // 9 bits
            bos.write((byte) 24);
            bits = distance - minimum - 1024;
            bitLength = 9;
        } else if (distance < minimum + 2048) {
            // 9 bits
            bos.write((byte) 25);
            bits = distance - minimum - 1536;
            bitLength = 9;
        } else if (distance < minimum + 3072) {
            // 10 bits
            bos.write((byte) 26);
            bits = distance - minimum - 2048;
            bitLength = 10;
        } else if (distance < minimum + 4096) {
            // 10 bits
            bos.write((byte) 27);
            bits = distance - minimum - 3072;
            bitLength = 10;
        } else if (distance < minimum + 6144) {
            // 11 bits
            bos.write((byte) 28);
            bits = distance - minimum - 4096;
            bitLength = 11;
        } else if (distance < minimum + 8192) {
            // 11 bits
            bos.write((byte) 29);
            bits = distance - minimum - 6144;
            bitLength = 11;
        } else if (distance < minimum + 12288) {
            // 12 bits
            bos.write((byte) 30);
            bits = distance - minimum - 8192;
            bitLength = 12;
        } else if (distance < minimum + 16384) {
            // 12 bits
            bos.write((byte) 31);
            bits = distance - minimum - 12288;
            bitLength = 12;
        } else if (distance < minimum + 24576) {
            // 13 bits
            bos.write((byte) 32);
            bits = distance - minimum - 16384;
            bitLength = 13;
        } else if (distance < minimum + 32768) {
            // 13 bits
            bos.write((byte) 33);
            bits = distance - minimum - 24576;
            bitLength = 13;
        } else if (distance < minimum + 49152) {
            // 14 bits
            bos.write((byte) 34);
            bits = distance - minimum - 32768;
            bitLength = 14;
        } else if (distance < minimum + 65536) {
            // 14 bits
            // Up to 65535
            // The maximum of LZZ Algorithm
            bos.write((byte) 35);
            bits = distance - minimum - 49152;
            bitLength = 14;
        } else if (distance < minimum + 98304) {
            // 15 bits
            bos.write((byte) 36);
            bits = distance - minimum - 65536;
            bitLength = 15;
        } else if (distance < minimum + 131072) {
            // 15 bits
            bos.write((byte) 37);
            bits = distance - minimum - 98304;
            bitLength = 15;
        } else if (distance < minimum + 196608) {
            // 16 bits
            bos.write((byte) 38);
            bits = distance - minimum - 131072;
            bitLength = 16;
        } else if (distance < minimum + 262144) {
            // 16 bits
            bos.write((byte) 39);
            bits = distance - minimum - 196608;
            bitLength = 16;
        } else if (distance < minimum + 393216) {
            // 17 bits
            bos.write((byte) 40);
            bits = distance - minimum - 262144;
            bitLength = 17;
        } else if (distance < minimum + 524288) {
            // 17 bits
            bos.write((byte) 41);
            bits = distance - minimum - 393216;
            bitLength = 17;
        } else if (distance < minimum + 786432) {
            // 18 bits
            bos.write((byte) 42);
            bits = distance - minimum - 524288;
            bitLength = 18;
        } else if (distance < minimum + 1048576) {
            // 18 bits
            bos.write((byte) 43);
            bits = distance - minimum - 786432;
            bitLength = 18;
        } else if (distance < minimum + 1572864) {
            // 20 bits
            bos.write((byte) 44);
            bits = distance - minimum - 1048576;
            bitLength = 19;
        } else if (distance < minimum + 2097152) {
            // 21 bits
            bos.write((byte) 45);
            bits = distance - minimum - 1572864;
            bitLength = 19;
        } else if (distance < minimum + 3145728) {
            // 22 bits
            bos.write((byte) 46);
            bits = distance - minimum - 2097152;
            bitLength = 20;
        } else if (distance < minimum + 4194304) {
            // 22 bits
            bos.write((byte) 47);
            bits = distance - minimum - 3145728;
            bitLength = 20;
        } else if (distance < minimum + 6291456) {
            // 22 bits
            bos.write((byte) 48);
            bits = distance - minimum - 4194304;
            bitLength = 21;
        } else if (distance < minimum + 8388608) {
            // 22 bits
            bos.write((byte) 49);
            bits = distance - minimum - 6291456;
            bitLength = 21;
        } else if (distance < minimum + 12582912) {
            // 22 bits
            bos.write((byte) 50);
            bits = distance - minimum - 8388608;
            bitLength = 22;
        } else {
            // 23 bits
            // Up to 16777215
            // Maximum of LZZ2 algorithm
            bos.write((byte) 51);
            bits = distance - minimum - 12582912;
            bitLength = 22;
        }

        fbo.write(bits, bitLength);
    }

    /**
     * Returns the distance recovered from the <code>disRep</code> and <code>dlbBis</code>
     *
     * @param disRep the distance head
     * @param dlbBis the extension bits input stream
     * @return the recovered distance
     * @throws IOException if the <code>disRep</code> is not readable
     */
    public static int recoverDistance(int disRep, FileBitInputStream dlbBis) throws IOException {
        int bitLen;
        int add;
        switch (disRep) {
            case 4:
//                bitLen = 0;
//                add = 0;
                throw new IndexOutOfBoundsException("Not possible case");
//                break;
            case 5:
                bitLen = 0;
                add = 1;
                break;
            case 6:
                bitLen = 0;
                add = 2;
                break;
            case 7:
                bitLen = 0;
                add = 3;
                break;
            case 8:
                bitLen = 1;
                add = 4;
                break;
            case 9:
                bitLen = 1;
                add = 6;
                break;
            case 10:
                bitLen = 2;
                add = 8;
                break;
            case 11:
                bitLen = 2;
                add = 12;
                break;
            case 12:
                bitLen = 3;
                add = 16;
                break;
            case 13:
                bitLen = 3;
                add = 24;
                break;
            case 14:
                bitLen = 4;
                add = 32;
                break;
            case 15:
                bitLen = 4;
                add = 48;
                break;
            case 16:
                bitLen = 5;
                add = 64;
                break;
            case 17:
                bitLen = 5;
                add = 96;
                break;
            case 18:
                bitLen = 6;
                add = 128;
                break;
            case 19:
                bitLen = 6;
                add = 192;
                break;
            case 20:
                bitLen = 7;
                add = 256;
                break;
            case 21:
                bitLen = 7;
                add = 384;
                break;
            case 22:
                bitLen = 8;
                add = 512;
                break;
            case 23:
                bitLen = 8;
                add = 768;
                break;
            case 24:
                bitLen = 9;
                add = 1024;
                break;
            case 25:
                bitLen = 9;
                add = 1536;
                break;
            case 26:
                bitLen = 10;
                add = 2048;
                break;
            case 27:
                bitLen = 10;
                add = 3072;
                break;
            case 28:
                bitLen = 11;
                add = 4096;
                break;
            case 29:
                bitLen = 11;
                add = 6144;
                break;
            case 30:
                bitLen = 12;
                add = 8192;
                break;
            case 31:
                bitLen = 12;
                add = 12288;
                break;
            case 32:
                bitLen = 13;
                add = 16384;
                break;
            case 33:
                bitLen = 13;
                add = 24576;
                break;
            case 34:
                bitLen = 14;
                add = 32768;
                break;
            case 35:
                bitLen = 14;
                add = 49152;
                break;
            case 36:
                bitLen = 15;
                add = 65536;
                break;
            case 37:
                bitLen = 15;
                add = 98304;
                break;
            case 38:
                bitLen = 16;
                add = 131072;
                break;
            case 39:
                bitLen = 16;
                add = 196608;
                break;
            case 40:
                bitLen = 17;
                add = 262144;
                break;
            case 41:
                bitLen = 17;
                add = 393216;
                break;
            case 42:
                bitLen = 18;
                add = 524288;
                break;
            case 43:
                bitLen = 18;
                add = 786432;
                break;
            case 44:
                bitLen = 19;
                add = 1048576;
                break;
            case 45:
                bitLen = 19;
                add = 1572864;
                break;
            case 46:
                bitLen = 20;
                add = 2097152;
                break;
            case 47:
                bitLen = 20;
                add = 3145728;
                break;
            case 48:
                bitLen = 21;
                add = 4194304;
                break;
            case 49:
                bitLen = 21;
                add = 6291456;
                break;
            case 50:
                bitLen = 22;
                add = 8388608;
                break;
            case 51:
                bitLen = 22;
                add = 12582912;
                break;
            default:
                bitLen = 0;
                add = 0;
                break;
        }
        // Max 16777215
        if (bitLen != 0) {
            return add + dlbBis.read(bitLen);
        } else {
            return add;
        }
    }

    /**
     * Returns the length recovered from the <code>lenRep</code> and <code>dlbBis</code>
     *
     * @param lenRep the distance head
     * @param dlbBis the extension bits input stream
     * @return the recovered length
     * @throws IOException if the <code>disRep</code> is not readable
     */
    public static int recoverLength(int lenRep, FileBitInputStream dlbBis) throws IOException {
        int bitLen;
        int add;
        switch (lenRep) {
            case 0:
                bitLen = 0;
                add = 0;
                break;
            case 1:
                bitLen = 0;
                add = 1;
                break;
            case 2:
                bitLen = 0;
                add = 2;
                break;
            case 3:
                bitLen = 0;
                add = 3;
                break;
            case 4:
                bitLen = 0;
                add = 4;
                break;
            case 5:
                bitLen = 0;
                add = 5;
                break;
            case 6:
                bitLen = 1;
                add = 6;
                break;
            case 7:
                bitLen = 1;
                add = 8;
                break;
            case 8:
                bitLen = 1;
                add = 10;
                break;
            case 9:
                bitLen = 1;
                add = 12;
                break;
            case 10:
                bitLen = 2;
                add = 14;
                break;
            case 11:
                bitLen = 2;
                add = 18;
                break;
            case 12:
                bitLen = 2;
                add = 22;
                break;
            case 13:
                bitLen = 2;
                add = 26;
                break;
            case 14:
                bitLen = 3;
                add = 30;
                break;
            case 15:
                bitLen = 3;
                add = 38;
                break;
            case 16:
                bitLen = 3;
                add = 46;
                break;
            case 17:
                bitLen = 3;
                add = 54;
                break;
            case 18:
                bitLen = 4;
                add = 62;
                break;
            case 19:
                bitLen = 4;
                add = 78;
                break;
            case 20:
                bitLen = 4;
                add = 94;
                break;
            case 21:
                bitLen = 4;
                add = 110;
                break;
            case 22:
                bitLen = 5;
                add = 126;
                break;
            case 23:
                bitLen = 5;
                add = 158;
                break;
            case 24:
                bitLen = 5;
                add = 190;
                break;
            case 25:
                bitLen = 5;
                add = 222;
                break;
            case 26:
                bitLen = 5;
                add = 254;
                break;
            default:
                bitLen = 0;
                add = 286;
                break;
        }
        // Max 286
        if (bitLen != 0) {
            return add + dlbBis.read(bitLen);
        } else {
            return add;
        }
    }
}
