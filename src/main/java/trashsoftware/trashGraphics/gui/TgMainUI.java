package trashsoftware.trashGraphics.gui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import trashsoftware.trashGraphics.core.ImageViewer;
import trashsoftware.trashGraphics.core.TgiCoder;
import trashsoftware.winBwz.gui.controllers.MainUI;
import trashsoftware.winBwz.gui.graphicUtil.InfoBoxes;
import trashsoftware.winBwz.resourcesPack.configLoader.LoaderManager;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class TgMainUI implements Initializable {

    private static final double[] zoomLevels =
            {12.5, 25.0, 50.0, 75.0, 90.0, 100.0, 125.0, 150.0, 175.0, 200.0, 300.0, 500.0};
    private final Label blockerLabel = new Label();
    private final ImageViewer baseImageViewer = new ImageViewer();
    @FXML
    ImageView imageView;
    @FXML
    Label msgLabel;
    @FXML
    MenuItem showHideToolbar;
    @FXML
    VBox toolbar;
    @FXML
    HBox filtersBar;
    @FXML
    RowConstraints toolbarRow;
    @FXML
    Label zoomRatioLabel;
    @FXML
    Button zoomInBtn, zoomOutBtn;
    private ResourceBundle bundle;
    private int currentZoomIndex = 5;

    private FileChooser.ExtensionFilter tgi8, tgi16, tgi24, tgi32, tgi4gray, tgi8gray;

    private Stage thisStage;
    private Scene thisScene;
    private MainUI parent;
    private String initFileName;
    private boolean toolbarShown = false;
    private Stage blocker;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.bundle = resources;

        initExtensionFilters();
    }

    public void setStage(Stage thisStage, Scene thisScene, MainUI parent) {
        this.parent = parent;
        this.thisStage = thisStage;
        this.thisScene = thisScene;
        initBlocker(thisStage);
    }

    void setInitImage(File file) {
        Thread loadThread = new Thread(() -> {
            if (!baseImageViewer.addLayer(file.getAbsolutePath())) {
                System.out.println("Error initializing image");
                loadFailed();
                return;
            }
            initFileName = file.getName();
            try {
                refreshImage();
            } catch (IOException e) {
                e.printStackTrace();
                loadFailed();
            }
        });
        loadThread.start();
    }

    public void showLoadingBlocker() {
        showBlocker(bundle.getString("loading"));
    }

    public void refreshImage() throws IOException {
        baseImageViewer.show(imageView);
        loadSuccess();
    }

    private void loadSuccess() {
        Platform.runLater(() -> {
//            loadingLabel.setVisible(false);
            blocker.close();
        });
    }

    private void loadFailed() {
        Platform.runLater(() -> {
            msgLabel.setText(bundle.getString("cannotShowImage"));
            msgLabel.setVisible(true);
            msgLabel.setManaged(true);
            blocker.close();
        });
    }

    private void initBlocker(Stage thisStage) {
        blocker = new Stage();
        blocker.initOwner(thisStage);
        blocker.initModality(Modality.WINDOW_MODAL);
        blocker.initStyle(StageStyle.UNDECORATED);

        HBox box = new HBox();
        box.setAlignment(Pos.CENTER);
        box.setSpacing(10.0);

        ProgressIndicator pi = new ProgressIndicator();

        box.getChildren().addAll(pi, blockerLabel);
        Scene scene = new Scene(box);

        blocker.setScene(scene);

        blocker.setWidth(200.0);
        blocker.setHeight(100.0);
    }

    @FXML
    void saveAction() {

    }

    @FXML
    void saveAsAction() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(LoaderManager.getCacheSaver().readLastSelectedDir());
        fileChooser.setInitialFileName(initFileName);

        fileChooser.getExtensionFilters().addAll(tgi8, tgi16, tgi24, tgi32, tgi4gray, tgi8gray);
        File file = fileChooser.showSaveDialog(thisStage);
        if (file != null) {
            FileChooser.ExtensionFilter extensionFilter = fileChooser.getSelectedExtensionFilter();
            String extension = extensionFilter.getExtensions().get(0);
            if (extension.equals("*.tgi")) {
                int bitDepth;
                boolean colored = true;
                if (extensionFilter == tgi8) bitDepth = 8;
                else if (extensionFilter == tgi16) bitDepth = 16;
                else if (extensionFilter == tgi24) bitDepth = 24;
                else if (extensionFilter == tgi32) bitDepth = 32;
                else if (extensionFilter == tgi4gray) {
                    bitDepth = 4;
                    colored = false;
                } else if (extensionFilter == tgi8gray) {
                    bitDepth = 8;
                    colored = false;
                } else bitDepth = 0;

                thisScene.setCursor(Cursor.WAIT);
                showSavingBlocker();
                final boolean finalColored = colored;
                Thread thread = new Thread(() -> {
                    try {
                        saveAsTgiImage(bitDepth, finalColored, file);
                    } catch (Exception e) {
                        e.printStackTrace();
                        e.printStackTrace();
                        Platform.runLater(() -> {
                            InfoBoxes.showError(bundle.getString("error"),
                                    bundle.getString("failedToSave"),
                                    bundle.getString("failedToSave"));
                            Platform.runLater(() -> thisScene.setCursor(Cursor.DEFAULT));
                        });
                    }
                    Platform.runLater(() -> {
                        thisScene.setCursor(Cursor.DEFAULT);
                        blocker.close();
                    });
                });
                thread.start();

                parent.refreshAction();
            }
        }
    }

    @FXML
    void showHideToolbarAction() {
        if (toolbarShown) {
            toolbarRow.setPrefHeight(0.0);
            toolbar.setVisible(false);
            toolbar.setManaged(false);
            showHideToolbar.setText(bundle.getString("showToolbar"));
        } else {
            toolbarRow.setPrefHeight(30.0);
            toolbar.setVisible(true);
            toolbar.setManaged(true);
            showHideToolbar.setText(bundle.getString("hideToolbar"));
        }
        toolbarShown = !toolbarShown;
    }

    @FXML
    void zoomInAction() {
        currentZoomIndex += 1;
        zoomImage();
        refreshZoomLabelAndBtn();
    }

    @FXML
    void zoomOutAction() {
        currentZoomIndex -= 1;
        zoomImage();
        refreshZoomLabelAndBtn();
    }

    @FXML
    void infoAction() {

    }

    @FXML
    void showFilterBarAction() {
        if (!filtersBar.isVisible()) {
            toolbarRow.setPrefHeight(toolbarRow.getPrefHeight() + 40.0);
            filtersBar.setManaged(true);
            filtersBar.setVisible(true);
        }
    }

    @FXML
    void hideFilterBarAction() {
        filtersBar.setManaged(false);
        filtersBar.setVisible(false);
        toolbarRow.setPrefHeight(toolbarRow.getPrefHeight() - 40.0);
    }

    @FXML
    void grayScaleAction() throws IOException {
        showBlocker(bundle.getString("processingImage"));

        refreshImage();
    }

    @FXML
    void antiColorAction() throws IOException {
        showBlocker(bundle.getString("processingImage"));
        baseImageViewer.toAntiColor();
        refreshImage();
    }

    private void zoomImage() {
        int origW = baseImageViewer.getBaseWidth();
        int newW = (int) (zoomLevels[currentZoomIndex] / 100 * origW);
        imageView.setFitWidth(newW);
        imageView.setPreserveRatio(true);
    }

    private void refreshZoomLabelAndBtn() {
        double zoomPercentage = zoomLevels[currentZoomIndex];
        zoomRatioLabel.setText(
                (zoomPercentage == (int) zoomPercentage ?
                        String.valueOf((int) zoomPercentage) : String.valueOf(zoomPercentage))
                        + "%");
        if (currentZoomIndex == 0) {
            zoomOutBtn.setDisable(true);
        } else if (currentZoomIndex == zoomLevels.length - 1) {
            zoomInBtn.setDisable(true);
        } else {
            zoomOutBtn.setDisable(false);
            zoomInBtn.setDisable(false);
        }
    }

    private void showBlocker(String text) {
        blockerLabel.setText(text);
        blocker.show();
    }

    private void showSavingBlocker() {
        showBlocker(bundle.getString("saving"));
    }

    private void saveAsTgiImage(int bitDepth, boolean colored, File savedFile) throws Exception {
        TgiCoder coder = new TgiCoder(baseImageViewer);
        coder.save(bitDepth, colored, savedFile.getAbsolutePath());
    }

    private void initExtensionFilters() {
        tgi8 = new FileChooser.ExtensionFilter(bundle.getString("extTgi8bits"), "*.tgi");
        tgi16 = new FileChooser.ExtensionFilter(bundle.getString("extTgi16bits"), "*.tgi");
        tgi24 = new FileChooser.ExtensionFilter(bundle.getString("extTgi24bits"), "*.tgi");
        tgi32 = new FileChooser.ExtensionFilter(bundle.getString("extTgi32bits"), "*.tgi");
        tgi4gray = new FileChooser.ExtensionFilter(bundle.getString("extTgi4bitsGray"), "*.tgi");
        tgi8gray = new FileChooser.ExtensionFilter(bundle.getString("extTgi8bitsGray"), "*.tgi");
    }
}
