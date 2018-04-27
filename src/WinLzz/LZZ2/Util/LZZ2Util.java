package WinLzz.LZZ2.Util;

import WinLzz.Utility.Bytes;
import WinLzz.Utility.FileBitInputStream;
import WinLzz.Utility.FileBitOutputStream;
import WinLzz.Utility.Util;

import java.io.BufferedOutputStream;
import java.io.IOException;

import static WinLzz.Utility.Bytes.numberToBitString;

public abstract class LZZ2Util {

    public static void addLength(int distance, int minimum, BufferedOutputStream bos, FileBitOutputStream fbo) throws IOException {
        // In this case, "distance" means length
        String binaryString = "";

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
            binaryString = numberToBitString(distance - minimum - 6, 1);
        } else if (distance < minimum + 10) {
            // 1 bits
            bos.write((byte) 7);
            binaryString = numberToBitString(distance - minimum - 8, 1);
        } else if (distance < minimum + 12) {
            // 1 bits
            bos.write((byte) 8);
            binaryString = numberToBitString(distance - minimum - 10, 1);
        } else if (distance < minimum + 14) {
            // 1 bits
            bos.write((byte) 9);
            binaryString = numberToBitString(distance - minimum - 12, 1);
        } else if (distance < minimum + 18) {
            // 2 bits
            bos.write((byte) 10);
            binaryString = numberToBitString(distance - minimum - 14, 2);
        } else if (distance < minimum + 22) {
            // 2 bits
            bos.write((byte) 11);
            binaryString = numberToBitString(distance - minimum - 18, 2);
        } else if (distance < minimum + 26) {
            // 2 bits
            bos.write((byte) 12);
            binaryString = numberToBitString(distance - minimum - 22, 2);
        } else if (distance < minimum + 30) {
            // 2 bits
            bos.write((byte) 13);
            binaryString = numberToBitString(distance - minimum - 26, 2);
        } else if (distance < minimum + 38) {
            // 3 bits
            bos.write((byte) 14);
            binaryString = numberToBitString(distance - minimum - 30, 3);
        } else if (distance < minimum + 46) {
            // 3 bits
            bos.write((byte) 15);
            binaryString = numberToBitString(distance - minimum - 38, 3);
        } else if (distance < minimum + 54) {
            // 3 bits
            bos.write((byte) 16);
            binaryString = numberToBitString(distance - minimum - 46, 3);
        } else if (distance < minimum + 62) {
            // 3 bits
            bos.write((byte) 17);
            binaryString = numberToBitString(distance - minimum - 54, 3);
        } else if (distance < minimum + 78) {
            // 4 bits
            bos.write((byte) 18);
            binaryString = numberToBitString(distance - minimum - 62, 4);
        } else if (distance < minimum + 94) {
            // 4 bits
            bos.write((byte) 19);
            binaryString = numberToBitString(distance - minimum - 78, 4);
        } else if (distance < minimum + 110) {
            // 4 bits
            bos.write((byte) 20);
            binaryString = numberToBitString(distance - minimum - 94, 4);
        } else if (distance < minimum + 126) {
            // 4 bits
            bos.write((byte) 21);
            binaryString = numberToBitString(distance - minimum - 110, 4);
        } else if (distance < minimum + 158) {
            // 5 bits
            bos.write((byte) 22);
            binaryString = numberToBitString(distance - minimum - 126, 5);
        } else if (distance < minimum + 190) {
            // 5 bits
            bos.write((byte) 23);
            binaryString = numberToBitString(distance - minimum - 158, 5);
        } else if (distance < minimum + 222) {
            // 5 bits
            bos.write((byte) 24);
            binaryString = numberToBitString(distance - minimum - 190, 5);
        } else if (distance < minimum + 254) {
            // 5 bits
            bos.write((byte) 25);
            binaryString = numberToBitString(distance - minimum - 222, 5);
        } else if (distance < minimum + 286) {
            // 5 bits
            bos.write((byte) 26);
            binaryString = numberToBitString(distance - minimum - 254, 5);
        } else {
            // 0 bits
            // minimum + 286
            bos.write((byte) 27);
        }

        fbo.write(binaryString);
    }

    public static void addDistance(int distance, int minimum, BufferedOutputStream bos, FileBitOutputStream fbo) throws IOException {
        String binaryString = "";

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
            binaryString = numberToBitString(distance - minimum - 4, 1);
        } else if (distance < minimum + 8) {
            // 6 - 7
            // 1 bit
            bos.write((byte) 9);
            binaryString = numberToBitString(distance - minimum - 6, 1);
        } else if (distance < minimum + 12) {
            // 8 - 11
            // 2 bits
            bos.write((byte) 10);
            binaryString = numberToBitString(distance - minimum - 8, 2);
        } else if (distance < minimum + 16) {
            // 2 bits
            bos.write((byte) 11);
            binaryString = numberToBitString(distance - minimum - 12, 2);
        } else if (distance < minimum + 24) {
            // 3 bits
            bos.write((byte) 12);
            binaryString = numberToBitString(distance - minimum - 16, 3);
        } else if (distance < minimum + 32) {
            // 3 bits
            bos.write((byte) 13);
            binaryString = numberToBitString(distance - minimum - 24, 3);
        } else if (distance < minimum + 48) {
            // 4 bits
            bos.write((byte) 14);
            binaryString = numberToBitString(distance - minimum - 32, 4);
        } else if (distance < minimum + 64) {
            // 4 bits
            bos.write((byte) 15);
            binaryString = numberToBitString(distance - minimum - 48, 4);
        } else if (distance < minimum + 96) {
            // 5 bits
            bos.write((byte) 16);
            binaryString = numberToBitString(distance - minimum - 64, 5);
        } else if (distance < minimum + 128) {
            // 5 bits
            bos.write((byte) 17);
            binaryString = numberToBitString(distance - minimum - 96, 5);
        } else if (distance < minimum + 192) {
            // 6 bits
            bos.write((byte) 18);
            binaryString = numberToBitString(distance - minimum - 128, 6);
        } else if (distance < minimum + 256) {
            // 6 bits
            bos.write((byte) 19);
            binaryString = numberToBitString(distance - minimum - 192, 6);
        } else if (distance < minimum + 384) {
            // 7 bits
            bos.write((byte) 20);
            binaryString = numberToBitString(distance - minimum - 256, 7);
        } else if (distance < minimum + 512) {
            // 7 bits
            bos.write((byte) 21);
            binaryString = numberToBitString(distance - minimum - 384, 7);
        } else if (distance < minimum + 768) {
            // 8 bits
            bos.write((byte) 22);
            binaryString = numberToBitString(distance - minimum - 512, 8);
        } else if (distance < minimum + 1024) {
            // 8 bits
            bos.write((byte) 23);
            binaryString = numberToBitString(distance - minimum - 768, 8);
        } else if (distance < minimum + 1536) {
            // 9 bits
            bos.write((byte) 24);
            binaryString = numberToBitString(distance - minimum - 1024, 9);
        } else if (distance < minimum + 2048) {
            // 9 bits
            bos.write((byte) 25);
            binaryString = numberToBitString(distance - minimum - 1536, 9);
        } else if (distance < minimum + 3072) {
            // 10 bits
            bos.write((byte) 26);
            binaryString = numberToBitString(distance - minimum - 2048, 10);
        } else if (distance < minimum + 4096) {
            // 10 bits
            bos.write((byte) 27);
            binaryString = numberToBitString(distance - minimum - 3072, 10);
        } else if (distance < minimum + 6144) {
            // 11 bits
            bos.write((byte) 28);
            binaryString = numberToBitString(distance - minimum - 4096, 11);
        } else if (distance < minimum + 8192) {
            // 11 bits
            bos.write((byte) 29);
            binaryString = numberToBitString(distance - minimum - 6144, 11);
        } else if (distance < minimum + 12288) {
            // 12 bits
            bos.write((byte) 30);
            binaryString = numberToBitString(distance - minimum - 8192, 12);
        } else if (distance < minimum + 16384) {
            // 12 bits
            bos.write((byte) 31);
            binaryString = numberToBitString(distance - minimum - 12288, 12);
        } else if (distance < minimum + 24576) {
            // 13 bits
            bos.write((byte) 32);
            binaryString = numberToBitString(distance - minimum - 16384, 13);
        } else if (distance < minimum + 32768) {
            // 13 bits
            bos.write((byte) 33);
            binaryString = numberToBitString(distance - minimum - 24576, 13);
        } else if (distance < minimum + 49152) {
            // 14 bits
            bos.write((byte) 34);
            binaryString = numberToBitString(distance - minimum - 32768, 14);
        } else if (distance < minimum + 65536) {
            // 14 bits
            // Up to 65535
            // The maximum of LZZ Algorithm
            bos.write((byte) 35);
            binaryString = numberToBitString(distance - minimum - 49152, 14);
        } else if (distance < minimum + 98304) {
            // 15 bits
            bos.write((byte) 36);
            binaryString = numberToBitString(distance - minimum - 65536, 15);
        } else if (distance < minimum + 131072) {
            // 15 bits
            bos.write((byte) 37);
            binaryString = numberToBitString(distance - minimum - 98304, 15);
        } else if (distance < minimum + 196608) {
            // 16 bits
            bos.write((byte) 38);
            binaryString = numberToBitString(distance - minimum - 131072, 16);
        } else if (distance < minimum + 262144) {
            // 16 bits
            bos.write((byte) 39);
            binaryString = numberToBitString(distance - minimum - 196608, 16);
        } else if (distance < minimum + 393216) {
            // 17 bits
            bos.write((byte) 40);
            binaryString = numberToBitString(distance - minimum - 262144, 17);
        } else if (distance < minimum + 524288) {
            // 17 bits
            bos.write((byte) 41);
            binaryString = numberToBitString(distance - minimum - 393216, 17);
        } else if (distance < minimum + 786432) {
            // 18 bits
            bos.write((byte) 42);
            binaryString = numberToBitString(distance - minimum - 524288, 18);
        } else if (distance < minimum + 1048576) {
            // 18 bits
            bos.write((byte) 43);
            binaryString = numberToBitString(distance - minimum - 786432, 18);
        } else if (distance < minimum + 1572864) {
            // 20 bits
            bos.write((byte) 44);
            binaryString = numberToBitString(distance - minimum - 1048576, 19);
        } else if (distance < minimum + 2097152) {
            // 21 bits
            bos.write((byte) 45);
            binaryString = numberToBitString(distance - minimum - 1572864, 19);
        } else if (distance < minimum + 3145728) {
            // 22 bits
            bos.write((byte) 46);
            binaryString = numberToBitString(distance - minimum - 2097152, 20);
        } else if (distance < minimum + 4194304) {
            // 22 bits
            bos.write((byte) 47);
            binaryString = numberToBitString(distance - minimum - 3145728, 20);
        } else if (distance < minimum + 6291456) {
            // 22 bits
            bos.write((byte) 48);
            binaryString = numberToBitString(distance - minimum - 4194304, 21);
        } else if (distance < minimum + 8388608) {
            // 22 bits
            bos.write((byte) 49);
            binaryString = numberToBitString(distance - minimum - 6291456, 21);
        } else if (distance < minimum + 12582912) {
            // 22 bits
            bos.write((byte) 50);
            binaryString = numberToBitString(distance - minimum - 8388608, 22);
        } else {
            // 23 bits
            // Up to 16777215
            // Maximum of LZZ2 algorithm
            bos.write((byte) 51);
            binaryString = numberToBitString(distance - minimum - 12582912, 22);
        }
//        System.out.print(binaryString + " ");

        fbo.write(binaryString);
    }

//    public static void addDistance2(int distance, int minimum, BufferedOutputStream bos) throws IOException {
//        String binaryString = "";
//
//        // 0, 1, 2, 3 reserved for repeated distances.
//        if (distance == minimum) {  // Currently unused since distance will not equal to 0.
//            // 0 bits
//            throw new IndexOutOfBoundsException();
////            bos.write((byte) 4);
//        } else if (distance == minimum + 1) {
//            // 0 bits
//            bos.write((byte) 5);
//        } else if (distance == minimum + 2) {
//            // 0 bits
//            bos.write((byte) 6);
//        } else if (distance == minimum + 3) {
//            // 0 bits
//            bos.write((byte) 7);
//        } else if (distance < minimum + 6) {
//            // 4 - 5
//            // 1 bit
//            bos.write((byte) 8);
//            bos.write(enumerate(distance - minimum - 4));
//        } else if (distance < minimum + 8) {
//            // 6 - 7
//            // 1 bit
//            bos.write((byte) 9);
//            bos.write(enumerate(distance - minimum - 6));
//        } else if (distance < minimum + 12) {
//            // 8 - 11
//            // 2 bits
//            bos.write((byte) 10);
//            bos.write(enumerate(distance - minimum - 10));
//        } else if (distance < minimum + 16) {
//            // 2 bits
//            bos.write((byte) 11);
//            bos.write(enumerate(distance - minimum - 12));
//        } else if (distance < minimum + 24) {
//            // 3 bits
//            bos.write((byte) 12);
//            bos.write(enumerate(distance - minimum - 16));
//        } else if (distance < minimum + 32) {
//            // 3 bits
//            bos.write((byte) 13);
//            bos.write(enumerate(distance - minimum - 24));
//        } else if (distance < minimum + 48) {
//            // 4 bits
//            bos.write((byte) 14);
//            bos.write(enumerate(distance - minimum - 32));
//        } else if (distance < minimum + 64) {
//            // 4 bits
//            bos.write((byte) 15);
//            bos.write(enumerate(distance - minimum - 48));
//        } else if (distance < minimum + 96) {
//            // 5 bits
//            bos.write((byte) 16);
//            bos.write(enumerate(distance - minimum - 64));
//        } else if (distance < minimum + 128) {
//            // 5 bits
//            bos.write((byte) 17);
//            bos.write(enumerate(distance - minimum - 96));
//        } else if (distance < minimum + 192) {
//            // 6 bits
//            bos.write((byte) 18);
//            bos.write(enumerate(distance - minimum - 128));
//        } else if (distance < minimum + 256) {
//            // 6 bits
//            bos.write((byte) 19);
//            bos.write(enumerate(distance - minimum - 192));
//        } else if (distance < minimum + 384) {
//            // 7 bits
//            bos.write((byte) 20);
//            bos.write(enumerate(distance - minimum - 256));
//        } else if (distance < minimum + 512) {
//            // 7 bits
//            bos.write((byte) 21);
//            bos.write(enumerate(distance - minimum - 384));
//        } else if (distance < minimum + 768) {
//            // 8 bits
//            bos.write((byte) 22);
//            bos.write(enumerate(distance - minimum - 512));
//        } else if (distance < minimum + 1024) {
//            // 8 bits
//            bos.write((byte) 23);
//            bos.write(enumerate(distance - minimum - 768));
//        } else if (distance < minimum + 1536) {
//            // 9 bits
//            bos.write((byte) 24);
//            bos.write(enumerate(distance - minimum - 1024));
//        } else if (distance < minimum + 2048) {
//            // 9 bits
//            bos.write((byte) 25);
//            bos.write(enumerate(distance - minimum - 1536));
//        } else if (distance < minimum + 3072) {
//            // 10 bits
//            bos.write((byte) 26);
//            bos.write(enumerate(distance - minimum - 2048));
//        } else if (distance < minimum + 4096) {
//            // 10 bits
//            bos.write((byte) 27);
//            bos.write(enumerate(distance - minimum - 3072));
//        } else if (distance < minimum + 6144) {
//            // 11 bits
//            bos.write((byte) 28);
//            bos.write(enumerate(distance - minimum - 4096));
//        } else if (distance < minimum + 8192) {
//            // 11 bits
//            bos.write((byte) 29);
//            bos.write(enumerate(distance - minimum - 6144));
//        } else if (distance < minimum + 12288) {
//            // 12 bits
//            bos.write((byte) 30);
//            bos.write(enumerate(distance - minimum - 8192));
//        } else if (distance < minimum + 16384) {
//            // 12 bits
//            bos.write((byte) 31);
//            bos.write(enumerate(distance - minimum - 12288));
//        } else if (distance < minimum + 24576) {
//            // 13 bits
//            bos.write((byte) 32);
//            bos.write(enumerate(distance - minimum - 16384));
//        } else if (distance < minimum + 32768) {
//            // 13 bits
//            bos.write((byte) 33);
//            bos.write(enumerate(distance - minimum - 24576));
//        } else if (distance < minimum + 49152) {
//            // 14 bits
//            bos.write((byte) 34);
//            bos.write(enumerate(distance - minimum - 32768));
//        } else if (distance < minimum + 65536) {
//            // 14 bits
//            // Up to 65535
//            // The maximum of LZZ Algorithm
//            bos.write((byte) 35);
//            bos.write(enumerate(distance - minimum - 49152));
//        } else if (distance < minimum + 98304) {
//            // 15 bits
//            bos.write((byte) 36);
//            bos.write(enumerate(distance - minimum - 65536));
//        } else if (distance < minimum + 131072) {
//            // 15 bits
//            bos.write((byte) 37);
//            binaryString = numberToBitString(distance - minimum - 98304, 15);
//        } else if (distance < minimum + 196608) {
//            // 16 bits
//            bos.write((byte) 38);
//            binaryString = numberToBitString(distance - minimum - 131072, 16);
//        } else if (distance < minimum + 262144) {
//            // 16 bits
//            bos.write((byte) 39);
//            binaryString = numberToBitString(distance - minimum - 196608, 16);
//        } else if (distance < minimum + 393216) {
//            // 17 bits
//            bos.write((byte) 40);
//            binaryString = numberToBitString(distance - minimum - 262144, 17);
//        } else if (distance < minimum + 524288) {
//            // 17 bits
//            bos.write((byte) 41);
//            binaryString = numberToBitString(distance - minimum - 393216, 17);
//        } else if (distance < minimum + 786432) {
//            // 18 bits
//            bos.write((byte) 42);
//            binaryString = numberToBitString(distance - minimum - 524288, 18);
//        } else if (distance < minimum + 1048576) {
//            // 18 bits
//            bos.write((byte) 43);
//            binaryString = numberToBitString(distance - minimum - 786432, 18);
//        } else if (distance < minimum + 1572864) {
//            // 20 bits
//            bos.write((byte) 44);
//            binaryString = numberToBitString(distance - minimum - 1048576, 19);
//        } else if (distance < minimum + 2097152) {
//            // 21 bits
//            bos.write((byte) 45);
//            binaryString = numberToBitString(distance - minimum - 1572864, 19);
//        } else if (distance < minimum + 3145728) {
//            // 22 bits
//            bos.write((byte) 46);
//            binaryString = numberToBitString(distance - minimum - 2097152, 20);
//        } else if (distance < minimum + 4194304) {
//            // 22 bits
//            bos.write((byte) 47);
//            binaryString = numberToBitString(distance - minimum - 3145728, 20);
//        } else if (distance < minimum + 6291456) {
//            // 22 bits
//            bos.write((byte) 48);
//            binaryString = numberToBitString(distance - minimum - 4194304, 21);
//        } else if (distance < minimum + 8388608) {
//            // 22 bits
//            bos.write((byte) 49);
//            binaryString = numberToBitString(distance - minimum - 6291456, 21);
//        } else if (distance < minimum + 12582912) {
//            // 22 bits
//            bos.write((byte) 50);
//            binaryString = numberToBitString(distance - minimum - 8388608, 22);
//        } else {
//            // 23 bits
//            // Up to 16777215
//            // Maximum of LZZ2 algorithm
//            bos.write((byte) 51);
//            binaryString = numberToBitString(distance - minimum - 12582912, 22);
//        }
////        System.out.print(binaryString + " ");
//
//    }

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
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < bitLen; i++) {
                builder.append(dlbBis.read());
            }
//            System.out.print(builder.toString() + " ");
            return add + Integer.parseInt(builder.toString(), 2);
        } else {
            return add;
        }
    }

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
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < bitLen; i++) {
                builder.append(dlbBis.read());
            }
            return add + Integer.parseInt(builder.toString(), 2);
        } else {
            return add;
        }
    }

    public static byte windowSizeToByte(int windowSize) {
        return (byte) Util.ceiling(Util.log2(windowSize));
    }

    public static int byteToWindowSize(byte windowRep) {
        if (windowRep == 0) return 0;
        else return (int) Math.pow(2, windowRep & 0xff);
    }

    public static byte[] generateSizeBlock(int[] sizes) {
        StringBuilder bits = new StringBuilder();
        for (int i : sizes) {
            if (i < 256) {
                bits.append("00");
                bits.append(Bytes.numberToBitString(i, 8));
            } else if (i < 65792) {
                bits.append("01");
                bits.append(Bytes.numberToBitString(i - 256, 16));
            } else if (i < 16843008) {
                bits.append("10");
                bits.append(Bytes.numberToBitString(i - 65792, 24));
            } else {
                bits.append("11");
                bits.append(Bytes.numberToBitString(i - 16843008, 32));
            }
        }
        byte[] block = Bytes.stringBuilderToBytesFull(bits);
        byte[] result = new byte[block.length + 1];
        System.arraycopy(block, 0, result, 0, block.length);
        result[result.length - 1] = (byte) block.length;
        return result;
    }

    public static int[] recoverSizeBlock(byte[] block, int number) {
        String bits = Bytes.bytesToString(block);
        int[] result = new int[number];
        int i = 0;
        int j = 0;
        while (j < number) {
            String flag = bits.substring(i, i + 2);
            i += 2;
            int base;
            int read;
            switch (flag) {
                case "00":
                    base = 0;
                    read = 8;
                    break;
                case "01":
                    base = 256;
                    read = 16;
                    break;
                case "10":
                    base = 65792;
                    read = 24;
                    break;
                case "11":
                    base = 16843008;
                    read = 32;
                    break;
                default:
                    throw new IndexOutOfBoundsException("Unknown Error");
            }
            int num = base + Integer.parseInt(bits.substring(i, i + read), 2);
            i += read;
            result[j] = num;
            j += 1;
        }
        return result;
    }
}
