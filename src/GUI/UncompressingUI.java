package GUI;

import Packer.ContextNode;
import Packer.UnPacker;
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

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class UncompressingUI implements Initializable {

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label messageLabel;

    @FXML
    private Label percentageLabel;

    @FXML
    private Label ratioLabel;

    @FXML
    private Label timeUsedLabel;

    @FXML
    private Label expectTimeLabel;

    @FXML
    private Label totalSizeLabel;

    @FXML
    private Label passedSizeLabel;

    @FXML
    private Label passedSizeTitleLabel;

    private UncompressService service;

    private ChangeListener<Number> progressListener;

    private ChangeListener<String> percentageListener;

    private ChangeListener<String> stepListener;

    private ChangeListener<String> speedRatioListener;

    private ChangeListener<String> timeUsedListener;

    private ChangeListener<String> timeExpectedListener;

    private ChangeListener<String> passedLengthListener;

    private long startTime;

    private Stage stage;

    private UncompressUI parent;

    private UnPacker unPacker;

    private File targetDir;

    private ContextNode startNode;

    private int threadNumber;

    private boolean isTest;

    private boolean testResult;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        timeUsedLabel.setText("--:--");
        expectTimeLabel.setText("--:--");
        passedSizeLabel.setText("0 字节");
    }

    void setTest() {
        this.isTest = true;
        passedSizeTitleLabel.setText("已测试大小:  ");
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
            info.setHeaderText("解压失败");
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
        messageLabel.textProperty().bind(service.titleProperty());
        ratioLabel.textProperty().bind(service.messageProperty());

        service.start();
    }

    private void unbindListeners() {
        unPacker.progressProperty().removeListener(progressListener);
        unPacker.stepProperty().removeListener(stepListener);
        unPacker.ratioProperty().removeListener(speedRatioListener);
        unPacker.percentageProperty().removeListener(percentageListener);
        unPacker.timeUsedProperty().removeListener(timeUsedListener);
        unPacker.timeExpectedProperty().removeListener(timeExpectedListener);
        unPacker.passedLengthProperty().removeListener(passedLengthListener);

        messageLabel.textProperty().unbind();
        progressBar.progressProperty().unbind();
        ratioLabel.textProperty().unbind();
    }

    @FXML
    public void interruptAction() {
        if (!isTest) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("WinLZZ");
            alert.setHeaderText("取消解压");
            alert.setContentText("确认要取消解压?");
            alert.showAndWait();
            if (alert.getResult() != ButtonType.OK) return;
        }
        service.cancel();
    }

    private void showSuccessInfo() {
        long timeUsed = System.currentTimeMillis() - startTime;

        Alert info = new Alert(Alert.AlertType.CONFIRMATION);
        info.setTitle("WinLZZ");
        double seconds = (double) timeUsed / 1000;
        info.setHeaderText("解压完成, 共耗时" + seconds + "秒");
        info.setContentText("打开目标文件夹？");
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
        info.setHeaderText("测试结束，未发现问题");
        info.setContentText("共耗时" + seconds + "秒");
        info.showAndWait();
    }

    private void showTestFailInfo() {
        long timeUsed = System.currentTimeMillis() - startTime;

        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("WinLZZ");
        double seconds = (double) timeUsed / 1000;
        info.setHeaderText("测试未通过，文件已损坏");
        info.setContentText("共耗时" + seconds + "秒");
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
                    stepListener = (observable, oldValue, newValue) -> updateTitle(newValue);
                    speedRatioListener = (observable, oldValue, newValue) -> updateMessage(newValue);
                    percentageListener = (observable, oldValue, newValue) -> Platform.runLater(() -> percentageLabel.setText(newValue));
                    timeUsedListener = (observable, oldValue, newValue) -> Platform.runLater(() -> timeUsedLabel.setText(newValue));
                    timeExpectedListener = (observable, oldValue, newValue) -> Platform.runLater(() -> expectTimeLabel.setText(newValue));
                    passedLengthListener = (observable, oldValue, newValue) -> Platform.runLater(() -> passedSizeLabel.setText(newValue));

                    unPacker.stepProperty().addListener(stepListener);
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
}
