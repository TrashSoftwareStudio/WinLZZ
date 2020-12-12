package trashsoftware.winBwz.gui.controllers;

import trashsoftware.winBwz.utility.Util;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;

public class ReplaceUI implements Initializable {

    @FXML
    private Label header, name1, name2, time1, time2, size1, size2;

    private int result;

    private Stage stage;
    private ResourceBundle bundle;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        bundle = resources;
    }

    void setStage(Stage stage) {
        this.stage = stage;
    }

    void setFiles(File existFile, File fromFile) {
        String path = Util.splitStringToLine(existFile.getAbsolutePath(), 30);
        header.setText(String.format("%s\n%s", path, bundle.getString("existContinuePaste")));
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
}
