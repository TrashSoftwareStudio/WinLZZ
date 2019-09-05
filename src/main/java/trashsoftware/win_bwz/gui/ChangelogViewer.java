package trashsoftware.win_bwz.gui;

import trashsoftware.win_bwz.resourcesPack.info.ChangelogReader;
import trashsoftware.win_bwz.resourcesPack.languages.LanguageLoader;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ChangelogViewer implements Initializable {

    @FXML
    private Label changelogLabel, changelogTextLabel;

    private LanguageLoader lanLoader;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            setLabel();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void setLanLoader(LanguageLoader lanLoader) {
        this.lanLoader = lanLoader;
        fillText();
    }

    private void setLabel() throws IOException {
        changelogLabel.setText(new ChangelogReader().readChangelog());
    }

    private void fillText() {
        changelogTextLabel.setText(lanLoader.get(800));
    }
}
