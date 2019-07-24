package trashsoftware.win_bwz.GUI;

import trashsoftware.win_bwz.GraphicUtil.AnnotationNode;
import trashsoftware.win_bwz.Packer.Packer;
import trashsoftware.win_bwz.ResourcesPack.Languages.LanguageLoader;
import trashsoftware.win_bwz.Utility.Util;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class CompressingUI implements Initializable {

    @FXML
    ProgressBar progressBar;

    @FXML
    private Label messageLabel, percentageLabel, ratioLabel, timeUsedLabel, expectTimeLabel, totalSizeLabel,
            passedSizeLabel, cmpSizeTextLabel, currentCmpRatioTextLabel, compressedSizeLabel, currentCmpRatioLabel,
            origSizeTextLabel, timeUsedTextLabel, passedSizeTextLabel, timeRemainTextLabel, speedTextLabel, fileLabel;

    @FXML
    private Button cancelButton;

    private CompressService service;

    private ChangeListener<Number> progressListener;

    private ChangeListener<String> percentageListener, stepListener, fileListener, speedRatioListener, timeUsedListener,
            timeExpectedListener, passedLengthListener, cmpSizeListener, currentCmpRatioListener;

    private String name, alg;
    private File[] path;
    private int windowSize, bufferSize, cmpLevel, encryptLevel, threads;
    private String password;
    private String encAlg;
    private String passAlg;
    private Packer packer;
    private AnnotationNode annotation;
    private long startTime;
    private long partSize;

    private MainUI grandParent;
    private Stage stage;
    private LanguageLoader lanLoader;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    void setLanLoader(LanguageLoader lanLoader) {
        this.lanLoader = lanLoader;
        fillText();
    }

    /**
     * Sets up the parent {@code MainUI} instance of the parent {@code CompressUI} instance which launches this
     * {@code CompressingUI}.
     *
     * @param grandParent the parent {@code MainUI} instance of the parent {@code CompressUI} instance which launches
     *                    this {@code CompressingUI}
     */
    void setGrandParent(MainUI grandParent) {
        this.grandParent = grandParent;
    }

    void setStage(Stage stage) {
        this.stage = stage;
    }

    void setName(String name, File[] path) {
        this.name = name;
        this.path = path;
    }

    void setPref(int windowSize, int bufferSize, int compressionLevel, String algorithm, int threads,
                 AnnotationNode annotation, long partSize) {
        this.windowSize = windowSize;
        this.bufferSize = bufferSize;
        this.cmpLevel = compressionLevel;
        this.alg = algorithm;
        this.threads = threads;
        this.annotation = annotation;
        this.partSize = partSize;
        if (algorithm.equals("lzz2") || algorithm.equals("lzz2p")) {
            compressedSizeLabel.setDisable(true);
            compressedSizeLabel.setVisible(false);
            cmpSizeTextLabel.setDisable(true);
            cmpSizeTextLabel.setVisible(false);
            currentCmpRatioLabel.setDisable(true);
            currentCmpRatioLabel.setVisible(false);
            currentCmpRatioTextLabel.setDisable(true);
            currentCmpRatioTextLabel.setVisible(false);
        }
    }

    void setEncrypt(String password, int level, String encAlg, String passAlg) {
        this.encryptLevel = level;
        this.password = password;
        this.encAlg = encAlg;
        this.passAlg = passAlg;
    }

    void compress() {
        service = new CompressService();

        service.setOnSucceeded(e -> {
            long timeUsed = System.currentTimeMillis() - startTime;
            unbindListeners();

            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("WinLZZ");
            info.setHeaderText(lanLoader.get(251));
            double seconds = (double) timeUsed / 1000;
            double rounded;
            if (packer.getTotalOrigSize() == 0) {
                rounded = 0;
            } else {
                double compressRate = (double) packer.getCompressedLength() / packer.getTotalOrigSize();
                rounded = (double) Math.round(compressRate * 10000) / 100;
            }
            info.setContentText(lanLoader.get(252) + seconds + lanLoader.get(253) + " " + lanLoader.get(254)
                    + ": " + rounded + "%");
            info.show();

            grandParent.refreshAction();
            stage.close();
            System.gc();
        });

        service.setOnFailed(e -> {
            unbindListeners();

            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("WinLZZ");
            info.setHeaderText(lanLoader.get(255));

            info.show();
            System.gc();
            stage.close();

            Throwable exception = e.getSource().getException();
            exception.printStackTrace();
        });

        service.setOnCancelled(e -> {
            if (packer != null) packer.interrupt();
            unbindListeners();
            progressBar.setProgress(0.0);
            System.gc();
            stage.close();
        });

        progressBar.progressProperty().bind(service.progressProperty());
        messageLabel.textProperty().bind(service.titleProperty());
        ratioLabel.textProperty().bind(service.messageProperty());

        service.start();
    }

    private void unbindListeners() {
        packer.progressProperty().removeListener(progressListener);
        packer.stepProperty().removeListener(stepListener);
        packer.fileProperty().removeListener(fileListener);
        packer.ratioProperty().removeListener(speedRatioListener);
        packer.percentageProperty().removeListener(percentageListener);
        packer.timeUsedProperty().removeListener(timeUsedListener);
        packer.timeExpectedProperty().removeListener(timeExpectedListener);
        packer.passedLengthProperty().removeListener(passedLengthListener);

        packer.compressedSizeProperty().removeListener(cmpSizeListener);
        packer.currentCmpRatioProperty().removeListener(currentCmpRatioListener);

        messageLabel.textProperty().unbind();
        progressBar.progressProperty().unbind();
        ratioLabel.textProperty().unbind();
    }

    @FXML
    public void interruptAction() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("WinLZZ");
        alert.setHeaderText(lanLoader.get(256));
        alert.setContentText(lanLoader.get(257));
        alert.showAndWait();
        if (alert.getResult() == ButtonType.OK) service.cancel();
    }

    private class CompressService extends Service<Void> {

        @Override
        protected Task<Void> createTask() {
            return new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    startTime = System.currentTimeMillis();
                    packer = new Packer(path);
                    if (isCancelled()) return null;
                    packer.setCmpLevel(cmpLevel);
                    packer.setEncrypt(password, encryptLevel, encAlg, passAlg);
                    packer.setAlgorithm(alg);
                    packer.setThreads(threads);
                    packer.setPartSize(partSize);
                    packer.setLanLoader(lanLoader);
                    if (annotation != null) packer.setAnnotation(annotation);

                    stepListener = (observable, oldValue, newValue) -> updateTitle(newValue);
                    fileListener = (observable, oldValue, newValue) -> Platform.runLater(() -> fileLabel.setText(newValue));
                    speedRatioListener = (observable, oldValue, newValue) -> updateMessage(newValue);
                    percentageListener = (observable, oldValue, newValue) -> Platform.runLater(() -> percentageLabel.setText(newValue));
                    timeUsedListener = (observable, oldValue, newValue) -> Platform.runLater(() -> timeUsedLabel.setText(newValue));
                    timeExpectedListener = (observable, oldValue, newValue) -> Platform.runLater(() -> expectTimeLabel.setText(newValue));
                    passedLengthListener = (observable, oldValue, newValue) -> Platform.runLater(() -> passedSizeLabel.setText(newValue));

                    // Listeners only work under BWT.
                    cmpSizeListener = (observable, oldValue, newValue) -> Platform.runLater(() -> compressedSizeLabel.setText(newValue));
                    currentCmpRatioListener = (observable, oldValue, newValue) -> Platform.runLater(() -> currentCmpRatioLabel.setText(newValue));

                    packer.stepProperty().addListener(stepListener);
                    packer.fileProperty().addListener(fileListener);
                    packer.ratioProperty().addListener(speedRatioListener);
                    packer.percentageProperty().addListener(percentageListener);
                    packer.timeUsedProperty().addListener(timeUsedListener);
                    packer.timeExpectedProperty().addListener(timeExpectedListener);
                    packer.passedLengthProperty().addListener(passedLengthListener);

                    packer.compressedSizeProperty().addListener(cmpSizeListener);
                    packer.currentCmpRatioProperty().addListener(currentCmpRatioListener);

                    updateTitle(lanLoader.get(208));
                    Platform.runLater(() -> percentageLabel.setText("0.0"));
                    packer.build();

                    // Add progress bar
                    long totalLength = packer.getTotalOrigSize();
                    Platform.runLater(() -> totalSizeLabel.setText(Util.sizeToReadable(totalLength)));
                    progressListener = (observable, oldValue, newValue) -> updateProgress(newValue.longValue(), totalLength);
                    packer.progressProperty().addListener(progressListener);

                    packer.Pack(path[0].getParent() + File.separator + name, windowSize, bufferSize);
                    updateProgress(totalLength, totalLength);

                    return null;
                }
            };
        }
    }

    private void fillText() {
        timeUsedLabel.setText("--:--");
        expectTimeLabel.setText("--:--");
        passedSizeLabel.setText("0 " + lanLoader.get(250));
        compressedSizeLabel.setText("0 " + lanLoader.get(250));
        currentCmpRatioLabel.setText("0.0%");

        origSizeTextLabel.setText(lanLoader.get(200));
        timeUsedTextLabel.setText(lanLoader.get(201));
        passedSizeTextLabel.setText(lanLoader.get(202));
        timeRemainTextLabel.setText(lanLoader.get(203));
        cmpSizeTextLabel.setText(lanLoader.get(204));
        currentCmpRatioTextLabel.setText(lanLoader.get(205));
        speedTextLabel.setText(lanLoader.get(207));
        cancelButton.setText(lanLoader.get(2));
        messageLabel.setText(lanLoader.get(350));
    }
}
