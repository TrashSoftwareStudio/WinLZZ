package GUI;

import Packer.ContextNode;
import Packer.UnPacker;
import ResourcesPack.Languages.LanguageLoader;
import Utility.Util;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class UncompressingUI implements Initializable {

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label messageLabel, percentageLabel, ratioLabel, timeUsedLabel, expectTimeLabel, totalSizeLabel,
            passedSizeLabel, passedSizeTitleLabel, origSizeTextLabel, timeUsedTextLabel, timeRemainTextLabel,
            speedTextLabel;

    @FXML
    private Button cancelButton;

    private UncompressService service;

    private ChangeListener<Number> progressListener;

    private ChangeListener<String> percentageListener, speedRatioListener, timeUsedListener,
            timeExpectedListener, passedLengthListener;

    private long startTime;

    private Stage stage;

    private UncompressUI parent;

    private UnPacker unPacker;

    private File targetDir;

    private ContextNode startNode;

    private int threadNumber;

    private boolean isTest, testResult;

    private LanguageLoader lanLoader;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    void setLanLoader(LanguageLoader lanLoader) {
        this.lanLoader = lanLoader;
        fillText();
    }

    void setTest() {
        this.isTest = true;
        passedSizeTitleLabel.setText(lanLoader.get(550));
    }

    void setStage(Stage stage, UncompressUI parent) {
        this.stage = stage;
        this.parent = parent;
    }

    void setThreadNumber(int threadNumber) {
        this.threadNumber = threadNumber;
    }

    void setParameters(UnPacker unPacker, File targetDir, ContextNode startNode) {
        this.unPacker = unPacker;
        this.targetDir = targetDir;
        this.startNode = startNode;
        totalSizeLabel.setText(Util.sizeToReadable(unPacker.getTotalOrigSize()));
    }

    void setParameters(UnPacker unPacker) {
        this.unPacker = unPacker;
        totalSizeLabel.setText(Util.sizeToReadable(unPacker.getTotalOrigSize()));
    }

    void startUncompress() {
        service = new UncompressService();

        service.setOnSucceeded(e -> {
            unbindListeners();
            if (isTest) {
                if (testResult) showTestPassInfo();
                else showTestFailInfo();
            } else {
                showSuccessInfo();
            }
            System.gc();
            stage.close();
        });

        service.setOnFailed(e -> {
            unbindListeners();

            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("WinLZZ");
            info.setHeaderText(lanLoader.get(551));
            info.show();
            System.gc();
            stage.close();

            e.getSource().getException().printStackTrace();
        });

        service.setOnCancelled(e -> {
            unPacker.interrupt();
            unbindListeners();
            progressBar.setProgress(0.0);
            System.gc();
            stage.close();

            parent.close();
        });

        progressBar.progressProperty().bind(service.progressProperty());
        ratioLabel.textProperty().bind(service.messageProperty());

        if (isTest) messageLabel.setText(lanLoader.get(506));
        else messageLabel.setText(lanLoader.get(504));

        service.start();
    }

    private void unbindListeners() {
        unPacker.progressProperty().removeListener(progressListener);
        unPacker.ratioProperty().removeListener(speedRatioListener);
        unPacker.percentageProperty().removeListener(percentageListener);
        unPacker.timeUsedProperty().removeListener(timeUsedListener);
        unPacker.timeExpectedProperty().removeListener(timeExpectedListener);
        unPacker.passedLengthProperty().removeListener(passedLengthListener);

        progressBar.progressProperty().unbind();
        ratioLabel.textProperty().unbind();
    }

    @FXML
    public void interruptAction() {
        if (!isTest) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("WinLZZ");
            alert.setHeaderText(lanLoader.get(552));
            alert.setContentText(lanLoader.get(553));
            alert.showAndWait();
            if (alert.getResult() != ButtonType.OK) return;
        }
        service.cancel();
    }

    private void showSuccessInfo() {
        long timeUsed = System.currentTimeMillis() - startTime;
        messageLabel.setText(lanLoader.get(560));

        Alert info = new Alert(Alert.AlertType.CONFIRMATION);
        info.setTitle("WinLZZ");
        double seconds = (double) timeUsed / 1000;
        info.setHeaderText(lanLoader.get(554) + lanLoader.get(555) + " " + seconds + lanLoader.get(559));
        info.setContentText(lanLoader.get(556));
        info.showAndWait();
        if (info.getResult() == ButtonType.OK) {
            try {
                Desktop.getDesktop().open(targetDir);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    private void showTestPassInfo() {
        long timeUsed = System.currentTimeMillis() - startTime;

        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("WinLZZ");
        double seconds = (double) timeUsed / 1000;
        info.setHeaderText(lanLoader.get(557));
        info.setContentText(lanLoader.get(555) + " " + seconds + lanLoader.get(559));
        info.showAndWait();
    }

    private void showTestFailInfo() {
        long timeUsed = System.currentTimeMillis() - startTime;

        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("WinLZZ");
        double seconds = (double) timeUsed / 1000;
        info.setHeaderText(lanLoader.get(558));
        info.setContentText(lanLoader.get(555) + " " + seconds + lanLoader.get(559));
        info.showAndWait();
    }

    private class UncompressService extends Service<Void> {

        @Override
        protected Task<Void> createTask() {
            return new Task<>() {
                @Override
                protected Void call() throws Exception {
                    startTime = System.currentTimeMillis();

                    long totalLength = unPacker.getTotalOrigSize();
                    unPacker.setThreads(threadNumber);

                    Platform.runLater(() -> totalSizeLabel.setText(Util.sizeToReadable(totalLength)));

                    progressListener = (observable, oldValue, newValue) -> updateProgress(newValue.longValue(), totalLength);
                    speedRatioListener = (observable, oldValue, newValue) -> updateMessage(newValue);
                    percentageListener = (observable, oldValue, newValue) -> Platform.runLater(() -> percentageLabel.setText(newValue));
                    timeUsedListener = (observable, oldValue, newValue) -> Platform.runLater(() -> timeUsedLabel.setText(newValue));
                    timeExpectedListener = (observable, oldValue, newValue) -> Platform.runLater(() -> expectTimeLabel.setText(newValue));
                    passedLengthListener = (observable, oldValue, newValue) -> Platform.runLater(() -> passedSizeLabel.setText(newValue));

                    unPacker.progressProperty().addListener(progressListener);
                    unPacker.ratioProperty().addListener(speedRatioListener);
                    unPacker.percentageProperty().addListener(percentageListener);
                    unPacker.timeUsedProperty().addListener(timeUsedListener);
                    unPacker.timeExpectedProperty().addListener(timeExpectedListener);
                    unPacker.passedLengthProperty().addListener(passedLengthListener);

                    if (isTest) testResult = unPacker.TestPack();
                    else unPacker.unCompressFrom(targetDir.getAbsolutePath(), startNode);
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

        origSizeTextLabel.setText(lanLoader.get(500));
        passedSizeTitleLabel.setText(lanLoader.get(501));
        timeUsedTextLabel.setText(lanLoader.get(502));
        timeRemainTextLabel.setText(lanLoader.get(503));
        speedTextLabel.setText(lanLoader.get(505));
        cancelButton.setText(lanLoader.get(2));
    }
}
