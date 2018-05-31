package WinLzz.GUI;

import WinLzz.ResourcesPack.Languages.LanguageLoader;
import WinLzz.Utility.Util;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;

public class ReplaceUI implements Initializable {

    @FXML
    private Label header, fromDir, existDir, name1, name2, time1, time2, size1, size2;

    @FXML
    private Button replace, skip, rename;

    private int result;

    private Stage stage;
    private LanguageLoader lanLoader;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    void setLanLoader(LanguageLoader lanLoader) {
        this.lanLoader = lanLoader;
        fillText();
    }

    void setStage(Stage stage) {
        this.stage = stage;
    }

    void setFiles(File existFile, File fromFile) {
        String path = Util.splitStringToLine(existFile.getAbsolutePath(), 30);
        header.setText(String.format("%s\n%s", path, lanLoader.get(83)));
        name1.setText(existFile.getName());
        name2.setText(fromFile.getName());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        time1.setText(sdf.format(new Date(existFile.lastModified())));
        time2.setText(sdf.format(new Date(fromFile.lastModified())));

        size1.setText(Util.sizeToReadable(existFile.length()));
        size2.setText(Util.sizeToReadable(fromFile.length()));
    }

    int getResult() {
        return result;
    }

    @FXML
    private void replaceAction() {
        result = 0;
        stage.close();
    }

    @FXML
    private void skipAction() {
        result = 1;
        stage.close();
    }

    @FXML
    private void renameAction() {
        result = 2;
        stage.close();
    }

    private void fillText() {
        replace.setText(lanLoader.get(80));
        skip.setText(lanLoader.get(81));
        rename.setText(lanLoader.get(82));

        existDir.setText(lanLoader.get(84));
        fromDir.setText(lanLoader.get(85));
    }
}
