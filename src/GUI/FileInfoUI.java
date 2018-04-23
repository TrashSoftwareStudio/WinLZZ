package GUI;

import Packer.*;
import ResourcesPack.Languages.LanguageLoader;
import Utility.Bytes;
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
    private Label typeLabel, algLabel, versionNeededLabel, fileCountLabel, dirCountLabel, windowSizeLabel,
            compressRateLabel, origSizeLabel, compressSizeLabel, timeLabel, crcChecksumLabel;

    @FXML
    private ProgressBar compressRateBar;

    private LanguageLoader lanLoader;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    void setLanLoader(LanguageLoader lanLoader) {
        this.lanLoader = lanLoader;
    }

    void setInfo(File cmpFile, UnPacker unPacker) {
        this.unPacker = unPacker;
        this.cmpFile = cmpFile;
    }


    void setItems() {
        String prefix = unPacker.getEncryptLevel() == 0 ? "" : lanLoader.get(650) + " ";
        typeLabel.setText(prefix + "WinLZZ " + lanLoader.get(651));
        double rate = unPacker.getTotalOrigSize() == 0 ? 0 : (double) cmpFile.length() / unPacker.getTotalOrigSize();

        String alg;
        switch (unPacker.getAlg()) {
            case "lzz2":
                alg = "LZZ2";
                break;
            case "bwz":
                alg = "BWZ";
                break;
            default:
                alg = lanLoader.get(652);
                break;
        }

        compressRateBar.setProgress(rate);
        double roundedRate = ((double) Math.round(rate * 10000)) / 100.0;
        Date date = new Date(unPacker.getCreationTime());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        algLabel.setText(lanLoader.get(601) + ": " + alg);
        versionNeededLabel.setText(lanLoader.get(602) + ": " + translateVersion(unPacker.versionNeeded()));
        compressRateLabel.setText(lanLoader.get(603) + ": " + String.valueOf(roundedRate) + " %");
        windowSizeLabel.setText(lanLoader.get(604) + ": " + sizeToString3Digit(unPacker.getWindowSize()));
        origSizeLabel.setText(lanLoader.get(605) + ": " + Util.sizeToReadable(unPacker.getTotalOrigSize()));
        compressSizeLabel.setText(lanLoader.get(606) + ": " + Util.sizeToReadable(cmpFile.length()));
        fileCountLabel.setText(lanLoader.get(607) + ": " + splitNumber(String.valueOf(unPacker.getFileCount())));
        dirCountLabel.setText(lanLoader.get(608) + ": " + splitNumber(String.valueOf(unPacker.getDirCount())));
        timeLabel.setText(lanLoader.get(609) + ": " + sdf.format(date));
        crcChecksumLabel.setText(lanLoader.get(610) + ": " + Bytes.longToHex(unPacker.getCrc32Checksum(), false));
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
        else if (versionInt == 18) return "0.5.2";
        else if (versionInt == 20) return "0.6.0 - 0.6.1";
        else if (versionInt == 21) return "0.6.2+";
        else return lanLoader.get(652);
    }

    private String sizeToString3Digit(long src) {
        if (src < 1024) return src + " " + lanLoader.get(250);
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
