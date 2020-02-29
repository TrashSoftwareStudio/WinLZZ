package trashsoftware.trashGraphics.core;

import trashsoftware.winBwz.core.bwz.BWZDeCompressor;
import trashsoftware.winBwz.utility.Bytes;
import trashsoftware.winBwz.utility.Util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

public class TgiDecoder {

    private InputStream inputStream;

    private int bitDepth;
    private boolean colored;
    private int compressionIndicator;
    private int width;
    private int height;
    private long creationTime;
    private int dataOffset;
    private int compressWindowSize;

    private String cmpTempName, tempName, tempDataName;

    public TgiDecoder(InputStream inputStream, String fileName) {
        this.inputStream = inputStream;
        cmpTempName = fileName + ".cmp";
        tempName = fileName + ".temp.bmp";
    }

    public static BufferedImage readAsBitmap(InputStream inputStream, String fileName) throws Exception {
        TgiDecoder decoder = new TgiDecoder(inputStream, fileName);
        return decoder.toBitmap();
    }

    public BufferedImage toBitmap() throws Exception {
        readHeader();

        int dataSize = width * height * bitDepth / 8;
        Util.fileTruncate(inputStream, cmpTempName, 8192, dataSize);

        ByteArrayOutputStream tgiDataStream = new ByteArrayOutputStream();

        if (compressionIndicator == 1) {
            BWZDeCompressor contentDecompressor = new BWZDeCompressor(cmpTempName, compressWindowSize, 0);
            contentDecompressor.uncompress(tgiDataStream);
        }

        tgiDataStream.flush();
        tgiDataStream.close();

        writeToTempBmp(tgiDataStream.toByteArray());

        FileInputStream bmpInputStream = new FileInputStream(tempName);
        BufferedImage bufferedImage = ImageIO.read(bmpInputStream);

        bmpInputStream.close();
        Util.deleteFile(cmpTempName);
        Util.deleteFile(tempName);

        return bufferedImage;
    }

    private void writeToTempBmp(byte[] tgiData) throws IOException {
        BufferedOutputStream bmpOutput = new BufferedOutputStream(new FileOutputStream(tempName));

        int dimension = width * height;

        byte[] bmpHeader = new byte[54];
        bmpHeader[0] = 'B';
        bmpHeader[1] = 'M';
        int bfSize = 54 + dimension * 3;
        Bytes.intToBytes32Little(bfSize, bmpHeader, 2);
        Bytes.intToBytes32Little(54, bmpHeader, 10);

        Bytes.intToBytes32Little(40, bmpHeader, 14);
        Bytes.intToBytes32Little(width, bmpHeader, 18);
        Bytes.intToBytes32Little(height, bmpHeader, 22);
        bmpHeader[26] = 1;
        bmpHeader[28] = 24;
        bmpOutput.write(bmpHeader);

        if (bitDepth == 4) {
            if (colored) {

            } else {
                byte[] arr = new byte[6];
                int dataLen = dimension / 2;
                for (int i = 0; i < dataLen; ++i) {
                    byte grayScaleTwoPixels = tgiData[i];
                    byte gs1 = (byte) (grayScaleTwoPixels & 0xf0);  // first 4 bits, multiply by 16
                    byte gs2 = (byte) ((grayScaleTwoPixels & 0x0f) << 4);  // last 4 bits, multiply by 16
                    arr[0] = gs1;
                    arr[1] = gs1;
                    arr[2] = gs1;
                    arr[3] = gs2;
                    arr[4] = gs2;
                    arr[5] = gs2;
                    bmpOutput.write(arr);
                }
            }
        } else if (bitDepth == 8) {
            byte[] arr = new byte[3];
            if (colored) {
                for (int i = 0; i < dimension; ++i) {
                    int merged = tgiData[i] & 0xff;
                    int b = merged & 0b11000000;
                    int g = (merged & 0b00111000) << 2;
                    int r = (merged & 0b00000111) << 5;
                    arr[0] = (byte) b;
                    arr[1] = (byte) g;
                    arr[2] = (byte) r;
                    bmpOutput.write(arr);
                }
            } else {
                for (int i = 0; i < dimension; ++i) {
                    byte grayScale = tgiData[i];
                    arr[0] = grayScale;
                    arr[1] = grayScale;
                    arr[2] = grayScale;
                    bmpOutput.write(arr);
                }
            }
        } else if (bitDepth == 16) {
            if (colored) {
                byte[] arr = new byte[3];
                for (int i = 0; i < dimension; ++i) {
                    int merged = ((tgiData[i * 2] & 0xff) << 8) | (tgiData[i * 2 + 1] & 0xff);
                    int b = (merged & 0b11111000_00000000) >> 8;
                    int g = (merged & 0b00000111_11100000) >> 3;
                    int r = (merged & 0b00011111) << 3;
                    arr[0] = (byte) b;
                    arr[1] = (byte) g;
                    arr[2] = (byte) r;
                    bmpOutput.write(arr);
                }
            } else {
                throw new TgiException();
            }
        } else if (bitDepth == 24) {
            if (colored) {
                bmpOutput.write(tgiData);
            } else {
                throw new TgiException();
            }
        } else if (bitDepth == 32) {

        } else {
            bmpOutput.flush();
            bmpOutput.close();
            throw new TgiException("Unknown bit depth " + bitDepth);
        }

        bmpOutput.flush();
        bmpOutput.close();
    }

    private void readHeader() throws IOException {
        byte[] header = new byte[TgiCoder.FILE_HEADER_SIZE];
        if (inputStream.read(header) != TgiCoder.FILE_HEADER_SIZE) throw new TgiException();
        long signature = Bytes.bytesToInt32(header, 0);
        if (signature != TgiCoder.SIGNATURE) {
            throw new TgiException("Signature does not match");
        }
        dataOffset = (int) Bytes.bytesToInt32(header, 4);
        int infoHeaderSize = Bytes.bytesToShort(header, 8) & 0xffff;

        byte[] infoHeader = new byte[infoHeaderSize];
        if (inputStream.read(infoHeader) != infoHeaderSize) throw new TgiException();

        bitDepth = infoHeader[0] & 0xff;
        int colorCompression = infoHeader[1] & 0xff;
        colored = colorCompression >> 4 == 1;
        compressionIndicator = colorCompression & 0x0f;

        width = (int) Bytes.bytesToInt32(infoHeader, 2);
        height = (int) Bytes.bytesToInt32(infoHeader, 6);
        creationTime = Bytes.recoverTimeMillsFromInt((int) Bytes.bytesToInt32(infoHeader, 10));
        compressWindowSize = 1 << (infoHeader[14] & 0xff);  // 2 ^ windowSizeLog2
    }
}
