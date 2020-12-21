package trashsoftware.winBwz.gui.controllers;

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
import trashsoftware.winBwz.gui.graphicUtil.AnnotationNode;
import trashsoftware.winBwz.packer.Packer;
import trashsoftware.winBwz.packer.SeparateException;
import trashsoftware.winBwz.packer.pz.PzSolidPacker;
import trashsoftware.winBwz.packer.pzNonSolid.PzNsPacker;
import trashsoftware.winBwz.packer.zip.ZipPacker;
import trashsoftware.winBwz.utility.Util;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class CompressingUI implements Initializable {

    @FXML
    ProgressBar progressBar;

    @FXML
    private Label messageLabel, percentageLabel, ratioLabel, timeUsedLabel, expectTimeLabel, totalSizeLabel,
            passedSizeLabel, cmpSizeTextLabel, currentCmpRatioTextLabel, compressedSizeLabel, currentCmpRatioLabel,
            fileLabel;

    private CompressService service;

    private ChangeListener<Number> progressListener, exitStatusListener;

    private ChangeListener<String> percentageListener, stepListener, fileListener, speedRatioListener, timeUsedListener,
            timeExpectedListener, passedLengthListener, cmpSizeListener, currentCmpRatioListener;

    private String name, alg;
    private File[] path;
    private int windowSize, bufferSize, cmpLevel, encryptLevel, threads;
    private CompressUI.FmtBoxItem fmt;
    private String password;
    private String encAlg;
    private String passAlg;
    private Packer packer;
    private AnnotationNode annotation;
    private long startTime;
    private long partSize;

    private MainUI grandParent;
    private Stage stage;
    //    private LanguageLoader lanLoader;
    private ResourceBundle bundle;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.bundle = resources;
        fillText();
    }

//    void setLanLoader(LanguageLoader lanLoader) {
//        this.lanLoader = lanLoader;
////        fillText();
//    }

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

    void setPref(CompressUI.FmtBoxItem format, int windowSize, int bufferSize, int compressionLevel, String algorithm, int threads,
                 AnnotationNode annotation, long partSize) {
        this.fmt = format;
        this.windowSize = windowSize;
        this.bufferSize = bufferSize;
        this.cmpLevel = compressionLevel;
        this.alg = algorithm;
        this.threads = threads;
        this.annotation = annotation;
        this.partSize = partSize;
        if (algorithm.equals("lzz2") || algorithm.equals("fastLzz")) {
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
            info.setHeaderText(bundle.getString("compressComplete"));
            double seconds = (double) timeUsed / 1000;
            double rounded;
            if (packer.getTotalOrigSize() == 0) {
                rounded = 0;
            } else {
                double compressRate = (double) packer.getCompressedLength() / packer.getTotalOrigSize();
                rounded = (double) Math.round(compressRate * 10000) / 100;
            }
            info.setContentText(String.format("%s %.2f %s, %s: %.2f%%",
                    bundle.getString("timeUsedTotal"),
                    seconds,
                    bundle.getString("second"),
                    bundle.getString("compressRateTotal"),
                    rounded
            ));
//            info.setContentText(lanLoader.get(252) + seconds + lanLoader.get(253) + " " + lanLoader.get(254)
//                    + ": " + rounded + "%");
            info.show();

            grandParent.refreshAction();
            stage.close();
            System.gc();
        });

        service.setOnFailed(e -> {
            unbindListeners();

            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("WinLZZ");
            info.setHeaderText(bundle.getString("compressFailed"));

            Throwable thr = e.getSource().getException();
            if (thr instanceof SeparateException) {
                info.setContentText(String.format(bundle.getString("fileStructureSpaceNotEnough"),
                        Util.sizeToReadable(((SeparateException) thr).getBytesRequired())));
            }

            info.show();
            System.gc();
            stage.close();

            Throwable exception = e.getSource().getException();
            exception.printStackTrace();
        });

        service.setOnCancelled(e -> {
            if (packer != null) packer.interrupt();
            unbindListeners();
            if (packer.exitStatusProperty().intValue() != 0) {
                Alert info = new Alert(Alert.AlertType.ERROR);
                info.setTitle("WinLZZ");
                info.setHeaderText(packer.getErrorMsg());

                info.show();
            }
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
        packer.exitStatusProperty().removeListener(exitStatusListener);

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
        alert.setHeaderText(bundle.getString("cancelCompress"));
        alert.setContentText(bundle.getString("confirmCancelCompress"));
        alert.showAndWait();
        if (alert.getResult() == ButtonType.OK) service.cancel();
    }

    private void fillText() {
        timeUsedLabel.setText("--:--");
        expectTimeLabel.setText("--:--");
        passedSizeLabel.setText("0 " + bundle.getString("byte"));
        compressedSizeLabel.setText("0 " + bundle.getString("byte"));
        currentCmpRatioLabel.setText("0.0%");

//        origSizeTextLabel.setText(lanLoader.get(200));
//        timeUsedTextLabel.setText(lanLoader.get(201));
//        passedSizeTextLabel.setText(lanLoader.get(202));
//        timeRemainTextLabel.setText(lanLoader.get(203));
//        cmpSizeTextLabel.setText(lanLoader.get(204));
//        currentCmpRatioTextLabel.setText(lanLoader.get(205));
//        speedTextLabel.setText(lanLoader.get(207));
//        cancelButton.setText(lanLoader.get(2));
//        messageLabel.setText(lanLoader.get(350));
    }

    private class CompressService extends Service<Void> {

        @Override
        protected Task<Void> createTask() {
            return new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    startTime = System.currentTimeMillis();
                    if (fmt == CompressUI.FmtBoxItem.PZ)
                        packer = new PzSolidPacker(path);
                    else if (fmt == CompressUI.FmtBoxItem.PZN)
                        packer = new PzNsPacker(path);
                    else if (fmt == CompressUI.FmtBoxItem.ZIP)
                        packer = new ZipPacker(path);
                    else
                        throw new RuntimeException("No such format " + fmt);

                    if (isCancelled()) return null;
                    packer.setCmpLevel(cmpLevel);
                    packer.setEncrypt(password, encryptLevel, encAlg, passAlg);
                    packer.setAlgorithm(alg);
                    packer.setThreads(threads);
                    packer.setPartSize(partSize);
                    packer.setResourceBundle(bundle);
                    if (annotation != null) packer.setAnnotation(annotation);

                    stepListener = (observable, oldValue, newValue) -> updateTitle(newValue);
                    fileListener = (observable, oldValue, newValue) ->
                            Platform.runLater(() -> fileLabel.setText(newValue));
                    speedRatioListener = (observable, oldValue, newValue) -> updateMessage(newValue);
                    percentageListener = (observable, oldValue, newValue) ->
                            Platform.runLater(() -> percentageLabel.setText(newValue));
                    timeUsedListener = (observable, oldValue, newValue) ->
                            Platform.runLater(() -> timeUsedLabel.setText(newValue));
                    timeExpectedListener = (observable, oldValue, newValue) ->
                            Platform.runLater(() -> expectTimeLabel.setText(newValue));
                    passedLengthListener = (observable, oldValue, newValue) ->
                            Platform.runLater(() -> passedSizeLabel.setText(newValue));
                    exitStatusListener = (observable, oldValue, newValue) -> {
                        Platform.runLater(() -> fileLabel.setText(packer.getErrorMsg()));
                        if (newValue.intValue() != 0) this.cancel();
                    };

                    // Listeners only work under BWT.
                    cmpSizeListener = (observable, oldValue, newValue) ->
                            Platform.runLater(() -> compressedSizeLabel.setText(newValue));
                    currentCmpRatioListener = (observable, oldValue, newValue) ->
                            Platform.runLater(() -> currentCmpRatioLabel.setText(newValue));

                    if (packer.stepProperty() != null)
                        packer.stepProperty().addListener(stepListener);
                    if (packer.fileProperty() != null)
                        packer.fileProperty().addListener(fileListener);
                    if (packer.ratioProperty() != null)
                        packer.ratioProperty().addListener(speedRatioListener);
                    if (packer.percentageProperty() != null)
                        packer.percentageProperty().addListener(percentageListener);
                    if (packer.timeUsedProperty() != null)
                        packer.timeUsedProperty().addListener(timeUsedListener);
                    if (packer.timeExpectedProperty() != null)
                        packer.timeExpectedProperty().addListener(timeExpectedListener);
                    if (packer.passedLengthProperty() != null)
                        packer.passedLengthProperty().addListener(passedLengthListener);

                    if (packer.compressedSizeProperty() != null)
                        packer.compressedSizeProperty().addListener(cmpSizeListener);
                    if (packer.currentCmpRatioProperty() != null)
                        packer.currentCmpRatioProperty().addListener(currentCmpRatioListener);
                    if (packer.exitStatusProperty() != null)
                        packer.exitStatusProperty().addListener(exitStatusListener);

                    updateTitle(bundle.getString("searching"));
                    Platform.runLater(() -> percentageLabel.setText("0.0"));
                    packer.build();

                    // Add progress bar
                    long totalLength = packer.getTotalOrigSize();
                    Platform.runLater(() -> totalSizeLabel.setText(Util.sizeToReadable(totalLength)));
                    progressListener = (observable, oldValue, newValue) ->
                            updateProgress(newValue.longValue(), packer.getTotalOrigSize());
                    packer.progressProperty().addListener(progressListener);

                    packer.pack(path[0].getParent() + File.separator + name, windowSize, bufferSize);
                    updateProgress(totalLength, totalLength);

                    return null;
                }
            };
        }
    }
}
