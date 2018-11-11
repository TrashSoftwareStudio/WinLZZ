package WinLzz.GUI;

import WinLzz.GraphicUtil.InfoNode;
import WinLzz.ResourcesPack.Languages.LanguageLoader;
import WinLzz.Utility.Util;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.RowConstraints;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ResourceBundle;

public class FilePropertiesUI implements Initializable {

    private InfoNode file;
    private LanguageLoader lanLoader;

    @FXML
    private Label dirText, typeText, sizeText, containText, cTimeText, mTimeText, aTimeText;

    @FXML
    private Label name, dir, type, size, fileCount, dirCount, cTime, mTime, aTime;

    @FXML
    private Separator sep2;

    @FXML
    private RowConstraints typeRow, containsRow, sepRow2, timeRow1, timeRow2, timeRow3;

    private Service<Void> service;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    void setFiles(InfoNode file) {
        this.file = file;
    }

    void setLanLoader(LanguageLoader lanLoader) {
        this.lanLoader = lanLoader;
        fillText();
    }

    void display() throws IOException {
        file.readInfo();
        if (file.isSingle()) {
            singleDisplay();
        } else {
            multiDisplay();
        }
    }

    void interrupt() {
        service.cancel();
    }

    private void singleDisplay() {
        String fileName = file.getFile().getName();
        if (fileName.length() == 0) {
            name.setText(file.getFile().getAbsolutePath());
            dir.setText(lanLoader.get(861));
            type.setText(lanLoader.get(26));
        } else {
            name.setText(file.getFile().getName());
            String parentDir = Util.splitStringToLine(file.getFile().getParent(), 30);
            dir.setText(parentDir);
            type.setText(displayType(file));
        }

        if (!file.getFile().isDirectory()) {
            hide(containText);
            hide(fileCount);
            hide(dirCount);
            containsRow.setPrefHeight(0.0);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        cTime.setText(sdf.format(file.getCreationTime()));
        mTime.setText(sdf.format(file.getModifiedTime()));
        aTime.setText(sdf.format(file.getAccessTime()));

        count();
    }

    private void multiDisplay() {
        hide(sep2);
        hide(cTimeText);
        hide(cTime);
        hide(mTimeText);
        hide(mTime);
        hide(aTimeText);
        hide(aTime);
        hide(typeText);
        hide(type);

        typeRow.setPrefHeight(0.0);
        sepRow2.setPrefHeight(0.0);
        timeRow1.setPrefHeight(0.0);
        timeRow2.setPrefHeight(0.0);
        timeRow3.setPrefHeight(0.0);

        String basicName = file.getFile().getName();
        if (basicName.length() == 0) {
            name.setText(String.format("%s %s%s", file.getFile().getAbsolutePath(), lanLoader.get(850),
                    lanLoader.get(862)));
            dir.setText(lanLoader.get(861));
        } else {
            name.setText(String.format("%s %s%s", file.getFile().getName(), lanLoader.get(850), lanLoader.get(851)));
            String parentDir = Util.splitStringToLine(file.getFile().getParent(), 30);
            dir.setText(parentDir);
        }

        count();
    }

    private static void hide(Node node) {
        node.setVisible(false);
        node.setManaged(false);
    }

    private void count() {
        service = new Service<Void>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<Void>() {
                    @Override
                    protected Void call() {
                        ChangeListener<Number> sizeListener = (observable, oldValue, newValue) ->
                                Platform.runLater(() -> {
                                    String sizeR = String.format("%s (%s %s)", Util.sizeToReadable(newValue.longValue()),
                                            Util.splitNumber(String.valueOf(newValue.longValue())), lanLoader.get(250));
                                    size.setText(sizeR);
                                });

                        ChangeListener<Number> fileCountListener = (observable, oldValue, newValue) ->
                                Platform.runLater(() -> {
                                    String content = String.format("%s %s, ", Util.splitNumber(String.valueOf(newValue)),
                                            lanLoader.get(864));
                                    fileCount.setText(content);
                                });

                        ChangeListener<Number> dirCountListener = (observable, oldValue, newValue) ->
                                Platform.runLater(() -> {
                                    String content = String.format("%s %s", Util.splitNumber(String.valueOf(newValue)),
                                            lanLoader.get(860));
                                    dirCount.setText(content);
                                });

                        file.sizeProperty().addListener(sizeListener);
                        file.fileCountProperty().addListener(fileCountListener);
                        file.dirCountProperty().addListener(dirCountListener);

                        file.initializeProperties();

                        file.readDirs();
                        return null;
                    }
                };
            }
        };
        service.setOnFailed(e -> e.getSource().getException().printStackTrace());
        service.setOnCancelled(e -> file.interrupt());
        service.start();
    }

    private String displayType(InfoNode node) {
        String s = node.getType();
        if (s == null) return lanLoader.get(25);
        else return s + lanLoader.get(24);
    }

    private void fillText() {
        dirText.setText(lanLoader.get(853));
        typeText.setText(lanLoader.get(852));
        sizeText.setText(lanLoader.get(854));
        containText.setText(lanLoader.get(856));
        cTimeText.setText(lanLoader.get(857));
        mTimeText.setText(lanLoader.get(858));
        aTimeText.setText(lanLoader.get(859));
    }
}
