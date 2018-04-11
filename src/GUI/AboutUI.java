package GUI;

import Packer.Packer;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;

public class AboutUI implements Initializable {

    @FXML
    private Label versionLabel;

    @FXML
    private Label coreVersionLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        versionLabel.setText(StartUI.version);
        coreVersionLabel.setText(String.valueOf(Packer.version));
    }
}
