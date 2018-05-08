package WinLzz.GUI;

import WinLzz.ResourcesPack.Languages.LanguageLoader;
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
import java.security.NoSuchAlgorithmException;
import java.util.ResourceBundle;

public class CompressUI implements Initializable {

    @FXML
    private TextField nameText;

    @FXML
    private ComboBox<String> algBox, levelBox, windowNameBox, modeBox;

    @FXML
    private ComboBox<Integer> bufferBox, threadBox;

    @FXML
    private Label nameLabel, algLabel, levelLabel, windowLabel, bufferLabel, strongModeLabel, threadLabel;

    @FXML
    private Button startCompressButton, passwordButton;

    private File[] rootDir;
    private Stage pStage;
    private String password;

    private int encryptLevel = 0;
    private String[] algNames = new String[]{"BWZ", "LZZ2"};
    private String[] compressionLevels = new String[6];
    private String[] windowsSizeNames = new String[]{"4KB", "16KB", "32KB", "64KB", "128KB", "256KB", "1MB"};
    private String[] windowsSizeNames2 = new String[]{"128KB", "256KB", "512KB", "1MB", "2MB", "4MB", "8MB", "16MB"};

    private int[] windowSizes = new int[]{4096, 16384, 32768, 65536, 131072, 262144, 1048576};
    // Window sizes of LZZ2.

    private int[] windowSizes2 = new int[]{131072, 262144, 524288, 1048576, 2097152, 4194304, 8388608, 16777216};
    // Window sizes of BWT.

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
        levelBox.getSelectionModel().select(3);
        modeBox.getSelectionModel().select(1);
        algBox.getSelectionModel().select(0);
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

    void setEncryptLevel(int level) {
        this.encryptLevel = level;
    }

    private void setBoxes() {
        algBox.getItems().addAll(algNames);
        bufferBox.getItems().addAll(bufferSizes);
        levelBox.getItems().addAll(compressionLevels);
        modeBox.getItems().addAll(cmpLevels[0], cmpLevels[1]);
    }

    private void setWindowSizeBox() {
        windowNameBox.getItems().clear();
        threadBox.getItems().clear();
        if (algBox.getSelectionModel().getSelectedItem().equals("BWZ")) {
            windowNameBox.getItems().addAll(windowsSizeNames2);
            threadBox.getItems().addAll(threads);
        } else {
            windowNameBox.getItems().addAll(windowsSizeNames);
            threadBox.getItems().add(1);
        }
        threadBox.getSelectionModel().select(0);
    }

    private boolean isLevelAble() {
        return algBox.getSelectionModel().getSelectedItem().equals("LZZ2");
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
                case "LZZ2":
                    bufferBox.setDisable(false);
                    modeBox.setDisable(false);
                    bufferBox.getSelectionModel().select(3);
                    modeBox.getItems().clear();
                    modeBox.getItems().addAll(cmpLevels);
                    modeBox.getSelectionModel().select(1);
                    break;
                case "BWZ":
                    bufferBox.getSelectionModel().clearSelection();
                    modeBox.getSelectionModel().clearSelection();
                    bufferBox.setDisable(true);
                    modeBox.getItems().clear();
                    modeBox.getItems().addAll(cmpLevels[0], cmpLevels[1]);
                    modeBox.getSelectionModel().select(1);
                    break;
            }
            setWindowSizeBox();
            levelBox.getSelectionModel().select(3);
            windowNameBox.getSelectionModel().select(2);
        });
    }

    @FXML
    void showPasswordBox() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("passwordBox.fxml"));

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
    void startCompress() throws Exception {
        String name = nameText.getText();
        if (!name.endsWith(".pz")) name += ".pz";

        String algName = algBox.getSelectionModel().getSelectedItem();
        String alg;
        switch (algName) {
            case "LZZ2":
                alg = "lzz2";
                break;
            case "BWZ":
                alg = "bwz";
                break;
            default:
                throw new NoSuchAlgorithmException();
        }

        int window, buffer, cmpLevel;
        if (levelBox.getSelectionModel().getSelectedIndex() == 0) {
            window = 0;
            buffer = 0;
            cmpLevel = 0;
        } else {
            if (alg.equals("bwz")) window = windowSizes2[windowNameBox.getSelectionModel().getSelectedIndex()];
            else window = windowSizes[windowNameBox.getSelectionModel().getSelectedIndex()];
            if (alg.equals("lzz2")) buffer = bufferBox.getSelectionModel().getSelectedItem();
            else buffer = 0;
            cmpLevel = modeBox.getSelectionModel().getSelectedIndex();
        }
        int threads = threadBox.getSelectionModel().getSelectedItem();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("compressingUI.fxml"));

        Parent root = loader.load();
        Stage stage = new Stage();

        stage.setTitle("WinLZZ");
        stage.setScene(new Scene(root));
        stage.setResizable(false);

        CompressingUI cui = loader.getController();
        cui.setName(name, rootDir);
        cui.setLanLoader(lanLoader);
        cui.setGrandParent(parent);
        cui.setPref(window, buffer, cmpLevel, alg, threads);
        cui.setStage(stage);
        cui.setEncrypt(password, encryptLevel);
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
        startCompressButton.setText(lanLoader.get(108));

        for (int i = 0; i < 6; i++) compressionLevels[i] = lanLoader.get(110 + i);
        for (int i = 0; i < 5; i++) cmpLevels[i] = lanLoader.get(116 + i);
    }
}
