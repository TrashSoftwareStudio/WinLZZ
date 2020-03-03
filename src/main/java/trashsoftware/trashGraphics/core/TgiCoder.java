package trashsoftware.trashGraphics.core;

import trashsoftware.winBwz.core.bwz.BWZCompressor;
import trashsoftware.winBwz.utility.Bytes;
import trashsoftware.winBwz.utility.Util;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Encoder for "Trash Graphics Image"
 */
public class TgiCoder {

    public static final long SIGNATURE = 0x5447494D;

    static final int FILE_HEADER_SIZE = 12;

    static final int COMPRESS_WINDOW_SIZE = 524288;

    public static final int COMPRESSION_NO = 0;
    public static final int COMPRESSION_BWZ = 1;
    public static final int COMPRESSION_DELTA_BWZ = 3;

    private int width, height;
    private ImageViewer imageViewer;

    private String tempDataName;

    private int compressionMethod = COMPRESSION_BWZ;

    public TgiCoder(ImageViewer imageViewer) {
        width = imageViewer.getBaseWidth();
        height = imageViewer.getBaseHeight();
        this.imageViewer = imageViewer;
    }

    public void save(int bitDepth, boolean colored, String fileName) throws Exception {
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(fileName));
        tempDataName = fileName + ".temp";

        byte[] header = generateHeader(bitDepth, colored);
        bufferedOutputStream.write(header);

        writeImageToTemp(bitDepth, colored);

        BWZCompressor contentCompressor = new BWZCompressor(tempDataName, COMPRESS_WINDOW_SIZE);
        contentCompressor.compress(bufferedOutputStream);

        bufferedOutputStream.flush();
        bufferedOutputStream.close();
        Util.deleteFile(tempDataName);
    }

    public void setCompressionMethod(int compressionMethod) {
        this.compressionMethod = compressionMethod;
    }

    /**
     * Generates the image header.
     * <p>
     * Formats:
     * === [0, 12): file header ===
     * 0-3: signature
     * 4-7: offset to image data
     * 8-9: size of info header
     * 10-11: reserved
     * <p>
     * === [12, 28): information header ===
     * 12: bit depth
     * 13: grayscale/compression indicator: bit pos 0-3 grayscale, 4-7 compression:
     * * grayscale: 0 if grayscale, 1 if colored.
     * * compression: 0 if no compression, 1 if bwz
     * 14-17: width in pixel
     * 18-21: height in pixel
     * 22-25: creation time
     * 26: log2 compression window size
     * 17: reserved
     *
     * @param bitDepth bit depth: 8, 24, or 32
     * @param colored  {@code true} if store as colored image, {@code false} if grayscale
     * @return the image header
     */
    private byte[] generateHeader(int bitDepth, boolean colored) {
        int infoHeadSize = 16;
        int dataOffset = FILE_HEADER_SIZE + infoHeadSize;

        byte[] header = new byte[FILE_HEADER_SIZE];
        Bytes.intToBytes32(SIGNATURE, header, 0);
        Bytes.intToBytes32(dataOffset, header, 4);
        Bytes.shortToBytes(infoHeadSize, header, 8);

        byte[] info = new byte[infoHeadSize];
        info[0] = (byte) bitDepth;
        int colorCompression = (colored ? 1 : 0) << 4 | compressionMethod;
        info[1] = (byte) colorCompression;

        Bytes.intToBytes32(width, info, 2);
        Bytes.intToBytes32(height, info, 6);

        int createTime = Bytes.getCurrentTimeInInt();
        Bytes.intToBytes32(createTime, info, 10);

        info[14] = Util.windowSizeToByte(COMPRESS_WINDOW_SIZE);

        byte[] fullHeader = new byte[dataOffset];
        System.arraycopy(header, 0, fullHeader, 0, FILE_HEADER_SIZE);
        System.arraycopy(info, 0, fullHeader, FILE_HEADER_SIZE, infoHeadSize);
        return fullHeader;
    }

    private void writeImageToTemp(int bitDepth, boolean colored) throws IOException {
        OutputStream tempOut = new BufferedOutputStream(new FileOutputStream(tempDataName));

        byte[] imageBgrData = imageViewer.getComposedBgrData();
//        byte[] alphaChannel = imageViewer.getComposedBgrData();
        int pixelsCount = width * height;

        if (bitDepth == 4) {
            if (colored) {
                throw new RuntimeException();
            } else {
                int halfDim = pixelsCount / 2;
                for (int i = 0; i < halfDim; ++i) {
                    int bgrSum1 = (imageBgrData[i * 6] & 0xff) +
                            (imageBgrData[i * 6 + 1] & 0xff) +
                            (imageBgrData[i * 6 + 2] & 0xff);
                    int bgrSum2 = (imageBgrData[i * 6 + 3] & 0xff) +
                            (imageBgrData[i * 6 + 4] & 0xff) +
                            (imageBgrData[i * 6 + 5] & 0xff);
                    int average1 = bgrSum1 / 3;
                    int average2 = bgrSum2 / 3;
                    int toWrite = (average1 & 0xf0) | (average2 >> 4);
                    tempOut.write((byte) toWrite);
                }
            }
        } else if (bitDepth == 8) {
            if (colored) {
                for (int i = 0; i < pixelsCount; ++i) {
                    int b = imageBgrData[i * 3] & 0xff;
                    int g = imageBgrData[i * 3 + 1] & 0xff;
                    int r = imageBgrData[i * 3 + 2] & 0xff;
                    // 2 bits for b, 3 bits for g, 3 bits for r
                    int res = (b & 0b11000000) | ((g >> 2) & 0b00111000) | (r >> 5);
                    tempOut.write((byte) res);
                }
            } else {
                for (int i = 0; i < pixelsCount; ++i) {
                    int bgrSum = (imageBgrData[i * 3] & 0xff) +
                            (imageBgrData[i * 3 + 1] & 0xff) +
                            (imageBgrData[i * 3 + 2] & 0xff);
                    int average = (int) ((double) bgrSum / 3);
                    tempOut.write((byte) average);
                }
            }
        } else if (bitDepth == 16) {
            if (colored) {
                for (int i = 0; i < pixelsCount; ++i) {
                    int b = imageBgrData[i * 3] & 0xff;
                    int g = imageBgrData[i * 3 + 1] & 0xff;
                    int r = imageBgrData[i * 3 + 2] & 0xff;
                    // 5 bits for b, 6 bits for g, 5 bits for r
                    int res = ((b << 8) & 0b11111000_00000000) |
                            ((g << 3) & 0b00000111_11100000) |
                            ((r >> 3) & 0b00011111);
                    tempOut.write((byte) (res >> 8));
                    tempOut.write((byte) res);
                }
            } else {
                throw new TgiException("Unsupported encoding");
            }
        } else if (bitDepth == 24) {
            if (colored) {
                if (compressionMethod == COMPRESSION_DELTA_BWZ) {
                    deltaBwzCompress24bits(imageBgrData, tempOut);
                } else {
                    tempOut.write(imageBgrData);
                }
            } else {
                throw new TgiException("Unsupported encoding");
            }
        } else if (bitDepth == 32) {
            throw new RuntimeException();
        } else {
            throw new UnsupportedTgiOptionException("Unsupported bit depth " + bitDepth);
        }

        tempOut.flush();
        tempOut.close();
    }

    private void deltaBwzCompress24bits(byte[] imageBgrData, OutputStream tempOut) throws IOException {

        // transfer to integrated data
        // e.g. bgrbgrbgr to bbbgggrrr
        byte[] bgrIntegrateData = new byte[imageBgrData.length];
        int pixelsCount = imageBgrData.length / 3;
        int rBegin = pixelsCount * 2;
        for (int i = 0; i < pixelsCount; ++i) {
            bgrIntegrateData[i] = imageBgrData[i * 3];  // blue
            bgrIntegrateData[i + pixelsCount] = imageBgrData[i * 3 + 1];  // green
            bgrIntegrateData[i + rBegin] = imageBgrData[i * 3 + 2];  //red
        }

        // delta encoding
        int current = 0;
        for (byte bgrIntegrateDatum : bgrIntegrateData) {
            int next = bgrIntegrateDatum & 0xff;
            int delta = next - current;
            tempOut.write((byte) delta);
        }
    }
}
