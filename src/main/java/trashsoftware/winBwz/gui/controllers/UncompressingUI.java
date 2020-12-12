package trashsoftware.winBwz.gui.controllers;

import trashsoftware.winBwz.packer.ContextNode;
import trashsoftware.winBwz.packer.UnPacker;
import trashsoftware.winBwz.utility.Util;
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
    private Label messageLabel, percentageLabel, ratioLabel, timeUsedLabel, expectTimeLabel, totalSizeLabel,
            passedSizeLabel, passedSizeTitleLabel, fileLabel;

    private UncompressService service;

    private ChangeListener<Number> progressListener;

    private ChangeListener<String> percentageListener, speedRatioListener, timeUsedListener,
            timeExpectedListener, passedLengthListener, messageListener, fileListener;

    private long startTime;
    private Stage stage;
    private UncompressUI parent;
    private UnPacker unPacker;
    private File targetDir;
    private ContextNode startNode;
    private int threadNumber;
    private boolean isTest, testResult, isAllUncompress, openAfterUnc;

    private MainUI grandParent;
    private ResourceBundle bundle;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        bundle = resources;
        fillText();
    }

    /**
     * Sets up the parent {@code MainUI} instance of the parent {@code UncompressUI} instance which launches this
     * {@code UncompressingUI}.
     *
     * @param grandParent the parent {@code MainUI} instance of the parent {@code UncompressUI} instance which
     *                    launches this {@code UncompressingUI}
     */
    void setGrandParent(MainUI grandParent) {
        this.grandParent = grandParent;
    }

//    void setLanLoader(LanguageLoader lanLoader) {
//        this.lanLoader = lanLoader;
//        fillText();
//    }

    void setTest() {
        this.isTest = true;
        passedSizeTitleLabel.setText(bundle.getString("testedSize"));
    }

    void setStage(Stage stage, UncompressUI parent) {
        this.stage = stage;
        this.parent = parent;
    }

    void setThreadNumber(int threadNumber) {
        this.threadNumber = threadNumber;
    }

    /**
     * Sets up decompression parameters for uncompress part of file.
     *
     * @param unPacker  the un-packer
     * @param targetDir directory to extract files.
     * @param startNode the starting context node.
     */
    void setParameters(UnPacker unPacker, File targetDir, ContextNode startNode) {
        this.unPacker = unPacker;
        this.targetDir = targetDir;
        this.startNode = startNode;
        totalSizeLabel.setText(Util.sizeToReadable(unPacker.getTotalOrigSize()));
    }

    /**
     * Sets up decompression parameters for uncompress all.
     *
     * @param unPacker  the un-packer.
     * @param targetDir directory to extract files.
     */
    void setParameters(UnPacker unPacker, File targetDir) {
        this.unPacker = unPacker;
        this.targetDir = targetDir;
        this.isAllUncompress = true;
        totalSizeLabel.setText(Util.sizeToReadable(unPacker.getTotalOrigSize()));
    }

    /**
     * Sets up decompression parameters for testing.
     *
     * @param unPacker the un-packer.
     */
    void setParameters(UnPacker unPacker) {
        this.unPacker = unPacker;
        totalSizeLabel.setText(Util.sizeToReadable(unPacker.getTotalOrigSize()));
    }

    /**
     * Sets up decompression parameters for open a file in archive.
     *
     * @param unPacker the un-packer.
     * @param openNode the ContextNode of file to be open.
     */
    void setParametersOpen(UnPacker unPacker, ContextNode openNode) {
        this.unPacker = unPacker;
        targetDir = UncompressUI.tempDir;
        if (!UncompressUI.tempDir.exists()) {
            if (!UncompressUI.tempDir.mkdir()) System.out.println("Temp directory creation failed.");
        }
        totalSizeLabel.setText(Util.sizeToReadable(unPacker.getTotalOrigSize()));
        this.startNode = openNode;
        openAfterUnc = true;
    }

    void startUncompress() {
        service = new UncompressService();

        service.setOnSucceeded(e -> {
            unbindListeners();
            percentageLabel.setText("100.0");
            if (isTest) {
                if (testResult) showTestPassInfo();
                else showTestFailInfo();
            } else if (openAfterUnc) {
                openFile(new File(targetDir + File.separator + startNode.getName()));
            } else {
                showSuccessInfo();
            }

            grandParent.refreshAction();
            stage.close();
            System.gc();
        });

        service.setOnFailed(e -> {
            unbindListeners();

            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("WinLZZ");
            info.setHeaderText(bundle.getString("uncFailed"));
            info.setContentText(unPacker.getFailInfo());
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

        if (isTest) messageLabel.setText(bundle.getString("testing"));
        else messageLabel.setText(bundle.getString("uncIng"));

        service.start();
    }

    private void unbindListeners() {
        unPacker.progressProperty().removeListener(progressListener);
        unPacker.ratioProperty().removeListener(speedRatioListener);
        unPacker.percentageProperty().removeListener(percentageListener);
        unPacker.timeUsedProperty().removeListener(timeUsedListener);
        unPacker.timeExpectedProperty().removeListener(timeExpectedListener);
        unPacker.passedLengthProperty().removeListener(passedLengthListener);
        unPacker.stepProperty().removeListener(messageListener);
        unPacker.fileProperty().removeListener(fileListener);

        progressBar.progressProperty().unbind();
        ratioLabel.textProperty().unbind();
    }

    @FXML
    public void interruptAction() {
        if (!isTest) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("WinLZZ");
            alert.setHeaderText(bundle.getString("cancelUnc"));
            alert.setContentText(bundle.getString("confirmCancelUnc"));
            alert.showAndWait();
            if (alert.getResult() != ButtonType.OK) return;
        }
        service.cancel();
    }

    private void openFile(File f) {
        try {
            Desktop.getDesktop().open(f);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showSuccessInfo() {
        long timeUsed = System.currentTimeMillis() - startTime;
        messageLabel.setText(bundle.getString("uncSucceed"));

        Alert info = new Alert(Alert.AlertType.CONFIRMATION);
        info.setTitle("WinLZZ");
        double seconds = (double) timeUsed / 1000;
//        info.setHeaderText(lanLoader.get(554) + lanLoader.get(555) + " " + seconds + lanLoader.get(559));
        info.setHeaderText(String.format("%s%s %.2f %s",
                bundle.getString("uncDone"),
                bundle.getString("timeUsedTotal"),
                seconds,
                bundle.getString("seconds")));
        info.setContentText(bundle.getString("openTarFolder"));
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
        info.setHeaderText(bundle.getString("testDoneNoProblem"));
        info.setContentText(String.format("%s %.2f %s",
                bundle.getString("timeUsedTotal"),
                seconds,
                bundle.getString("seconds")));
//        info.setContentText(lanLoader.get(555) + " " + seconds + lanLoader.get(559));
        info.showAndWait();
    }

    private void showTestFailInfo() {
        long timeUsed = System.currentTimeMillis() - startTime;

        Alert info = new Alert(Alert.AlertType.ERROR);
        info.setTitle("WinLZZ");
        double seconds = (double) timeUsed / 1000;
        info.setHeaderText(bundle.getString("testFailFileDamaged"));
        info.setContentText(String.format("%s %.2f %s",
                bundle.getString("timeUsedTotal"),
                seconds,
                bundle.getString("seconds")));
        info.showAndWait();
    }

    private class UncompressService extends Service<Void> {

        @Override
        protected Task<Void> createTask() {
            return new Task<Void>() {
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
                    messageListener = (observable, oldValue, newValue) -> Platform.runLater(() -> messageLabel.setText(newValue));
                    fileListener = (observable, oldValue, newValue) -> Platform.runLater(() -> fileLabel.setText(newValue));

                    unPacker.progressProperty().addListener(progressListener);
                    unPacker.ratioProperty().addListener(speedRatioListener);
                    unPacker.percentageProperty().addListener(percentageListener);
                    unPacker.timeUsedProperty().addListener(timeUsedListener);
                    unPacker.timeExpectedProperty().addListener(timeExpectedListener);
                    unPacker.passedLengthProperty().addListener(passedLengthListener);
                    unPacker.stepProperty().addListener(messageListener);
                    unPacker.fileProperty().addListener(fileListener);

                    if (isTest) testResult = unPacker.TestPack();
                    else if (isAllUncompress) unPacker.unCompressAll(targetDir.getAbsolutePath());
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
        passedSizeLabel.setText("0 " + bundle.getString("byte"));

//        origSizeTextLabel.setText(lanLoader.get(500));
//        passedSizeTitleLabel.setText(lanLoader.get(501));
//        timeUsedTextLabel.setText(lanLoader.get(502));
//        timeRemainTextLabel.setText(lanLoader.get(503));
//        speedTextLabel.setText(lanLoader.get(505));
//        cancelButton.setText(lanLoader.get(2));
    }
}
