package BWGViewer.Codecs.BWGCodec;

import BWGViewer.Codecs.CommonLoader;
import BWGViewer.Codecs.DamagedBWGException;
import BWGViewer.Codecs.UnsupportedFormatException;
import WinLzz.BWZ.BWZDeCompressor;
import WinLzz.Interface.DeCompressor;
import WinLzz.LZZ2.LZZ2DeCompressor;
import WinLzz.LZZ2.Util.LZZ2Util;
import WinLzz.Utility.Bytes;
import WinLzz.Utility.Util;

import java.io.*;
import java.util.zip.CRC32;

public class BWGLoader implements CommonLoader {

    private byte[] content;
    private String path, tempName, rlcTempName, decName;
    private int compressionLevel, windowSize;
    private long creationTime, crc32Checksum;

    private int width, height, digits;

    public BWGLoader(String path) {
        this.path = path;
        this.rlcTempName = path + ".rlc";
        this.tempName = path + ".temp";
        this.decName = path + ".dec";
    }

    @Override
    public void load() throws IOException, UnsupportedFormatException {
        long fileLength = new File(path).length();
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(path));
        byte[] buffer4 = new byte[4];
        byte[] buffer2 = new byte[2];
        byte[] buffer1 = new byte[1];
        if (bis.read(buffer4) != 4) throw new IOException("Error occurs while reading");
        int signature = Bytes.bytesToInt32(buffer4);
        if (signature != BWGCoder.signature) throw new UnsupportedFormatException();

        if (bis.read(buffer2) != 2) throw new IOException("Error occurs while reading");
        short version = Bytes.bytesToShort(buffer2);
        if (version != BWGCoder.version) throw new UnsupportedFormatException();

        if (bis.read(buffer1) != 1) throw new IOException("Error occurs while reading");
        compressionLevel = buffer1[0] & 0xff;
        if (bis.read(buffer1) != 1) throw new IOException("Error occurs while reading");
        windowSize = LZZ2Util.byteToWindowSize(buffer1[0]);

        if (bis.read(buffer4) != 4) throw new IOException("Error occurs while reading");
        creationTime = (long) Bytes.bytesToInt32(buffer4) * 1000 + BWGCoder.dateOffset;

        if (bis.read(buffer4) != 4) throw new IOException("Error occurs while reading");
        byte[] fullCRC32Bytes = new byte[8];
        System.arraycopy(buffer4, 0, fullCRC32Bytes, 4, 4);
        crc32Checksum = Bytes.bytesToLong(fullCRC32Bytes);

        if (bis.read(buffer4) != 4) throw new IOException("Error occurs while reading");
        width = Bytes.bytesToInt32(buffer4);
        if (bis.read(buffer4) != 4) throw new IOException("Error occurs while reading");
        height = Bytes.bytesToInt32(buffer4);
        if (bis.read(buffer1) != 1) throw new IOException("Error occurs while reading");
        digits = buffer1[0] & 0xff;

        if (bis.read(buffer4) != 4) throw new IOException("Error occurs while reading");
        int offset = Bytes.bytesToInt32(buffer4);

        if (bis.skip(offset) != offset) throw new IOException("Error occurs while reading");  // Skip the reserved bytes.

        Util.fileTruncate(bis, rlcTempName, 8192, fileLength - 29 - offset);
        bis.close();
        uncompress();
    }

    private void uncompress() throws IOException, UnsupportedFormatException {
        int length = (int) new File(rlcTempName).length();
        FileInputStream fis = new FileInputStream(rlcTempName);
        if (compressionLevel == 0) {
            content = new byte[length];
            if (fis.read(content) != length) throw new IOException("Error occurs while reading");
        } else {
            DeCompressor deCompressor;
            switch (compressionLevel) {
                case 1:
                    // RLC
                    deCompressor = null;
                    break;
                case 2:
                    Util.fileTruncate(fis, tempName, 8192, length);
                    deCompressor = new BWZDeCompressor(tempName, windowSize);
                    deCompressor.setThreads(4);
                    break;
                case 3:
                    Util.fileTruncate(fis, tempName, 8192, length);
                    deCompressor = new LZZ2DeCompressor(tempName, windowSize);
                    break;
                default:
                    deCompressor = null;
                    break;
            }
            fis.close();
            FileOutputStream fos = new FileOutputStream(decName);
            if (deCompressor == null) {
                FileInputStream is = new FileInputStream(tempName);
                Util.fileTruncate(is, decName, 8192, new File(tempName).length());
                is.close();
            } else {
                try {
                    deCompressor.Uncompress(fos);
                } catch (Exception e) {
                    fos.flush();
                    fos.close();
                    deleteTemp();
                    e.printStackTrace();
                    throw new UnsupportedFormatException();
                }
            }
            fos.flush();
            fos.close();
            FileInputStream bitmapIs = new FileInputStream(decName);
            int bitmapLength = (int) new File(decName).length();
            content = new byte[bitmapLength];
            if (bitmapIs.read(content) != bitmapLength) throw new IOException("Error occurs while reading");
            bitmapIs.close();
        }
        deleteTemp();
        if (!checkCrc()) throw new DamagedBWGException();
    }

    private void deleteTemp() {
        Util.deleteFile(tempName);
        Util.deleteFile(rlcTempName);
        Util.deleteFile(decName);
    }

    private boolean checkCrc() {
        CRC32 crc32 = new CRC32();
        crc32.update(content);
        return crc32.getValue() == crc32Checksum;
    }

    public long getCreationTime() {
        return creationTime;
    }

    @Override
    public byte[] getContent() {
        return content;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getDigits() {
        return digits;
    }

    @Override
    public String getType() {
        return "BWG";
    }
}
