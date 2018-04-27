/*
 * BWG Image coder.
 *
 * Header info:
 * 4 bytes: signature
 * 2 bytes: version
 * 1 byte: compression level
 * 1 byte: window size
 * 4 bytes: creation time
 * 4 bytes: CRC32 checksum
 * 4 bytes: width
 * 4 bytes: height
 * 1 byte: digits
 * 4 bytes: reserved offset (Set to 0 by default, used to mark how many bytes are used to store extra information)
 */

package BWGViewer.Codecs.BWGCodec;

import WinLzz.BWZ.BWZCompressor;
import WinLzz.Interface.Compressor;
import WinLzz.LZZ2.LZZ2Compressor;
import WinLzz.LZZ2.Util.LZZ2Util;
import WinLzz.Utility.Bytes;
import WinLzz.Utility.Util;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.zip.CRC32;

public class BWGCoder {

    static final int signature = 0x016416DA;

    /**
     * Set 1999-12-31 19:00 as the initial time.
     */
    final static long dateOffset = 946684800000L;

    static final short version = 1;

    private byte[] pixels;
    private int width;
    private int height;
    private int digits;
    private int windowSize;
    private int compressionLevel;
    private BufferedOutputStream mainOut;
    private String outName, tempName;

    public BWGCoder(byte[] pixels, String outName) throws IOException {
        this.pixels = pixels;
        this.mainOut = new BufferedOutputStream(new FileOutputStream(outName));
        this.outName = outName;
        tempName = outName + ".temp";
    }

    /**
     * Sets up compression level and parameters.
     * <p>
     * Compression levels:
     * 0 = No compression
     * 1 = RLC only
     * 2 = BWZ only
     * 3 = LZZ2 only
     * 4 = RLC + BWZ
     * 5 = RLC + LZZ2
     *
     * @param compressionLevel compression level.
     * @param windowSize       block size or sliding window size.
     */
    public void setCompression(int compressionLevel, int windowSize) {
        this.compressionLevel = compressionLevel;
        this.windowSize = windowSize;
    }

    public void setParameters(int width, int height, int digits) {
        this.width = width;
        this.height = height;
        this.digits = digits;
    }

    public void code() throws Exception {
        mainOut.write(Bytes.intToBytes32(signature));
        mainOut.write(Bytes.shortToBytes(version));
        mainOut.write((byte) compressionLevel);
        mainOut.write(LZZ2Util.windowSizeToByte(windowSize));
        mainOut.write(new byte[8]);  // Reserved for creation time and crc32 checksum.
        mainOut.write(Bytes.intToBytes32(width));
        mainOut.write(Bytes.intToBytes32(height));
        mainOut.write((byte) digits);
        mainOut.write(new byte[4]);  // Reserved offset bytes
        // Currently set to all zero.

        if (compressionLevel == 0) {
            mainOut.write(pixels);
        } else {
            Compressor compressor;
            FileOutputStream tempOut = new FileOutputStream(tempName);
            switch (compressionLevel) {
                case 1:
                    // RLC
                    compressor = null;
                    break;
                case 2:
                    tempOut.write(pixels);
                    compressor = new BWZCompressor(tempName, windowSize);
                    break;
                case 3:
                    tempOut.write(pixels);
                    compressor = new LZZ2Compressor(tempName, windowSize, 286);
                    break;
                default:
                    throw new RuntimeException("No such Algorithm");

            }
            tempOut.flush();
            tempOut.close();
            if (compressor != null) {
                compressor.setCompressionLevel(1);
                compressor.setThreads(4);
                compressor.Compress(mainOut);
            } else {
                Util.fileConcatenate(mainOut, new String[]{tempName}, 8192);
            }
        }
        deleteTemp();
        mainOut.flush();
        mainOut.close();

        CRC32 crc = new CRC32();
        crc.update(pixels);
        long crc32 = crc.getValue();
        byte[] fullBytes = Bytes.longToBytes(crc32);
        byte[] crc32Checksum = new byte[4];
        System.arraycopy(fullBytes, 4, crc32Checksum, 0, 4);

        RandomAccessFile raf = new RandomAccessFile(outName, "rw");
        raf.seek(8);
        int currentTimeInt = (int) ((System.currentTimeMillis() - dateOffset) / 1000);  // Creation time,
        // rounded to second. Starting from 1999-12-31 19:00
        raf.writeInt(currentTimeInt);
        raf.write(crc32Checksum);

        raf.close();

    }

    private void deleteTemp() {
        Util.deleteFile(tempName);
    }
}
