package BWGViewer.GUI;

import BWGViewer.Codecs.BMPCodec.BMPLoader;
import BWGViewer.Codecs.BWGCodec.BWGCoder;
import BWGViewer.Codecs.BWGCodec.BWGLoader;
import BWGViewer.Codecs.CommonLoader;
import BWGViewer.Codecs.DamagedBWGException;
import BWGViewer.Codecs.UnsupportedFormatException;
import WinLzz.ResourcesPack.ConfigLoader.GeneralLoaders;
import WinLzz.ResourcesPack.Languages.LanguageLoader;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.PixelWriter;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class Viewer implements Initializable {

    @FXML
    private VBox rootBox, paneBox;

    @FXML
    private Canvas canvas;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private Button zoomInButton, zoomOutButton, saveButton;

    @FXML
    private Label placeHolder, ratioLabel;

    @FXML
    private Menu fileMenu, helpMenu;

    @FXML
    private MenuItem saveMenuItem, openMenuItem, fileInfoMenuItem;

    private CommonLoader imageLoader;

    private Stage stage;
    private Scene scene;

    private LanguageLoader lanLoader = new LanguageLoader();

    private File pictureFile;
    private int screenWidth;
    private int imageViewHeight;

    private double[] ratios = new double[]{0.125, 0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0};
    private int zoomIndex = 4;

    private boolean changed;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        screenWidth = (int) (gd.getDisplayMode().getWidth() * 0.8);
        int screenHeight = (int) (gd.getDisplayMode().getHeight() * 0.8 - 20);
        imageViewHeight = screenHeight - 70;
        rootBox.setPrefSize(screenWidth, screenHeight);
        scrollPane.setPrefSize(screenWidth, imageViewHeight);
        paneBox.setPrefSize(screenWidth - 20, imageViewHeight - 20);

        saveButton.setDisable(true);
        saveMenuItem.setDisable(true);

        canvas.setHeight(0);  // To display the placeHolder label in the mid of screen.
        fillText();
        ratioLabel.setText("");
    }

    public void setScene(Scene scene) {
        this.scene = scene;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    void setPictureFile(File pictureFile) {
        canvas.getGraphicsContext2D().clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        canvas.setHeight(0);
        this.pictureFile = pictureFile;
        changed = true;
    }

    void loadPicture() {
        if (changed) {
            placeHolder.setVisible(true);
            placeHolder.setManaged(true);
            placeHolder.setText(lanLoader.get(1001));
        }

        Service<Void> loadService = new Service<>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        if (pictureFile.getName().endsWith(".bmp")) {
                            if (changed) {
                                loadBmp();
                                changed = false;
                            }
                        } else if (pictureFile.getName().endsWith(".bwg")) {
                            if (changed) {
                                loadBwg();
                                changed = false;
                            }
                        }
                        return null;
                    }
                };
            }
        };

        loadService.setOnSucceeded(e -> {
            saveMenuItem.setDisable(false);
            saveButton.setDisable(false);
            fileInfoMenuItem.setDisable(false);
            placeHolder.setVisible(false);
            placeHolder.setManaged(false);
            stage.setTitle(pictureFile.getName());
            drawImage();
            scene.setCursor(Cursor.DEFAULT);
        });

        loadService.setOnFailed(e -> {
            if (e.getSource().getException() instanceof DamagedBWGException) placeHolder.setText(lanLoader.get(1008));
            else placeHolder.setText(lanLoader.get(1002));
            stage.setTitle(pictureFile.getName());
            scene.setCursor(Cursor.DEFAULT);
        });

        scene.setCursor(Cursor.WAIT);
        loadService.start();
    }

    @FXML
    public void zoomInAction() {
        zoomIndex += 1;
        if (zoomIndex == 8) zoomInButton.setDisable(true);
        else zoomInButton.setDisable(false);
        if (zoomIndex == 0) zoomOutButton.setDisable(true);
        else zoomOutButton.setDisable(false);
        loadPicture();
        updateRatioLabel();
    }

    @FXML
    public void zoomOutAction() {
        zoomIndex -= 1;
        if (zoomIndex == 8) zoomInButton.setDisable(true);
        else zoomInButton.setDisable(false);
        if (zoomIndex == 0) zoomOutButton.setDisable(true);
        else zoomOutButton.setDisable(false);
        loadPicture();
        updateRatioLabel();
    }

    @FXML
    public void saveAction() throws IOException {
        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(GeneralLoaders.readLastDir());
        FileChooser.ExtensionFilter bwgFilter = new FileChooser.ExtensionFilter(lanLoader.get(1050), "*.bwg");
        fc.getExtensionFilters().addAll(bwgFilter);
        fc.setInitialFileName(pictureFile.getName().substring(0, pictureFile.getName().lastIndexOf(".")) +
                ".bwg");
        File selected = fc.showSaveDialog(null);
        if (selected != null) {
            GeneralLoaders.writeLastDir(selected);
            if (fc.getSelectedExtensionFilter() == bwgFilter) {
                fc.setInitialFileName(pictureFile.getName().substring(0, pictureFile.getName().lastIndexOf(".")) +
                        ".bwg");
                Service<Void> saveService = new Service<>() {
                    @Override
                    protected Task<Void> createTask() {
                        return new Task<>() {
                            @Override
                            protected Void call() throws Exception {
                                BWGCoder coder = new BWGCoder(imageLoader.getContent(), selected.getAbsolutePath());
                                coder.setCompression(2, 4194304);
                                coder.setParameters(imageLoader.getWidth(), imageLoader.getHeight(), 24);
                                coder.code();
                                return null;
                            }
                        };
                    }
                };

                saveService.setOnSucceeded(e -> scene.setCursor(Cursor.DEFAULT));
                saveService.setOnFailed(e -> {
                    e.getSource().getException().printStackTrace();
                    scene.setCursor(Cursor.DEFAULT);
                });

                scene.setCursor(Cursor.WAIT);
                saveService.start();
            }
        }
    }

    @FXML
    public void openAction() throws IOException {
        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(GeneralLoaders.readLastDir());
        FileChooser.ExtensionFilter allFilter =
                new FileChooser.ExtensionFilter(lanLoader.get(1052), "*.bmp", "*.bwg");
        FileChooser.ExtensionFilter bwgFilter =
                new FileChooser.ExtensionFilter(lanLoader.get(1050), "*.bwg");
        FileChooser.ExtensionFilter bmpFilter =
                new FileChooser.ExtensionFilter(lanLoader.get(1051), "*.bmp");
        fc.getExtensionFilters().addAll(allFilter, bwgFilter, bmpFilter);
        File selected = fc.showOpenDialog(null);
        if (selected != null) {
            zoomIndex = 4;
            GeneralLoaders.writeLastDir(selected);
            setPictureFile(selected);
            loadPicture();
            updateRatioLabel();
        }
    }

    @FXML
    public void fileInfoAction() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("imageInfoUI.fxml"));
        Parent root = loader.load();
        Stage stage = new Stage();
        stage.setTitle(lanLoader.get(1007));
        stage.setScene(new Scene(root));
        stage.setResizable(false);

        ImageInfoUI fi = loader.getController();
        fi.setInfo(imageLoader, pictureFile);
        fi.setLanLoader(lanLoader);
        fi.showInfo();
        stage.show();
    }

    private void drawImage() {
        int height = imageLoader.getHeight();
        int width = imageLoader.getWidth();
        int visualHeight = (int) (height * ratios[zoomIndex]);
        int visualWidth = (int) (width * ratios[zoomIndex]);
        canvas.setHeight(visualHeight);
        canvas.setWidth(visualWidth);

        scrollPane.setPrefSize(screenWidth, imageViewHeight);

        PixelWriter pw = canvas.getGraphicsContext2D().getPixelWriter();
        for (int ro = 0; ro < height; ro++) {
            for (int c = 0; c < width; c++) {
                int b = imageLoader.getContent()[(ro * width + c) * 3] & 0xff;
                int g = imageLoader.getContent()[(ro * width + c) * 3 + 1] & 0xff;
                int r = imageLoader.getContent()[(ro * width + c) * 3 + 2] & 0xff;
                int row = height - ro - 1;
                int x = (int) Math.round(c * ratios[zoomIndex]);
                int y = (int) Math.round(row * ratios[zoomIndex]);
                Color color = Color.rgb(r, g, b);
                pw.setColor(x, y, color);
                if (ratios[zoomIndex] > 1) {
                    pw.setColor(x, y + 1, color);
                    pw.setColor(x + 1, y + 1, color);
                    pw.setColor(x + 1, y, color);
                }
            }
        }
    }

    private void loadBmp() throws IOException, UnsupportedFormatException {
        imageLoader = new BMPLoader(pictureFile.getAbsolutePath());
        imageLoader.load();

    }

    private void loadBwg() throws IOException, UnsupportedFormatException {
        imageLoader = new BWGLoader(pictureFile.getAbsolutePath());
        imageLoader.load();
    }

    private void updateRatioLabel() {
        double percentage = ratios[zoomIndex] * 100;
        String text = String.valueOf(percentage);
        if (text.charAt(text.length() - 1) == '0') ratioLabel.setText(text.substring(0, text.length() - 2) + " %");
        else ratioLabel.setText(text + " %");
    }

    private void fillText() {
        placeHolder.setText(lanLoader.get(1000));
        fileMenu.setText(lanLoader.get(1003));
        helpMenu.setText(lanLoader.get(1005));
        saveMenuItem.setText(lanLoader.get(1004));
        saveButton.setText(lanLoader.get(1004));
        openMenuItem.setText(lanLoader.get(1006));
        fileInfoMenuItem.setText(lanLoader.get(1007));
    }
}
