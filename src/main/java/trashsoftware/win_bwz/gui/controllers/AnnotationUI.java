package trashsoftware.win_bwz.gui.controllers;

import trashsoftware.win_bwz.core.bwz.BWZCompressor;
import trashsoftware.win_bwz.gui.graphicUtil.AnnotationNode;
import trashsoftware.win_bwz.resourcesPack.configLoader.GeneralLoaders;
import trashsoftware.win_bwz.resourcesPack.languages.LanguageLoader;
import trashsoftware.win_bwz.utility.Util;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AnnotationUI implements Initializable {

    @FXML
    private Label fromFile, fromText, warning;

    @FXML
    private Button browseButton, confirmButton;

    @FXML
    private ComboBox<HistorySelection> fileBox;

    @FXML
    private CheckBox compressAnnBox;

    @FXML
    private TextArea textArea;

    private LanguageLoader lanLoader;
    private Stage stage;
    private CompressUI parent;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        fillBox();
        setFileBoxListener();
    }

    void setLanLoader(LanguageLoader lanLoader) {
        this.lanLoader = lanLoader;
        fillText();
    }

    void setStage(Stage stage) {
        this.stage = stage;
    }

    void setParent(CompressUI parent) {
        this.parent = parent;
        addExistingAnnotation();
    }

    @FXML
    void browseAction() {
        FileChooser fc = new FileChooser();
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter(lanLoader.get(904), "*.txt");
        fc.getExtensionFilters().add(filter);
        fc.setInitialDirectory(GeneralLoaders.readLastDir());
        File selected = fc.showOpenDialog(null);

        if (selected != null) {
            GeneralLoaders.addHistoryAnnotation(selected);
            fillBox();
            fileBox.getSelectionModel().select(0);
        }
    }

    @FXML
    void confirmAction() throws Exception {
        byte[] byteAnnotation = textArea.getText().getBytes("utf-8");
        AnnotationNode node;
        if (compressAnnBox.isSelected()) {
            ByteArrayInputStream ais = new ByteArrayInputStream(byteAnnotation);

            BWZCompressor compressor = new BWZCompressor(ais, 32768);
            compressor.setCompressionLevel(1);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            compressor.compress(out);
            node = new AnnotationNode(out.toByteArray(), true);
            out.flush();
            out.close();
        } else {
            node = new AnnotationNode(byteAnnotation, false);
        }

        if (node.getAnnotation().length >= 32764) {  // Annotation too long
            warning.setText(lanLoader.get(905));
        } else {
            parent.setAnnotation(node);
            stage.close();
        }
    }

    private void fillBox() {
        fileBox.getItems().clear();
        List<File> historyFiles = GeneralLoaders.getHistoryAnnotation();
        for (File f : historyFiles) fileBox.getItems().add(new HistorySelection(f));
    }

    private void addExistingAnnotation() {
        if (parent.getAnnotation() != null) {
            try {
                String s = new String(parent.getAnnotation().getAnnotation(), "utf-8");
                textArea.appendText(s);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadFile(HistorySelection file) throws IOException {
        textArea.clear();
        textArea.appendText(Util.readTextFile(file.getFile()));
    }

    private void setFileBoxListener() {
        fileBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                try {
                    loadFile(newValue);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void fillText() {
        fromFile.setText(lanLoader.get(901));
        fromText.setText(lanLoader.get(903));
        browseButton.setText(lanLoader.get(902));
        confirmButton.setText(lanLoader.get(1));
        compressAnnBox.setText(lanLoader.get(906));
    }
}


class HistorySelection {

    private File file;

    HistorySelection(File file) {
        this.file = file;
    }

    File getFile() {
        return file;
    }

    @Override
    public String toString() {
        return file.getName();
    }
}
