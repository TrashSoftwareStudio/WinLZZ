package trashsoftware.trashGraphics.core;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ImageViewer {

    public static final String[] ALL_FORMATS_READ = {"bmp", "jpg", "png", "tgi"};

    private List<GraphicLayer> graphicLayers = new ArrayList<>();

    public ImageViewer() {

    }

    public boolean addLayer(String imageFileName) {
        try {
            GraphicLayer layer = createLayer(imageFileName);
            graphicLayers.add(layer);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void show(ImageView imageView) throws IOException {
        BufferedImage mattedImage = getMattedImage();
        imageView.setImage(SwingFXUtils.toFXImage(mattedImage, null));
    }

    public BufferedImage getMattedImage() throws IOException {
        byte[] imageData = getComposedBgrData();
        byte[] composedHeader = graphicLayers.get(0).getHeader();
        byte[] composedImage = new byte[composedHeader.length + imageData.length];
        System.arraycopy(composedHeader, 0, composedImage, 0, composedHeader.length);
        System.arraycopy(imageData, 0, composedImage, composedHeader.length, imageData.length);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(composedImage);
        BufferedImage resultImage = ImageIO.read(inputStream);
        inputStream.close();
        return resultImage;
    }

    public byte[] getComposedBgrData() {
        int backgroundWidth = getBaseWidth();
        int backgroundHeight = getBaseHeight();

        int dimension = backgroundWidth * backgroundHeight;
        byte[] imageData = new byte[dimension * 3];

        for (GraphicLayer graphicLayer : graphicLayers) {
            if (graphicLayer.hasAlpha()) {
                byte[] alphaChannel = graphicLayer.getAlphaChannel();
                byte[] layerData = graphicLayer.getBgrData();
                for (int i = 0; i < dimension; ++i) {
                    double alphaValue = (double) (alphaChannel[i] & 0xff) / 256;
                    int fr = layerData[i * 3] & 0xff;
                    int fg = layerData[i * 3 + 1] & 0xff;
                    int fb = layerData[i * 3 + 2] & 0xff;
                    int br = imageData[i * 3] & 0xff;
                    int bg = imageData[i * 3 + 1] & 0xff;
                    int bb = imageData[i * 3 + 2] & 0xff;
                    byte compR = (byte) (alphaValue * fr + (1 - alphaValue) * br);
                    byte compG = (byte) (alphaValue * fg + (1 - alphaValue) * bg);
                    byte compB = (byte) (alphaValue * fb + (1 - alphaValue) * bb);
                    imageData[i * 3] = compR;
                    imageData[i * 3 + 1] = compG;
                    imageData[i * 3 + 2] = compB;
                }
            } else {
                System.arraycopy(graphicLayer.getBgrData(), 0, imageData, 0, dimension * 3);
            }
        }
        return imageData;
    }

    public int getBaseWidth() {
        return graphicLayers.get(0).getWidth();
    }

    public int getBaseHeight() {
        return graphicLayers.get(0).getHeight();
    }

    private int maxWidth() {
        int max = 0;
        for (GraphicLayer graphicLayer : graphicLayers) {
            int width = graphicLayer.getWidth();
            if (width > max) max = width;
        }
        return max;
    }

    private int maxHeight() {
        int max = 0;
        for (GraphicLayer graphicLayer : graphicLayers) {
            int height = graphicLayer.getHeight();
            if (height > max) max = height;
        }
        return max;
    }

    public static boolean produceThumbnail(String origImageName, String thumbnailName, int width) {
        FileInputStream fis;
        FileOutputStream fos;
        try {
            fis = new FileInputStream(origImageName);
            fos = new FileOutputStream(thumbnailName);

            BufferedImage image = readImage(origImageName, fis);

            BufferedImage resized = resizeImage(image, width);

            boolean suc = ImageIO.write(resized, "bmp", fos);

            fis.close();
            fos.close();

            return suc;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return false;
        }
    }

    private static BufferedImage readImage(String origImageName, FileInputStream fis) throws IOException {
        int dotIndex = origImageName.lastIndexOf('.');
        if (dotIndex == -1) throw new IOException("Cannot identify image file");
        String ext = origImageName.substring(dotIndex + 1);
        BufferedImage image;
        if (ext.equals("tgi")) {
            try {
                image = TgiDecoder.readAsBitmap(fis, origImageName);
            } catch (Exception e) {
                throw new IOException(e);
            }
        } else {
            image = ImageIO.read(fis);
            if (image == null) throw new IOException("Unsupported image");
        }
        return image;
    }

    private static GraphicLayer createLayer(String imageFileName) throws IOException {
        FileInputStream fis = new FileInputStream(imageFileName);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        BufferedImage image = readImage(imageFileName, fis);

        // TODO: if not base layer, extend to base width and height

        boolean hasAlpha = false;
        byte[] alphaChannel = null;
        if (image.getType() == BufferedImage.TYPE_4BYTE_ABGR) {
            // TODO: extract alpha channel
            hasAlpha = true;
        }

        BufferedImage toWrite = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D graphics2D = toWrite.createGraphics();
        graphics2D.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
        graphics2D.dispose();
        boolean suc = ImageIO.write(toWrite, "bmp", bos);

        fis.close();
        if (!suc) throw new IOException("Cannot create image");
        bos.flush();
        GraphicLayer graphicLayer = new GraphicLayer(bos.toByteArray(), hasAlpha, alphaChannel);
        bos.close();  // this step is unnecessary but reserved for standard
        return graphicLayer;
    }

    private static BufferedImage resizeImage(BufferedImage origImage, int width) {
        int origWidth = origImage.getWidth();
        int origHeight = origImage.getHeight();
        double ratio = (double) width / origWidth;
        int newHeight = (int) (ratio * origHeight);
        BufferedImage resized = new BufferedImage(width, newHeight, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D graphics2D = resized.createGraphics();
        graphics2D.drawImage(origImage, 0, 0, width, newHeight, null);
        graphics2D.dispose();

        return resized;
    }
}
