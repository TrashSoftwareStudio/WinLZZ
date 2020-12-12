package trashsoftware.trashGraphics.core;

import trashsoftware.winBwz.utility.Bytes;

public class GraphicLayer {

    private int width;
    private int height;
    private byte[] fullData;
    private byte[] header;
    private byte[] bgrData;

    private boolean hasAlpha;
    private byte[] alphaChannel;

    private int dataType;

    public GraphicLayer(byte[] fullData, boolean hasAlpha, byte[] alphaChannel) {
        this.hasAlpha = hasAlpha;
        this.alphaChannel = alphaChannel;
        this.fullData = fullData;

        int offset = Bytes.bytesToInt32Little(fullData, 10);
        int dataSize = fullData.length - offset;
        bgrData = new byte[dataSize];
        header = new byte[offset];

        width = Bytes.bytesToInt32Little(fullData, 18);
        height = Bytes.bytesToInt32Little(fullData, 22);
        int depth = Bytes.bytesToInt32Little(fullData, 28);
        assert depth == 24;

        System.arraycopy(fullData, 0, header, 0, offset);
        System.arraycopy(fullData, offset, bgrData, 0, dataSize);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public byte[] getHeader() {
        return header;
    }

    public byte[] getBgrData() {
        return bgrData;
    }

    public byte[] getAlphaChannel() {
        return alphaChannel;
    }

    public boolean hasAlpha() {
        return hasAlpha;
    }

    public void antiColor() {
        for (int i = 0; i < bgrData.length; ++i) {
            bgrData[i] = (byte) (255 - bgrData[i] & 0xff);
        }
    }

    //    public ByteArrayInputStream getBitmapAsStream() {
//        ByteArrayInputStream inputStream = new ByteArrayInputStream(header);
//
//    }
}
