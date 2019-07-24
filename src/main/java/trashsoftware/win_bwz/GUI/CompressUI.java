package trashsoftware.win_bwz.GUI;

import trashsoftware.win_bwz.GraphicUtil.AnnotationNode;
import trashsoftware.win_bwz.ResourcesPack.Languages.LanguageLoader;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class CompressUI implements Initializable {

    @FXML
    private TextField nameText;

    @FXML
    private ComboBox<String> algBox, levelBox, windowNameBox, modeBox, partialBox, unitBox;

    @FXML
    private ComboBox<Integer> bufferBox, threadBox;

    @FXML
    private Label nameLabel, algLabel, levelLabel, windowLabel, bufferLabel, strongModeLabel, threadLabel, partialLabel;

    @FXML
    private Button startCompressButton, passwordButton, annotationButton;

    private File[] rootDir;
    private Stage pStage;
    private String password;
    private String encAlg;
    private String passAlg;
    private AnnotationNode annotation;

    private int encryptLevel = 0;
    private String[] algNames = new String[]{"BWZ", "LZZ2", "LZZ2+"};
    private String[] algValues = new String[]{"bwz", "lzz2", "lzz2p"};
    private String[] compressionLevels = new String[6];
    private String[] windowsSizeNames = new String[]{"4KB", "16KB", "32KB", "64KB", "128KB", "256KB", "1MB"};
    private String[] windowsSizeNames2 = new String[]{"128KB", "256KB", "512KB", "1MB", "2MB", "4MB", "8MB", "16MB"};
    private String[] splitSizeNames = new String[]{"1.44 MB - Floppy", "10 MB", "650 MB - CD", "700 MB - CD",
            "4095 MB - FAT32", "4481 MB - DVD"};

    private int[] windowSizes = new int[]{4096, 16384, 32768, 65536, 131072, 262144, 1048576};
    // Window sizes of LZZ2.

    private int[] windowSizes2 = new int[]{131072, 262144, 524288, 1048576, 2097152, 4194304, 8388608, 16777216};
    // Window sizes of BWT.

    /**
     * Indices of units of corresponding pre-selections, 0 for byte, 1 for kb, 2 for mb, 3 for gb.
     */
    private int[] splitUnits = new int[]{2, 2, 2, 2, 2, 2};

    private Integer[] bufferSizes = new Integer[]{8, 16, 32, 64, 128, 256, 286};

    private String[] cmpLevels = new String[5];
    private Integer[] threads = new Integer[]{1, 2, 3, 4};

    private MainUI parent;
    private LanguageLoader lanLoader;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    void setParent(MainUI parent) {
        this.parent = parent;
    }

    void load(LanguageLoader lanLoader) {
        this.lanLoader = lanLoader;
        fillTexts();
        setBoxes();
        algBox.getSelectionModel().select(0);
        setWindowSizeBox();
        setLevelListener();
        setAlgBoxListener();
        setSizeUnitListener();
        levelBox.getSelectionModel().select(3);
        modeBox.getSelectionModel().select(1);
        algBox.getSelectionModel().select(0);
        unitBox.getSelectionModel().select(0);
    }

    void setDir(File[] dir) {
        this.rootDir = dir;
        nameText.setText(dir[0].getName() + ".pz");
    }

    void setStage(Stage stage) {
        this.pStage = stage;
    }

    void setPassword(String password) {
        this.password = password;
    }

    void setEncryption(int level, String encAlg, String passAlg) {
        this.encryptLevel = level;
        this.encAlg = encAlg;
        this.passAlg = passAlg;
    }

    void setAnnotation(AnnotationNode annotation) {
        this.annotation = annotation;
    }

    AnnotationNode getAnnotation() {
        return annotation;
    }

    private void setBoxes() {
        algBox.getItems().addAll(algNames);
        bufferBox.getItems().addAll(bufferSizes);
        levelBox.getItems().addAll(compressionLevels);
        modeBox.getItems().addAll(cmpLevels[0], cmpLevels[1]);
        partialBox.getItems().addAll(splitSizeNames);
        unitBox.getItems().addAll("B", "KB", "MB", "GB");
    }

    private void setWindowSizeBox() {
        windowNameBox.getItems().clear();
        threadBox.getItems().clear();
        switch (algBox.getSelectionModel().getSelectedItem()) {
            case "BWZ":
                windowNameBox.getItems().addAll(windowsSizeNames2);
                threadBox.getItems().addAll(threads);
                break;
//            case "Huffman":
//                threadBox.getItems().addAll(threads);
//                break;
            default:
                windowNameBox.getItems().addAll(windowsSizeNames);
                threadBox.getItems().add(1);
                break;
        }
        threadBox.getSelectionModel().select(0);
    }

    private boolean isLevelAble() {
        return algBox.getSelectionModel().getSelectedItem().equals("LZZ2") ||
                algBox.getSelectionModel().getSelectedItem().equals("LZZ2+");
    }

    private void setSizeUnitListener() {
        partialBox.getSelectionModel().selectedIndexProperty().addListener(((observable, oldValue, newValue) -> {
            if (newValue.intValue() != -1) unitBox.getSelectionModel().select(splitUnits[newValue.intValue()]);
        }));
    }

    private void setLevelListener() {
        levelBox.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.intValue() == 0) {
                windowNameBox.getSelectionModel().clearSelection();
                bufferBox.getSelectionModel().clearSelection();
                modeBox.getSelectionModel().clearSelection();
                windowNameBox.setDisable(true);
                bufferBox.setDisable(true);
                modeBox.setDisable(true);
            } else {
                windowNameBox.setDisable(false);
                modeBox.setDisable(false);
                if (isLevelAble()) bufferBox.setDisable(false);
                if (newValue.intValue() == 1) {
                    windowNameBox.getSelectionModel().select(0);
                    modeBox.getSelectionModel().select(0);
                    if (isLevelAble()) bufferBox.getSelectionModel().select(1);
                } else if (newValue.intValue() == 2) {
                    windowNameBox.getSelectionModel().select(1);
                    modeBox.getSelectionModel().select(0);
                    if (isLevelAble()) bufferBox.getSelectionModel().select(2);
                } else if (newValue.intValue() == 3) {
                    windowNameBox.getSelectionModel().select(2);
                    modeBox.getSelectionModel().select(1);
                    if (isLevelAble()) bufferBox.getSelectionModel().select(3);
                } else if (newValue.intValue() == 4) {
                    windowNameBox.getSelectionModel().select(3);
                    modeBox.getSelectionModel().select(1);
                    if (isLevelAble()) bufferBox.getSelectionModel().select(3);
                } else if (newValue.intValue() == 5) {
                    windowNameBox.getSelectionModel().select(5);
                    modeBox.getSelectionModel().select(1);
                    if (isLevelAble()) bufferBox.getSelectionModel().select(4);
                }
            }
        });
    }

    private void setAlgBoxListener() {
        algBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            switch (newValue) {
                case "LZZ2+":

                case "LZZ2":
                    levelBox.setDisable(false);
                    windowNameBox.setDisable(false);
                    bufferBox.setDisable(false);
                    modeBox.setDisable(false);
                    bufferBox.getSelectionModel().select(3);
                    modeBox.getItems().clear();
                    modeBox.getItems().addAll(cmpLevels);
                    modeBox.getSelectionModel().select(1);
                    break;
                case "BWZ":
                    levelBox.setDisable(false);
                    windowNameBox.setDisable(false);
                    bufferBox.getSelectionModel().clearSelection();
                    modeBox.getSelectionModel().clearSelection();
                    bufferBox.setDisable(true);
                    modeBox.getItems().clear();
                    modeBox.getItems().addAll(cmpLevels[0], cmpLevels[1]);
                    modeBox.getSelectionModel().select(1);
                    break;
//                case "Huffman":
//                    levelBox.setDisable(true);
//                    windowNameBox.setDisable(true);
//                    bufferBox.getSelectionModel().clearSelection();
//                    modeBox.getSelectionModel().clearSelection();
//                    bufferBox.setDisable(true);
//                    modeBox.getItems().clear();
//                    modeBox.setDisable(true);
            }
            setWindowSizeBox();
            levelBox.getSelectionModel().select(3);
            windowNameBox.getSelectionModel().select(2);
        });
    }

    @FXML
    void showPasswordBox() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/trashsoftware/win_bwz/fxml/passwordBox.fxml"));

        Parent root = loader.load();
        Stage stage = new Stage();

        stage.setTitle("WinLZZ");
        stage.setScene(new Scene(root));

        PasswordBox pb = loader.getController();
        pb.setParent(this);
        pb.setLanLoader(lanLoader);
        pb.setStage(stage);

        stage.show();
    }

    @FXML
    void showAnnotationWindow() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/trashsoftware/win_bwz/fxml/annotationUI.fxml"));

        Parent root = loader.load();
        Stage stage = new Stage();

        stage.setTitle(lanLoader.get(900));
        stage.setScene(new Scene(root));

        AnnotationUI au = loader.getController();
        au.setStage(stage);
        au.setParent(this);
        au.setLanLoader(lanLoader);

        stage.show();
    }

    @FXML
    void startCompress() throws Exception {
        String name = nameText.getText();
        if (!name.endsWith(".pz")) name += ".pz";

        String alg = algValues[algBox.getSelectionModel().getSelectedIndex()];

        int window, buffer, cmpLevel;
        if (levelBox.getSelectionModel().getSelectedIndex() == 0) {
            window = 0;
            buffer = 0;
            cmpLevel = 0;
        } else {
            switch (alg) {
                case "bwz":
                    window = windowSizes2[windowNameBox.getSelectionModel().getSelectedIndex()];
                    buffer = 0;
                    cmpLevel = modeBox.getSelectionModel().getSelectedIndex();
                    break;
                case "lzz2p":
                case "lzz2":
                    window = windowSizes[windowNameBox.getSelectionModel().getSelectedIndex()];
                    buffer = bufferBox.getSelectionModel().getSelectedItem();
                    cmpLevel = modeBox.getSelectionModel().getSelectedIndex();
                    break;
                default:
                    window = windowSizes[windowNameBox.getSelectionModel().getSelectedIndex()];
                    buffer = 256;
                    cmpLevel = 0;
                    break;
            }
        }
        int threads = threadBox.getSelectionModel().getSelectedItem();

        long partSize;
        String partText = partialBox.getEditor().getText();
        if (partialBox.getSelectionModel().getSelectedIndex() == 0)
            partSize = 1457664;  // Special case for 3.5" floppy disk
        else if (partText.length() != 0) {
            long unit = (long) Math.pow(1024, unitBox.getSelectionModel().getSelectedIndex());
            String partSizeText;
            if (partText.contains(" ")) partSizeText = partText.split(" ")[0];
            else partSizeText = partText;
            partSize = (long) (Double.valueOf(partSizeText) * unit);
        } else {
            partSize = 0;
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/trashsoftware/win_bwz/fxml/compressingUI.fxml"));

        Parent root = loader.load();
        Stage stage = new Stage();

        stage.setTitle("WinLZZ");
        stage.setScene(new Scene(root));
        stage.setResizable(false);

        CompressingUI cui = loader.getController();
        cui.setName(name, rootDir);
        cui.setLanLoader(lanLoader);
        cui.setGrandParent(parent);
        cui.setPref(window, buffer, cmpLevel, alg, threads, annotation, partSize);
        cui.setStage(stage);
        cui.setEncrypt(password, encryptLevel, encAlg, passAlg);
        stage.show();
        cui.compress();

        pStage.close();
    }

    private void fillTexts() {
        nameLabel.setText(lanLoader.get(100));
        passwordButton.setText(lanLoader.get(101));
        algLabel.setText(lanLoader.get(102));
        levelLabel.setText(lanLoader.get(103));
        windowLabel.setText(lanLoader.get(104));
        bufferLabel.setText(lanLoader.get(105));
        strongModeLabel.setText(lanLoader.get(106));
        threadLabel.setText(lanLoader.get(107));
        partialLabel.setText(lanLoader.get(121));
        startCompressButton.setText(lanLoader.get(108));
        annotationButton.setText(lanLoader.get(109));

        for (int i = 0; i < 6; i++) compressionLevels[i] = lanLoader.get(110 + i);
        for (int i = 0; i < 5; i++) cmpLevels[i] = lanLoader.get(116 + i);
    }
}
