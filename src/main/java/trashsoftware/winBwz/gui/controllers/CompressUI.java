package trashsoftware.winBwz.gui.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import trashsoftware.winBwz.core.bwz.BWZCompressor;
import trashsoftware.winBwz.core.fastLzz.FastLzzCompressor;
import trashsoftware.winBwz.core.lzz2.LZZ2Compressor;
import trashsoftware.winBwz.gui.graphicUtil.AnnotationNode;
import trashsoftware.winBwz.resourcesPack.configLoader.LoaderManager;
import trashsoftware.winBwz.utility.Util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class CompressUI implements Initializable {

    private static final String[] fmts = {"pz", "zip"};
    //    private static final String[] algNames = {"BWZ", "LZZ2", "FastLZZ"};
//    private static final String[] algValues = {"bwz", "lzz2", "fastLzz"};
    private static final AlgBoxItem[] PZ_ALG = {
            new AlgBoxItem("BWZ", "bwz"),
            new AlgBoxItem("LZZ2", "lzz2"),
            new AlgBoxItem("FastLZZ", "fastLzz")
    };
    private static final AlgBoxItem[] ZIP_ALG = {
            new AlgBoxItem("Deflate", "deflate")
    };
    private static final String[] compressionLevels = new String[6];
    private static final String[] compressionLevelsZip = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
    private static final String[] windowSizeNamesLzz2 = {"4KB", "16KB", "32KB", "64KB", "128KB", "256KB", "1MB"};
    private static final String[] windowSizeNamesBwz = {"128KB", "256KB", "512KB", "1MB", "2MB", "4MB", "8MB", "16MB"};
    private static final String[] windowSizeNamesFastLzz = {"4KB", "16KB", "32KB", "64KB", "69KB"};
    private static final String[] splitSizeNames = {"1.44 MB - Floppy", "10 MB", "650 MB - CD", "700 MB - CD",
            "4095 MB - FAT32", "4481 MB - DVD"};
    private static final int[] windowSizesLzz2 = {4096, 16384, 32768, 65536, 131072, 262144, 1048576};
    private static final int[] windowSizesBwz = {131072, 262144, 524288, 1048576, 2097152, 4194304, 8388608, 16777216};
    private static final int[] windowSizesFastLzz = {4096, 16384, 32768, 65536, FastLzzCompressor.MAXIMUM_DISTANCE};
    /**
     * Indices of units of corresponding pre-selections, 0 for byte, 1 for kb, 2 for mb, 3 for gb.
     */
    private static final int[] splitUnits = {2, 2, 2, 2, 2, 2};
    private static final Integer[] labSizesLzz2 = {8, 16, 32, 64, 128, 256, LZZ2Compressor.MAXIMUM_LENGTH};
    private static final Integer[] labSizesFastLzz = {8, 16, 32, 64, 128, 256, FastLzzCompressor.MAXIMUM_LENGTH};
    private static final String[] cmpModeLevels = new String[5];
    private static final Integer[] threads = {1, 2, 3, 4};
    @FXML
    private TextField nameText;
    @FXML
    private ComboBox<String> fmtBox, presetLevelBox, windowNameBox, modeBox, partialBox, unitBox;
    @FXML
    private ComboBox<AlgBoxItem> algBox;
    @FXML
    private ComboBox<Integer> bufferBox, threadBox;
    @FXML
    private Label memoryNeedComLabel, memoryNeedUncLabel;
    // Window sizes of LZZ2.
    private File[] rootDir;
    // Window sizes of BWT.
    private Stage pStage;
    private String password;
    private String encAlg;
    private String passAlg;
    private AnnotationNode annotation;
    private int encryptLevel = 0;
    private MainUI parent;
    private ResourceBundle bundle;

    private int currentThreadIndex = 0;
    private int currentModeIndex = 1;
//    private int currentAlgIndex = 0;
    private int currentWindowIndex = 2;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.bundle = resources;
    }

    void setParent(MainUI parent) {
        this.parent = parent;
    }

    void load() {
        fillTexts();
        fillGeneralBoxes();
        setLevelListener();
        setFmtBoxListener();
        setAlgBoxListener();
        setSizeUnitListener();
        setThreadBoxListener();
        setWindowNameBoxListener();
        setModeBoxListener();
        fmtBox.getSelectionModel().select(
                LoaderManager.getCacheSaver().readString("fmtName", "pz")
        );
        algBox.getSelectionModel().select(
                LoaderManager.getCacheSaver().readInt("algBoxIndex", 0));
        presetLevelBox.getSelectionModel().select(
                LoaderManager.getCacheSaver().readInt("levelBoxIndex", 3));
        modeBox.getSelectionModel().select(1);
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

    AnnotationNode getAnnotation() {
        return annotation;
    }

    void setAnnotation(AnnotationNode annotation) {
        this.annotation = annotation;
    }

    private void fillGeneralBoxes() {
        fmtBox.getItems().addAll(fmts);
        partialBox.getItems().addAll(splitSizeNames);
        unitBox.getItems().addAll("B", "KB", "MB", "GB");
    }

    private void setSizeUnitListener() {
        partialBox.getSelectionModel().selectedIndexProperty().addListener(((observable, oldValue, newValue) -> {
            if (newValue.intValue() != -1) unitBox.getSelectionModel().select(splitUnits[newValue.intValue()]);
        }));
    }

    private void setWindowNameBoxListener() {
        windowNameBox.getSelectionModel().selectedIndexProperty().addListener((
                (observable, oldValue, newValue) -> {
                    if (newValue.intValue() >= 0) {
                        currentWindowIndex = newValue.intValue();
                        estimateMemoryUsage();
                    }
                }
        ));
    }

    private void setLevelListener() {
        presetLevelBox.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.intValue() == 0) {
                windowNameBox.getSelectionModel().clearSelection();
                bufferBox.getSelectionModel().clearSelection();
                modeBox.getSelectionModel().clearSelection();
                windowNameBox.setDisable(true);
                bufferBox.setDisable(true);
                modeBox.setDisable(true);
            } else {
                windowNameBox.setDisable(false);

                int[] windowModeBuffer = getPresetLevels(newValue.intValue());
                windowNameBox.getSelectionModel().select(windowModeBuffer[0]);
                if (windowModeBuffer[1] >= 0) {
                    modeBox.setDisable(false);
                    modeBox.getSelectionModel().select(windowModeBuffer[1]);
                }
                if (windowModeBuffer[2] >= 0) {  // has buffer (look ahead buffer) option
                    bufferBox.setDisable(false);
                    bufferBox.getSelectionModel().select(windowModeBuffer[2]);
                }
            }
            LoaderManager.getCacheSaver().writeCache("levelBoxIndex", newValue.intValue());
        });
    }

    private void setThreadBoxListener() {
        threadBox.getSelectionModel().selectedIndexProperty().addListener((
                (observable, oldValue, newValue) -> {
                    if (newValue != null && newValue.intValue() >= 0) {
                        currentThreadIndex = newValue.intValue();
                        estimateMemoryUsage();
                        LoaderManager.getCacheSaver().writeCache("threadBoxIndex", newValue.intValue());
                    }
                }
        ));
    }

    private void setFmtBoxListener() {
        fmtBox.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
            if (newValue != null) {
                int algIndex = LoaderManager.getCacheSaver().readInt("algBoxIndex", 0);
                int levelIndex = LoaderManager.getCacheSaver().readInt("levelBoxIndex", 3);
                if (newValue.equals("pz")) {
                    algBox.getItems().clear();
                    algBox.getItems().addAll(PZ_ALG);
                    presetLevelBox.getItems().clear();
                    presetLevelBox.getItems().addAll(compressionLevels);
                    replaceNameExt("pz");
                } else if (newValue.equals("zip")) {
                    algBox.getItems().clear();
                    algBox.getItems().addAll(ZIP_ALG);
                    presetLevelBox.getItems().clear();
                    presetLevelBox.getItems().addAll(compressionLevelsZip);
                    replaceNameExt("zip");
                }
                algBox.getSelectionModel().select(algIndex >= algBox.getItems().size() ?
                        0 : algIndex);
                presetLevelBox.getSelectionModel().select(levelIndex >= presetLevelBox.getItems().size() ?
                        3 : levelIndex);

                LoaderManager.getCacheSaver().writeCache("fmtName", newValue);
            }
        }));
    }

    private void replaceNameExt(String dstExt) {
        String outName = nameText.getText();
        int dotIndex = outName.lastIndexOf('.');
        String result;
        if (dotIndex >= 0) {
            result = outName.substring(0, dotIndex) + '.' + dstExt;
        } else {
            result = outName + '.' + dstExt;
        }
        nameText.setText(result);
    }

    private void setAlgBoxListener() {
        algBox.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
//            currentAlgIndex = newValue.intValue();
            if (newValue.intValue() < 0) return;
            AlgBoxItem abi = algBox.getItems().get(newValue.intValue());
            String newAlgName = abi.code;
            switch (newAlgName) {
                case "fastLzz":
                    setFastLzzUi();
                    bufferBox.getSelectionModel().select(3);
                    modeBox.getSelectionModel().select(0);
                    break;
                case "lzz2":
                    setLzz2Ui();
                    bufferBox.getSelectionModel().select(3);
                    modeBox.getSelectionModel().select(1);
                    break;
                case "bwz":
                    setBwzUi();
                    modeBox.getSelectionModel().select(1);
                    break;
                case "deflate":
                    setZipUi();
                    modeBox.getSelectionModel().select(1);
                    break;
                default:
                    throw new RuntimeException();
            }
//            presetLevelBox.getSelectionModel().select(3);
            windowNameBox.getSelectionModel().select(2);
            estimateMemoryUsage();
            LoaderManager.getCacheSaver().writeCache("algBoxIndex", newValue.intValue());
        });
    }

    private void setModeBoxListener() {
        modeBox.getSelectionModel().selectedIndexProperty().addListener(((observable, oldValue, newValue) -> {
            if (newValue.intValue() >= 0) {
                currentModeIndex = newValue.intValue();
                estimateMemoryUsage();
            }
        }));
    }

    private void estimateMemoryUsage() {
        String alg = getAlgCode();
        switch (alg) {
            case "bwz":
                updateMemoryLabels(
                        BWZCompressor.estimateMemoryUsage(
                                threads[currentThreadIndex],
                                windowSizesBwz[currentWindowIndex],
                                currentModeIndex
                        )
                );
                break;
            case "fastLzz":
                updateMemoryLabels(
                        FastLzzCompressor.estimateMemoryUsage(
                                threads[currentThreadIndex],
                                windowSizesFastLzz[currentWindowIndex],
                                currentModeIndex
                        )
                );
                break;
            case "lzz2":
                updateMemoryLabels(
                        LZZ2Compressor.estimateMemoryUsage(
                                threads[currentThreadIndex],
                                windowSizesLzz2[currentWindowIndex],
                                currentModeIndex
                        )
                );
                break;
        }
    }

    private void updateMemoryLabels(long[] memoryUse) {
        int cmpInMb = (int) Math.ceil((double) memoryUse[0] / 1048576);
        int uncInMb = (int) Math.ceil((double) memoryUse[1] / 1048576);
        memoryNeedComLabel.setText(Util.numToReadable2Decimal(cmpInMb) + " MB");
        memoryNeedUncLabel.setText(Util.numToReadable2Decimal(uncInMb) + " MB");
    }

    private String getAlgCode() {
        AlgBoxItem abi = algBox.getSelectionModel().getSelectedItem();
        if (abi != null)
            return abi.code;
        else
            if (fmtBox.getValue().equals("pz")) return "bwz";
            else return "deflate";
    }

    private void setZipUi() {
        presetLevelBox.setDisable(false);
        windowNameBox.setDisable(false);
        windowNameBox.getItems().clear();
        windowNameBox.getItems().addAll("32KB");
        windowNameBox.getSelectionModel().select(0);
        bufferBox.setDisable(false);
        bufferBox.getItems().clear();
        bufferBox.getItems().addAll(64);
        bufferBox.getSelectionModel().select(0);
        modeBox.setDisable(true);
        threadBox.getItems().clear();
        threadBox.getItems().add(1);
        threadBox.getSelectionModel().select(0);
    }

    private void setLzz2Ui() {
        presetLevelBox.setDisable(false);
        windowNameBox.setDisable(false);
        windowNameBox.getItems().clear();
        windowNameBox.getItems().addAll(windowSizeNamesLzz2);
        bufferBox.setDisable(false);
        bufferBox.getItems().clear();
        bufferBox.getItems().addAll(labSizesLzz2);
        modeBox.setDisable(false);
        modeBox.getItems().clear();
        modeBox.getItems().addAll(cmpModeLevels);
        threadBox.getItems().clear();
        threadBox.getItems().add(1);
        threadBox.getSelectionModel().select(0);
    }

    private void setBwzUi() {
        presetLevelBox.setDisable(false);
        windowNameBox.setDisable(false);
        windowNameBox.getItems().clear();
        windowNameBox.getItems().addAll(windowSizeNamesBwz);
        bufferBox.getSelectionModel().clearSelection();
        bufferBox.getItems().clear();
        bufferBox.setDisable(true);
        modeBox.setDisable(false);
        modeBox.getSelectionModel().clearSelection();
        modeBox.getItems().clear();
        modeBox.getItems().addAll(cmpModeLevels[0], cmpModeLevels[1]);
        threadBox.getItems().clear();
        threadBox.getItems().addAll(threads);
        threadBox.getSelectionModel().select(LoaderManager.getCacheSaver().readInt("threadBoxIndex", 0));
//        threadBox.getSelectionModel().select(0);
    }

    private void setFastLzzUi() {
        presetLevelBox.setDisable(false);
        windowNameBox.setDisable(false);
        windowNameBox.getItems().clear();
        windowNameBox.getItems().addAll(windowSizeNamesFastLzz);
        bufferBox.setDisable(false);
        bufferBox.getItems().clear();
        bufferBox.getItems().addAll(labSizesFastLzz);
        modeBox.setDisable(false);
        modeBox.getItems().clear();
        modeBox.getItems().addAll(cmpModeLevels[0], cmpModeLevels[1]);
        threadBox.getItems().clear();
        threadBox.getItems().addAll(threads);
        threadBox.getSelectionModel().select(LoaderManager.getCacheSaver().readInt("threadBoxIndex", 0));
//        threadBox.getSelectionModel().select(0);
    }

    @FXML
    void showPasswordBox() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/trashsoftware/winBwz/fxml/passwordBox.fxml"), bundle);

        Parent root = loader.load();
        Stage stage = new Stage();

        stage.setTitle("WinLZZ");
        stage.setScene(new Scene(root));

        PasswordBox pb = loader.getController();
        pb.setParent(this);
        pb.setStage(stage);

        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(this.pStage);
        stage.show();
    }

    @FXML
    void showAnnotationWindow() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/trashsoftware/winBwz/fxml/annotationUI.fxml"), bundle);

        Parent root = loader.load();
        Stage stage = new Stage();

        stage.setTitle(bundle.getString("annotations"));
        stage.setScene(new Scene(root));

        AnnotationUI au = loader.getController();
        au.setStage(stage);
        au.setParent(this);
//        au.setLanLoader(lanLoader);

        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(this.pStage);
        stage.show();
    }

    @FXML
    void startCompress() throws Exception {
        String name = nameText.getText();
        String fmt = fmtBox.getSelectionModel().getSelectedItem();
        if (!name.endsWith("." + fmt)) name += "." + fmt;

        String alg = getAlgCode();

        int window, buffer, cmpLevel;
        if (presetLevelBox.getSelectionModel().getSelectedIndex() == 0) {
            window = 0;
            buffer = 0;
            cmpLevel = 0;
        } else {
            switch (alg) {
                case "bwz":
                    window = windowSizesBwz[currentWindowIndex];
                    buffer = 0;
                    cmpLevel = currentModeIndex;
                    break;
                case "fastLzz":
                    window = windowSizesFastLzz[currentWindowIndex];
                    buffer = bufferBox.getSelectionModel().getSelectedItem();
                    cmpLevel = currentModeIndex;
                    break;
                case "lzz2":
                    window = windowSizesLzz2[currentWindowIndex];
                    buffer = bufferBox.getSelectionModel().getSelectedItem();
                    cmpLevel = currentModeIndex;
                    break;
                case "deflate":
                    window = 32768;
                    buffer = bufferBox.getSelectionModel().getSelectedItem();
                    // special for zip
                    cmpLevel = presetLevelBox.getSelectionModel().getSelectedIndex();
                    break;
                default:
                    throw new RuntimeException();
            }
        }
        int threadNum = threads[currentThreadIndex];

        long partSize;
        String partText = partialBox.getEditor().getText();
        if (partialBox.getSelectionModel().getSelectedIndex() == 0)
            partSize = 1457664;  // Special case for 3.5" floppy disk
        else if (partText.length() != 0) {
            long unit = (long) Math.pow(1024, unitBox.getSelectionModel().getSelectedIndex());
            String partSizeText;
            if (partText.contains(" ")) partSizeText = partText.split(" ")[0];
            else partSizeText = partText;
            partSize = (long) (Double.parseDouble(partSizeText) * unit);
        } else {
            partSize = 0;
        }

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/trashsoftware/winBwz/fxml/compressingUI.fxml"), bundle);

        Parent root = loader.load();
        Stage stage = new Stage();

        stage.setTitle("WinLZZ");
        stage.setScene(new Scene(root));
        stage.setResizable(false);

        CompressingUI cui = loader.getController();
        cui.setName(name, rootDir);
        cui.setGrandParent(parent);
        cui.setPref(fmt, window, buffer, cmpLevel, alg, threadNum, annotation, partSize);
        cui.setStage(stage);
        cui.setEncrypt(password, encryptLevel, encAlg, passAlg);
        stage.show();
        cui.compress();

        pStage.close();
    }

    private void fillTexts() {
        for (int i = 0; i < 6; i++) compressionLevels[i] = bundle.getString("compressLv" + i);
        for (int i = 0; i < 5; i++) cmpModeLevels[i] = bundle.getString("compressStrongLv" + i);
    }

    /**
     * @param presetLevel preset level
     * @return 3 int tuple: {window index, mode index, buffer index}
     */
    private int[] getPresetLevels(int presetLevel) {
        String currentAlgValue = getAlgCode();
        switch (currentAlgValue) {
            case "bwz":
                return getBwzPref(presetLevel);
            case "lzz2":
                return getLzz2Pref(presetLevel);
            case "fastLzz":
                return getFastLzzPref(presetLevel);
            case "deflate":
                return getDeflatePref(presetLevel);
            default:
                throw new RuntimeException();
        }
    }

    private int[] getBwzPref(int presetLevel) {
        switch (presetLevel) {
            case 1:
                return new int[]{0, 0, -1};
            case 2:
                return new int[]{1, 0, -1};
            default:  // default level is 3
                return new int[]{2, 1, -1};
            case 4:
                return new int[]{3, 1, -1};
            case 5:
                return new int[]{5, 1, -1};
        }
    }

    private int[] getLzz2Pref(int presetLevel) {
        switch (presetLevel) {
            case 1:
                return new int[]{0, 0, 1};
            case 2:
                return new int[]{1, 0, 2};
            default:  // default level is 3
                return new int[]{2, 1, 3};
            case 4:
                return new int[]{3, 2, 3};
            case 5:
                return new int[]{5, 3, 4};
        }
    }

    private int[] getFastLzzPref(int presetLevel) {
        switch (presetLevel) {
            case 1:
                return new int[]{0, 0, 1};
            case 2:
                return new int[]{1, 0, 2};
            default:  // default level is 3
                return new int[]{2, 0, 3};
            case 4:
                return new int[]{3, 1, 3};
            case 5:
                return new int[]{5, 1, 4};
        }
    }

    private int[] getDeflatePref(int presetLevel) {
        switch (presetLevel) {
            case 1:
                return new int[]{0, -1, 0};
            case 2:
                return new int[]{1, -1, 0};
            default:  // default level is 3
                return new int[]{2, -1, 0};
            case 4:
                return new int[]{3, -1, 0};
            case 5:
                return new int[]{5, -1, 0};
        }
    }

    private static class AlgBoxItem {
        private final String shown;
        private final String code;

        AlgBoxItem(String shown, String code) {
            this.shown = shown;
            this.code = code;
        }

        @Override
        public String toString() {
            return shown;
        }
    }
}
