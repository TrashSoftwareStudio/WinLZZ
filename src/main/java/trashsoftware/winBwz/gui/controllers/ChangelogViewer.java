package trashsoftware.winBwz.gui.controllers;

import trashsoftware.winBwz.resourcesPack.info.ChangelogReader;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ChangelogViewer implements Initializable {

    @FXML
    private Label changelogLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            setLabel();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setLabel() throws IOException {
        changelogLabel.setText(ChangelogReader.readChangelog());
    }
}
