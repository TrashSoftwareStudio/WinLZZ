package GUI;

import Packer.*;
import Utility.Util;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;

public class FileInfoUI implements Initializable {

    private File cmpFile;

    private UnPacker unPacker;

    @FXML
    private Label typeLabel;

    @FXML
    private Label algLabel;

    @FXML
    private Label versionNeededLabel;

    @FXML
    private Label fileCountLabel;

    @FXML
    private Label dirCountLabel;

    @FXML
    private Label windowSizeLabel;

    @FXML
    private Label compressRateLabel;

    @FXML
    private Label origSizeLabel;

    @FXML
    private Label compressSizeLabel;

    @FXML
    private Label timeLabel;

    @FXML
    private ProgressBar compressRateBar;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    void setInfo(File cmpFile, UnPacker unPacker) {
        this.unPacker = unPacker;
        this.cmpFile = cmpFile;
    }


    void setItems() {
        String prefix = unPacker.getEncryptLevel() == 0 ? "" : "加密的 ";
        typeLabel.setText(prefix + "WinLZZ 压缩文件");
        double rate = unPacker.getTotalOrigSize() == 0 ? 0 : (double) cmpFile.length() / unPacker.getTotalOrigSize();

        String alg;
        switch (unPacker.getAlg()) {
            case "lzz2":
                alg = "LZZ2";
                break;
            case "qlz":
                alg = "QuickLZZ";
                break;
            case "bwz":
                alg = "BWZ";
                break;
            default:
                alg = "未知";
                break;
        }

        compressRateBar.setProgress(rate);
        double roundedRate = ((double) Math.round(rate * 10000)) / 100.0;
        Date date = new Date(cmpFile.lastModified());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        algLabel.setText("压缩方法: " + alg);
        compressRateLabel.setText("压缩率: " + String.valueOf(roundedRate) + " %");
        versionNeededLabel.setText("解压所需WinLZZ版本: " + translateVersion(unPacker.versionNeeded()));
        origSizeLabel.setText("原文件大小: " + Util.sizeToReadable(unPacker.getTotalOrigSize()));
        compressSizeLabel.setText("压缩后大小: " + Util.sizeToReadable(cmpFile.length()));
        fileCountLabel.setText("文件总数: " + splitNumber(String.valueOf(unPacker.getFileCount())));
        dirCountLabel.setText("文件夹总数: " + splitNumber(String.valueOf(unPacker.getDirCount())));
        windowSizeLabel.setText("字典大小: " + sizeToString3Digit(unPacker.getWindowSize()));
        timeLabel.setText("创建时间: " + sdf.format(date));
    }


    private String translateVersion(short versionInt) {
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
        else if (versionInt == 18) return "0.5.2+";
        else return "未知";
    }

    private static String sizeToString3Digit(long src) {
        if (src < 1024) return src + " 字节";
        else if (src < 1048576) return src / 1024 + " KB";
        else return src / 1048576 + " MB";
    }

    private static String splitNumber(String src) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = src.length() - 1; i >= 0; i--) {
            if (count % 3 == 0 && count != 0) sb.append(",");
            sb.append(src.charAt(i));
            count += 1;
        }
        return sb.reverse().toString();
    }
}
