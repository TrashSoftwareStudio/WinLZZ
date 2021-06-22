package trashsoftware.winBwz.gui.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import trashsoftware.winBwz.packer.UnPacker;
import trashsoftware.winBwz.packer.pz.PzSolidPacker;
import trashsoftware.winBwz.packer.pz.PzUnPacker;
import trashsoftware.winBwz.packer.pzNonSolid.PzNsUnPacker;
import trashsoftware.winBwz.packer.zip.ZipUnPacker;
import trashsoftware.winBwz.utility.Bytes;
import trashsoftware.winBwz.utility.Util;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;

public class FileInfoUI implements Initializable {

    private static final Color headColor = Color.GREEN;
    private static final Color otherInfoColor = Color.GOLD;
    private static final Color contextColor = Color.ORANGERED;
    private static final Color mainColor = Color.DODGERBLUE;
    private UnPacker unPacker;
    @FXML
    private Label typeLabel, algLabel, versionLabel, versionNeededLabel, fileCountLabel, dirCountLabel, windowSizeLabel,
            compressRateLabel, netRateLabel, origSizeLabel, compressSizeLabel, annotationLabel, timeLabel,
            crcChecksumLabel, encryptionLabel, secretKeyLabel;
    @FXML
    private Canvas progressBarCanvas;
    private ResourceBundle bundle;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        bundle = resources;
        drawProgressCanvas();
    }

    private void drawProgressCanvas() {
        GraphicsContext graphicsContext = progressBarCanvas.getGraphicsContext2D();
        graphicsContext.setStroke(Color.BLACK);
        graphicsContext.setLineWidth(4);
        graphicsContext.strokeRect(0, 0, 400, 30);
    }

    private void drawCompressRate(long lengthBeforeCmp, long otherInfoLen, long contextLen, long mainLen) {
        double headRatio = (double) PzSolidPacker.FIXED_HEAD_LENGTH / lengthBeforeCmp;
        double otherInfoRatio = (double) otherInfoLen / lengthBeforeCmp;
        double contextRatio = (double) contextLen / lengthBeforeCmp;
        double mainRatio = (double) mainLen / lengthBeforeCmp;

        double totalPixelLen = 390;

        double healPixel = headRatio * totalPixelLen;
        double otherInfoPixel = otherInfoRatio * totalPixelLen;
        double contextPixel = contextRatio * totalPixelLen;
        double mainPixel = mainRatio * totalPixelLen;

        double current = 5;

        GraphicsContext graphicsContext = progressBarCanvas.getGraphicsContext2D();
        graphicsContext.setFill(headColor);
        graphicsContext.fillRect(current, 5, healPixel, 20);
        current += healPixel;
        graphicsContext.setFill(otherInfoColor);
        graphicsContext.fillRect(current, 5, otherInfoPixel, 20);
        current += otherInfoPixel;
        graphicsContext.setFill(contextColor);
        graphicsContext.fillRect(current, 5, contextPixel, 20);
        current += contextPixel;
        graphicsContext.setFill(mainColor);
        graphicsContext.fillRect(current, 5, mainPixel, 20);
    }

    void setInfo(UnPacker unPacker) {
        this.unPacker = unPacker;
        if (unPacker instanceof PzUnPacker) setItemsPz();
        else if (unPacker instanceof ZipUnPacker) setItemsZip();
        else throw new RuntimeException("No such unpacker");
    }

    private void setItemsPz() {
        PzUnPacker pzUnPacker = (PzUnPacker) unPacker;

        String prefix = unPacker.isSeparated() ? bundle.getString("multiSection") + " " : "";
        prefix += (unPacker instanceof PzNsUnPacker) ? bundle.getString("nonSolid") + " ": "";
        typeLabel.setText(prefix + "WinLZZ " + bundle.getString("archive"));

        String alg;
        switch (unPacker.getAlg()) {
            case "lzz2":
                alg = "LZZ2";
                break;
            case "fastLzz":
                alg = "FastLZZ";
                break;
            case "bwz":
                alg = "BWZ";
                break;
            case "deflate":
                alg = "Deflate";
                break;
            default:
                alg = bundle.getString("unknown");
                break;
        }

//        compressRateBar.setProgress(rate);
        drawCompressRate(pzUnPacker.getTotalOrigSize(), pzUnPacker.getOtherInfoLength(), pzUnPacker.getContextLength(),
                pzUnPacker.getCmpMainLength());

        double rate = unPacker.getTotalOrigSize() == 0 ? 0 : (double) unPacker.getDisplayArchiveLength() /
                unPacker.getTotalOrigSize();
        double roundedRate = ((double) Math.round(rate * 10000)) / 100.0;
        double netRate = (double) pzUnPacker.getCmpMainLength() / unPacker.getTotalOrigSize();
        double roundedNetRate = ((double) Math.round(netRate * 10000)) / 100.0;
        Date date = new Date(unPacker.getCreationTime());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        algLabel.setText(": " + alg);
        versionLabel.setText(": " + pzUnPacker.getArchiveFullVersion());
        versionNeededLabel.setText(": " + translateVersion(pzUnPacker.versionNeeded()));
        compressRateLabel.setText(": " + roundedRate + " %");
        netRateLabel.setText(": " + roundedNetRate + " %");
        windowSizeLabel.setText(": " + sizeToString3Digit(pzUnPacker.getWindowSize()));
        origSizeLabel.setText(": " + Util.sizeToReadable(unPacker.getTotalOrigSize()));
        compressSizeLabel.setText(": " + Util.sizeToReadable(unPacker.getDisplayArchiveLength()));
        fileCountLabel.setText(": " + Util.splitNumber(String.valueOf(unPacker.getFileCount())));
        dirCountLabel.setText(": " + Util.splitNumber(String.valueOf(unPacker.getDirCount() - 1)));

        annotationLabel.setText(
                ": " + bundle.getString(unPacker.getAnnotation() == null ? "doesNotExist" : "exist"));

        String enc = ": ";
        String secretKey = ": ";
        if (unPacker.getEncryptLevel() == 0) {
            enc += bundle.getString("doesNotExist");
            secretKey += bundle.getString("doesNotExist");
        } else {
            enc += pzUnPacker.getEncryption().toUpperCase();
            secretKey += pzUnPacker.getPasswordAlg().toUpperCase();
        }
        encryptionLabel.setText(enc);
        secretKeyLabel.setText(secretKey);

        // There is one root directory created by packer.
        timeLabel.setText(": " + sdf.format(date));
        crcChecksumLabel.setText(": " + Bytes.longToHex(pzUnPacker.getCrc32Checksum(), false));
    }

    private void setItemsZip() {
        typeLabel.setText("ZIP " + bundle.getString("archive"));
        versionNeededLabel.setText(": 1.0 Alpha 14+");

        ZipUnPacker zup = (ZipUnPacker) unPacker;
        long headerLen = zup.getDisplayArchiveLength() - zup.getNetCompressedSize();
        drawCompressRate(zup.getTotalOrigSize(),
                0,
                headerLen,
                zup.getNetCompressedSize());
        double rate = zup.getTotalOrigSize() == 0 ? 0 : (double) zup.getDisplayArchiveLength() /
                unPacker.getTotalOrigSize();
        double roundedRate = ((double) Math.round(rate * 10000)) / 100.0;
        double netRate = (double) zup.getNetCompressedSize() / unPacker.getTotalOrigSize();
        double roundedNetRate = ((double) Math.round(netRate * 10000)) / 100.0;
        Date date = new Date(unPacker.getCreationTime());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        compressRateLabel.setText(": " + roundedRate + " %");
        netRateLabel.setText(": " + roundedNetRate + " %");
        origSizeLabel.setText(": " + Util.sizeToReadable(unPacker.getTotalOrigSize()));
        compressSizeLabel.setText(": " + Util.sizeToReadable(unPacker.getDisplayArchiveLength()));
        fileCountLabel.setText(": " + Util.splitNumber(String.valueOf(unPacker.getFileCount())));
        dirCountLabel.setText(": " + Util.splitNumber(String.valueOf(unPacker.getDirCount() - 1)));

        annotationLabel.setText(
                ": " + bundle.getString(unPacker.getAnnotation() == null ? "doesNotExist" : "exist"));

        timeLabel.setText(": " + sdf.format(date));
    }

    private String translateVersion(byte versionInt) {
        switch (versionInt & 0xff) {
            case 1:
                return "0.1.2";
            case 2:
                return "0.1.3";
            case 3:
                return "0.1.4";
            case 4:
                return "0.1.5 - 0.1.6";
            case 5:
                return "0.1.7";
            case 6:
                return "0.1.8";
            case 7:
                return "0.1.9";
            case 8:
                return "0.1.10";
            case 9:
                return "0.2.3";
            case 10:
                return "0.2.4 - 0.2.11";
            case 11:
                return "0.3.0";
            case 12:
                return "0.3.1";
            case 14:
                return "0.4 - 0.4.1";
            case 15:
                return "0.4.2";
            case 16:
                return "0.4.3";
            case 17:
                return "0.5.0 - 0.5.1";
            case 18:
                return "0.5.2";
            case 20:
                return "0.6.0 - 0.6.1";
            case 21:
                return "0.6.2";
            case 22:
                return "0.7";
            case 23:
                return "0.7.0 - 0.7.1";
            case 24:
                return "0.7.2 - 0.7.3";
            case 25:
                return "1.0 Alpha+";
            case 26:
                return "1.0 Alpha 12+";
            case 27:
                return "1.0 Alpha 15+";
            case 28:
                return "1.0 Alpha 16+";
            default:
                return bundle.getString("unknown");
        }
    }

    private String sizeToString3Digit(long src) {
        if (src < 1024) return src + " " + bundle.getString("byte");
        else if (src < 1048576) return src / 1024 + " KB";
        else return src / 1048576 + " MB";
    }
}
