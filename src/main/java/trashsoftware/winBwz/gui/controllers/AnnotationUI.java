package trashsoftware.winBwz.gui.controllers;

import trashsoftware.winBwz.core.bwz.BWZCompressor;
import trashsoftware.winBwz.gui.graphicUtil.AnnotationNode;
import trashsoftware.winBwz.resourcesPack.configLoader.GeneralLoaders;
import trashsoftware.winBwz.resourcesPack.configLoader.LoaderManager;
import trashsoftware.winBwz.utility.Util;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ResourceBundle;

public class AnnotationUI implements Initializable {

    @FXML
    private Label warning;

    @FXML
    private ComboBox<HistorySelection> fileBox;

    @FXML
    private CheckBox compressAnnBox;

    @FXML
    private TextArea textArea;

    private Stage stage;
    private CompressUI parent;
    private ResourceBundle bundle;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.bundle = resources;
        fillBox();
        setFileBoxListener();
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
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter(
                bundle.getString("textFile"),
                "*.txt");
        fc.getExtensionFilters().add(filter);
        fc.setInitialDirectory(LoaderManager.getCacheSaver().readLastSelectedDir());
        File selected = fc.showOpenDialog(null);

        if (selected != null) {
            LoaderManager.getCacheSaver().addHistoryAnnotation(selected);
            fillBox();
            fileBox.getSelectionModel().select(0);
        }
    }

    @FXML
    void confirmAction() throws Exception {
        byte[] byteAnnotation = textArea.getText().getBytes(StandardCharsets.UTF_8);
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
            warning.setText(bundle.getString("annotationTooLong"));
        } else {
            parent.setAnnotation(node);
            stage.close();
        }
    }

    private void fillBox() {
        fileBox.getItems().clear();
        List<File> historyFiles = LoaderManager.getCacheSaver().getHistoryAnnotation();
        for (File f : historyFiles) fileBox.getItems().add(new HistorySelection(f));
    }

    private void addExistingAnnotation() {
        if (parent.getAnnotation() != null) {
            String s = new String(parent.getAnnotation().getAnnotation(), StandardCharsets.UTF_8);
            textArea.appendText(s);
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
}


class HistorySelection {

    private final File file;

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
