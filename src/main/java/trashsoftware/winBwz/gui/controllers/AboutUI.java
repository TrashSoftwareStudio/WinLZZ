package trashsoftware.winBwz.gui.controllers;

import trashsoftware.winBwz.Main;
import trashsoftware.winBwz.packer.pz.PzPacker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;

public class AboutUI implements Initializable {

    @FXML
    private Label versionLabel, coreVersionLabel, trashSoftwareLabel, coreVersionTextLabel;

    private ResourceBundle bundle;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.bundle = resources;
        versionLabel.setText(Main.VERSION);
        coreVersionLabel.setText(PzPacker.getProgramFullVersion());
        fillText();
    }

    private void fillText() {
        trashSoftwareLabel.setText("(C) " + bundle.getString("trashSoftwareStudio"));
        coreVersionTextLabel.setText(bundle.getString("coreVersion") + ":");
    }
}
