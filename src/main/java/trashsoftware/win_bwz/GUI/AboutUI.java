package trashsoftware.win_bwz.GUI;

import trashsoftware.win_bwz.Main;
import trashsoftware.win_bwz.Packer.Packer;
import trashsoftware.win_bwz.ResourcesPack.Languages.LanguageLoader;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;

public class AboutUI implements Initializable {

    @FXML
    private Label versionLabel, coreVersionLabel, trashSoftwareLabel, coreVersionTextLabel;

    private LanguageLoader lanLoader;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        versionLabel.setText(Main.version);
        coreVersionLabel.setText(String.format("%d.%d", Packer.primaryVersion & 0xff, Packer.secondaryVersion & 0xff));
    }

    void setLanLoader(LanguageLoader lanLoader) {
        this.lanLoader = lanLoader;
        fillText();
    }

    private void fillText() {
        trashSoftwareLabel.setText("(C) " + lanLoader.get(700));
        coreVersionTextLabel.setText(lanLoader.get(701) + ":");
    }
}
