package BWGViewer.GUI;

import BWGViewer.Codecs.CommonLoader;
import WinLzz.ResourcesPack.Languages.LanguageLoader;
import WinLzz.Utility.Util;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;

public class ImageInfoUI implements Initializable {

    @FXML
    private Label nameText, typeText, widthText, heightText, bitsText, creationTimeText, modifiedTimeText, sizeText;

    @FXML
    private Label name, type, width, height, bits, creationTime, modifiedTime, size;

    private CommonLoader imageLoader;
    private File imageFile;
    private LanguageLoader lanLoader;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    public void setLanLoader(LanguageLoader lanLoader) {
        this.lanLoader = lanLoader;
        fillText();
    }

    void setInfo(CommonLoader imageLoader, File imageFile) {
        this.imageLoader = imageLoader;
        this.imageFile = imageFile;
    }

    void showInfo() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date creation = new Date(imageLoader.getCreationTime());
        Date modified = new Date(imageFile.lastModified());

        name.setText(imageFile.getName());
        if (imageLoader.getType().equals("BWG")) type.setText(lanLoader.get(1050));
        else if (imageLoader.getType().equals("BMP")) type.setText(lanLoader.get(1051));
        width.setText(String.valueOf(imageLoader.getWidth()));
        height.setText(String.valueOf(imageLoader.getHeight()));
        bits.setText(String.valueOf(imageLoader.getDigits()));
        if (imageLoader.getCreationTime() != 0) creationTime.setText(sdf.format(creation));
        else creationTime.setText("");
        modifiedTime.setText(sdf.format(modified));
        size.setText(Util.sizeToReadable(imageFile.length()));
    }

    private void fillText() {
        nameText.setText(lanLoader.get(1100));
        typeText.setText(lanLoader.get(1101));
        widthText.setText(lanLoader.get(1102));
        heightText.setText(lanLoader.get(1103));
        bitsText.setText(lanLoader.get(1104));
        creationTimeText.setText(lanLoader.get(1105));
        modifiedTimeText.setText(lanLoader.get(1106));
        sizeText.setText(lanLoader.get(1107));
    }
}
