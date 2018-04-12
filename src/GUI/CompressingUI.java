package GUI;

import Packer.Packer;
import Utility.Util;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class CompressingUI implements Initializable {

    @FXML
    ProgressBar progressBar;

    @FXML
    Label messageLabel;

    @FXML
    Label percentageLabel;

    @FXML
    Label ratioLabel;

    @FXML
    Label timeUsedLabel;

    @FXML
    Label expectTimeLabel;

    @FXML
    Label totalSizeLabel;

    @FXML
    Label passedSizeLabel;

    @FXML
    Label cmpSizeTextLabel;

    @FXML
    Label currentCmpRatioTextLabel;

    @FXML
    Label compressedSizeLabel;

    @FXML
    Label currentCmpRatioLabel;

    private CompressService service;

    private ChangeListener<Number> progressListener;

    private ChangeListener<String> percentageListener;

    private ChangeListener<String> stepListener;

    private ChangeListener<String> speedRatioListener;

    private ChangeListener<String> timeUsedListener;

    private ChangeListener<String> timeExpectedListener;

    private ChangeListener<String> passedLengthListener;

    private ChangeListener<String> cmpSizeListener;

    private ChangeListener<String> currentCmpRatioListener;

    private String name;

    private File path;

    private int windowSize;

    private int bufferSize;

    private int cmpLevel;

    private int encryptLevel;

    private String alg;

    private String password;

    private Packer packer;

    private long startTime;

    private Stage stage;

    private int threads;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        timeUsedLabel.setText("--:--");
        expectTimeLabel.setText("--:--");
        passedSizeLabel.setText("0 字节");
        compressedSizeLabel.setText("0 字节");
        currentCmpRatioLabel.setText("0.0%");
    }

    void setStage(Stage stage) {
        this.stage = stage;
    }

    void setName(String name, File path) {
        this.name = name;
        this.path = path;
    }

    void setPref(int windowSize, int bufferSize, int compressionLevel, String algorithm, int threads) {
        this.windowSize = windowSize;
        this.bufferSize = bufferSize;
        this.cmpLevel = compressionLevel;
        this.alg = algorithm;
        this.threads = threads;
        if (!algorithm.equals("bwz")) {
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

    void setEncrypt(String password, int level) {
        this.encryptLevel = level;
        this.password = password;
    }

    void compress() {
        service = new CompressService();

        service.setOnSucceeded(e -> {
            long timeUsed = System.currentTimeMillis() - startTime;
            unbindListeners();

            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("WinLZZ");
            info.setHeaderText("压缩完成");
            double seconds = (double) timeUsed / 1000;
            double rounded;
            if (packer.getTotalOrigSize() == 0) {
                rounded = 0;
            } else {
                double compressRate = (double) packer.getCompressedLength() / packer.getTotalOrigSize();
                rounded = (double) Math.round(compressRate * 10000) / 100;
            }
            info.setContentText("共耗时" + seconds + "秒，压缩率" + rounded + "%");
            info.show();
            System.gc();
            stage.close();
        });

        service.setOnFailed(e -> {
            unbindListeners();

            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("WinLZZ");
            info.setHeaderText("压缩失败");

            info.show();
            System.gc();
            stage.close();

            Throwable exception = e.getSource().getException();
            exception.printStackTrace();
        });

        service.setOnCancelled(e -> {
            packer.interrupt();
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
        alert.setHeaderText("取消压缩");
        alert.setContentText("确认要取消压缩?");
        alert.showAndWait();
        if (alert.getResult() == ButtonType.OK) service.cancel();
    }

    private class CompressService extends Service<Void> {

        @Override
        protected Task<Void> createTask() {
            return new Task<>() {
                @Override
                protected Void call() throws Exception {
                    startTime = System.currentTimeMillis();
                    packer = new Packer(path);
                    packer.setCmpLevel(cmpLevel);
                    packer.setEncrypt(password, encryptLevel);
                    packer.setAlgorithm(alg);
                    packer.setThreads(threads);

                    long totalLength = packer.getTotalOrigSize();

                    Platform.runLater(() -> totalSizeLabel.setText(Util.sizeToReadable(totalLength)));

                    progressListener = (observable, oldValue, newValue) -> updateProgress(newValue.longValue(), totalLength);
                    stepListener = (observable, oldValue, newValue) -> updateTitle(newValue);
                    speedRatioListener = (observable, oldValue, newValue) -> updateMessage(newValue);
                    percentageListener = (observable, oldValue, newValue) -> Platform.runLater(() -> percentageLabel.setText(newValue));
                    timeUsedListener = (observable, oldValue, newValue) -> Platform.runLater(() -> timeUsedLabel.setText(newValue));
                    timeExpectedListener = (observable, oldValue, newValue) -> Platform.runLater(() -> expectTimeLabel.setText(newValue));
                    passedLengthListener = (observable, oldValue, newValue) -> Platform.runLater(() -> passedSizeLabel.setText(newValue));

                    // Listeners only work under BWT.
                    cmpSizeListener = (observable, oldValue, newValue) -> Platform.runLater(() -> compressedSizeLabel.setText(newValue));
                    currentCmpRatioListener = (observable, oldValue, newValue) -> Platform.runLater(() -> currentCmpRatioLabel.setText(newValue));

                    packer.stepProperty().addListener(stepListener);
                    packer.progressProperty().addListener(progressListener);
                    packer.ratioProperty().addListener(speedRatioListener);
                    packer.percentageProperty().addListener(percentageListener);
                    packer.timeUsedProperty().addListener(timeUsedListener);
                    packer.timeExpectedProperty().addListener(timeExpectedListener);
                    packer.passedLengthProperty().addListener(passedLengthListener);

                    packer.compressedSizeProperty().addListener(cmpSizeListener);
                    packer.currentCmpRatioProperty().addListener(currentCmpRatioListener);

                    packer.Pack(path.getParent() + File.separator + name, windowSize, bufferSize);
                    updateProgress(totalLength, totalLength);

                    return null;
                }
            };
        }
    }
}
