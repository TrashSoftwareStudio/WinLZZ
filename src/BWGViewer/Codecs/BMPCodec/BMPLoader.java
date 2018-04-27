package BWGViewer.Codecs.BMPCodec;

import BWGViewer.Codecs.CommonLoader;
import BWGViewer.Codecs.UnsupportedFormatException;
import WinLzz.Utility.Bytes;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class BMPLoader implements CommonLoader {

    private byte[] content;
    private String path;
    private int width, height, digits;

    public BMPLoader(String path) {
        this.path = path;
    }

    @Override
    public void load() throws IOException, UnsupportedFormatException {
        File f = new File(path);
        long fileLength = f.length();
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(path));
        if (bis.skip(10) != 10) throw new IOException("Error occurs while reading");
        byte[] bytes = new byte[4];
        if (bis.read(bytes) != 4) throw new IOException("Error occurs while reading");
        int offset = Bytes.bytesToInt32Little(bytes);

        if (bis.skip(4) != 4) throw new IOException("Error occurs while reading");
        if (bis.read(bytes) != 4) throw new IOException("Error occurs while reading");
        width = Bytes.bytesToInt32Little(bytes);
        if (bis.read(bytes) != 4) throw new IOException("Error occurs while reading");
        height = Bytes.bytesToInt32Little(bytes);

        if (bis.skip(2) != 2) throw new IOException("Error occurs while reading");
        byte[] bitCountBytes = new byte[2];
        if (bis.read(bitCountBytes) != 2) throw new IOException("Error occurs while reading");
        digits = Bytes.bytesToShortLittle(bitCountBytes);
        if (digits != 24) throw new UnsupportedFormatException();

        int skip = offset - 30;
        if (bis.skip(skip) != skip) throw new IOException("Error occurs while reading");

        int bitmapLength = (int) fileLength - offset;
        content = new byte[bitmapLength];
        if (bis.read(content) != bitmapLength) throw new IOException("Error occurs while reading");

        bis.close();
    }

    @Override
    public byte[] getContent() {
        return content;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getDigits() {
        return digits;
    }

    @Override
    public long getCreationTime() {
        return 0;
    }

    @Override
    public String getType() {
        return "BMP";
    }
}
