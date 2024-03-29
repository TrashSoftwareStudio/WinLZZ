package trashsoftware.winBwz.gui.controllers;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import trashsoftware.winBwz.packer.*;
import trashsoftware.winBwz.utility.Bytes;
import trashsoftware.winBwz.utility.Util;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;

public class FileInfoUI implements Initializable {

    private UnPacker unPacker;

    @FXML
    private Label typeLabel, algLabel, versionLabel, versionNeededLabel, fileCountLabel, dirCountLabel, windowSizeLabel,
            compressRateLabel, netRateLabel, origSizeLabel, compressSizeLabel, annotationLabel, timeLabel,
            crcChecksumLabel, encryptionLabel, secretKeyLabel;

    @FXML
    private Canvas progressBarCanvas;

    private ResourceBundle bundle;

    private static final Color headColor = Color.GREEN;
    private static final Color otherInfoColor = Color.GOLD;
    private static final Color contextColor = Color.ORANGERED;
    private static final Color mainColor = Color.DODGERBLUE;

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
        double headRatio = (double) Packer.FIXED_HEAD_LENGTH / lengthBeforeCmp;
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
    }

    void setItems() {
        String prefix = unPacker.isSeparated() ? bundle.getString("multiSection") + " " : "";
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
            default:
                alg = bundle.getString("unknown");
                break;
        }

//        compressRateBar.setProgress(rate);
        drawCompressRate(unPacker.getTotalOrigSize(), unPacker.getOtherInfoLength(), unPacker.getContextLength(),
                unPacker.getCmpMainLength());

        double rate = unPacker.getTotalOrigSize() == 0 ? 0 : (double) unPacker.getDisplayArchiveLength() /
                unPacker.getTotalOrigSize();
        double roundedRate = ((double) Math.round(rate * 10000)) / 100.0;
        double netRate = (double) unPacker.getCmpMainLength() / unPacker.getTotalOrigSize();
        double roundedNetRate = ((double) Math.round(netRate * 10000)) / 100.0;
        Date date = new Date(unPacker.getCreationTime());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        algLabel.setText(": " + alg);
        versionLabel.setText(unPacker.getArchiveFullVersion());
        versionNeededLabel.setText(": " + translateVersion(unPacker.versionNeeded()));
        compressRateLabel.setText(": " + roundedRate + " %");
        netRateLabel.setText(": " + roundedNetRate + " %");
        windowSizeLabel.setText(": " + sizeToString3Digit(unPacker.getWindowSize()));
        origSizeLabel.setText(": " + Util.sizeToReadable(unPacker.getTotalOrigSize()));
        compressSizeLabel.setText(": " + Util.sizeToReadable(unPacker.getDisplayArchiveLength()));
        fileCountLabel.setText(": " + Util.splitNumber(String.valueOf(unPacker.getFileCount())));
        dirCountLabel.setText(": " + Util.splitNumber(String.valueOf(unPacker.getDirCount() - 1)));

//        headLabel.setText(lanLoader.get(618));
//        otherInfoLabel.setText(lanLoader.get(619));
//        contextLabel.setText(lanLoader.get(620));
//        mainLabel.setText(lanLoader.get(621));

        annotationLabel.setText(
                ": " + bundle.getString(unPacker.getAnnotation() == null ? "doesNotExist" : "exist"));

        String enc = ": ";
        String secretKey = ": ";
        if (unPacker.getEncryptLevel() == 0) {
            enc += bundle.getString("doesNotExist");
            secretKey += bundle.getString("doesNotExist");
        } else {
            enc += unPacker.getEncryption().toUpperCase();
            secretKey += unPacker.getPasswordAlg().toUpperCase();
        }
        encryptionLabel.setText(enc);
        secretKeyLabel.setText(secretKey);

        // There is one root directory created by packer.
        timeLabel.setText(": " + sdf.format(date));
        crcChecksumLabel.setText(": " + Bytes.longToHex(unPacker.getCrc32Checksum(), false));
    }


    private String translateVersion(byte versionInt) {
        if (versionInt == 1) return "0.1.2";
        else if (versionInt == 2) return "0.1.3";
        else if (versionInt == 3) return "0.1.4";
        else if (versionInt == 4) return "0.1.5 - 0.1.6";
        else if (versionInt == 5) return "0.1.7";
        else if (versionInt == 6) return "0.1.8";
        else if (versionInt == 7) return "0.1.9";
        else if (versionInt == 8) return "0.1.10";
        else if (versionInt == 9) return "0.2.3";
        else if (versionInt == 10) return "0.2.4 - 0.2.11";
        else if (versionInt == 11) return "0.3.0";
        else if (versionInt == 12) return "0.3.1";
        else if (versionInt == 14) return "0.4 - 0.4.1";
        else if (versionInt == 15) return "0.4.2";
        else if (versionInt == 16) return "0.4.3";
        else if (versionInt == 17) return "0.5.0 - 0.5.1";
        else if (versionInt == 18) return "0.5.2";
        else if (versionInt == 20) return "0.6.0 - 0.6.1";
        else if (versionInt == 21) return "0.6.2";
        else if (versionInt == 22) return "0.7+";
        else if (versionInt == 23) return "0.7.0 - 0.7.1";
        else if (versionInt == 24) return "0.7.2 - 0.7.3";
        else if (versionInt == 25) return "0.7.4+";
        else return bundle.getString("unknown");
    }

    private String sizeToString3Digit(long src) {
        if (src < 1024) return src + " " + bundle.getString("byte");
        else if (src < 1048576) return src / 1024 + " KB";
        else return src / 1048576 + " MB";
    }
}
