package trashsoftware.winBwz.gui.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import trashsoftware.winBwz.encrypters.WrongPasswordException;
import trashsoftware.winBwz.gui.graphicUtil.FileNode;
import trashsoftware.winBwz.packer.CatalogNode;
import trashsoftware.winBwz.packer.ChecksumDoesNotMatchException;
import trashsoftware.winBwz.packer.UnPacker;
import trashsoftware.winBwz.packer.UnsupportedVersionException;
import trashsoftware.winBwz.packer.pz.PzSolidPacker;
import trashsoftware.winBwz.packer.pz.PzSolidUnPacker;
import trashsoftware.winBwz.packer.pz.PzUnPacker;
import trashsoftware.winBwz.packer.pzNonSolid.PzNsUnPacker;
import trashsoftware.winBwz.packer.zip.ZipUnPacker;
import trashsoftware.winBwz.resourcesPack.configLoader.LoaderManager;
import trashsoftware.winBwz.utility.Util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class UncompressUI implements Initializable {

    static final File tempDir = new File("temp");

    @FXML
    private Label dirText;

    @FXML
    private Button goBackButton, uncompressPart, infoButton, annotationButton;

    @FXML
    private TableView<FileNode> fileList;

    @FXML
    private TableColumn<FileNode, String> nameColumn, origSizeColumn, typeColumn;

    private TableColumn<FileNode, String> cmpSizeColumn;

    @FXML
    private ComboBox<Integer> threadNumberBox;

    private Stage stage;
    private MainUI parent;
    private File packFile;
    private UnPacker unPacker;
    private CatalogNode currentNode;

    private ResourceBundle bundle;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.bundle = resources;
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("Name"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("Type"));
        origSizeColumn.setCellValueFactory(new PropertyValueFactory<>("Size"));

        fileListSelectionListener();
        backButtonHoverListener();
        infoButtonHoverListener();
    }

    void setParent(MainUI parent) {
        this.parent = parent;
    }

    void setStage(Stage stage) {
        this.stage = stage;
    }

    void setPackFile(File packFile) {
        this.packFile = packFile;
    }

    private void enableCmpSize() {
        cmpSizeColumn = new TableColumn<>(bundle.getString("cmpSize"));
        fileList.getColumns().add(cmpSizeColumn);
        cmpSizeColumn.setCellValueFactory(new PropertyValueFactory<>("cmpSize"));
    }

    void loadPzHead() throws Exception {
        int sigCheck = PzUnPacker.checkSignature(packFile.getAbsolutePath());
        if (sigCheck == 0 || sigCheck == 2) {
            if (sigCheck == 0)
                unPacker = new PzSolidUnPacker(packFile.getAbsolutePath(), bundle);
            else {
                unPacker = new PzNsUnPacker(packFile.getAbsolutePath(), bundle);
                enableCmpSize();
            }

            try {
                unPacker.readInfo();
            } catch (UnsupportedVersionException uve) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle(bundle.getString("cannotOpenFile"));
                alert.setHeaderText(bundle.getString("unsupportedVersion"));
                alert.setContentText(String.format("%s %s, %s %s",
                        bundle.getString("curSoftCoreVer"),
                        PzSolidPacker.getProgramFullVersion(),
                        bundle.getString("uncNeedCoreVer"),
                        ((PzUnPacker) unPacker).getArchiveFullVersion()));
                alert.showAndWait();
                stage.close();
                return;
            }
        } else if (sigCheck == 1) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(bundle.getString("fileNotFirstSection"));
            alert.setHeaderText(bundle.getString("pleaseOpenFirstSection"));
            alert.setContentText(String.format("%s %s",
                    bundle.getString("probFirstSection"),
                    probableFirstName()));
            alert.showAndWait();
            stage.close();
            return;
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(bundle.getString("cannotOpenFile"));
            alert.setHeaderText(bundle.getString("unsupportedFormat"));
            alert.setContentText(bundle.getString("notWinLzzArchive"));
            alert.showAndWait();
            stage.close();
            return;
        }
        dirText.setText("");
        setThreadNumberBox();
        if (unPacker.getEncryptLevel() != 2) showContext();
        else passwordInputAction(true);
    }

    void loadZipHead() {
        unPacker = new ZipUnPacker(packFile.getAbsolutePath(), bundle);
        enableCmpSize();
        try {
            unPacker.readInfo();
            dirText.setText("");
            setThreadNumberBox();
            showContext();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String probableFirstName() {
        String nameWithNumber = packFile.getName().substring(0, packFile.getName().lastIndexOf('.'));
        String pureName = nameWithNumber.substring(0, nameWithNumber.lastIndexOf('.'));
        return String.format("%s.1.pz", pureName);
    }

    private void setThreadNumberBox() {
        threadNumberBox.getItems().clear();
        if (unPacker.getAlg().equals("bwz")) threadNumberBox.getItems().addAll(1, 2, 3, 4);
        else threadNumberBox.getItems().add(1);
        threadNumberBox.getSelectionModel().select(0);
    }

    private void showContext() {
        try {
            unPacker.readFileStructure();
        } catch (ChecksumDoesNotMatchException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(bundle.getString("error"));
            alert.setHeaderText(bundle.getString("cannotOpenFile"));
            alert.setContentText(bundle.getString("damagedArchiveContext"));
            alert.showAndWait();
            stage.close();
            return;
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        currentNode = unPacker.getRootNode();
        showFiles();
        if (unPacker.getAnnotation() != null) annotationButton.setDisable(false);
    }

    private void showFiles() {
        fileList.getItems().clear();
        List<CatalogNode> contexts = currentNode.getChildren();
        if (contexts.isEmpty()) {
            fileList.setPlaceholder(new Label(bundle.getString("emptyFolder")));
        } else {
            fileList.setPlaceholder(new Label(bundle.getString("loading")));
            for (CatalogNode cn : currentNode.getChildren())
                fileList.getItems().add(new FileNode(cn, bundle));
        }
        String path = currentNode.getPath();
        if (path.startsWith(File.separator)) path = path.substring(1);
        dirText.setText(path);
    }

    @FXML
    private void goBackAction() {
        if (currentNode.getParent().getParent() == null) {
            goBackButton.setDisable(true);
        }
        currentNode = currentNode.getParent();
        showFiles();
    }

    @FXML
    private void annotationAction() {
        Stage st = new Stage();
        ScrollPane root = new ScrollPane();
        root.setPrefSize(400.0, 300.0);
        Scene scene = new Scene(root);
        st.setScene(scene);
        st.setTitle(bundle.getString("annotations"));

        Label label = new Label(unPacker.getAnnotation());
        root.setContent(label);
        st.show();
    }

    private void fileListSelectionListener() {

        fileList.setRowFactory(new Callback<TableView<FileNode>, TableRow<FileNode>>() {
            @Override
            public TableRow<FileNode> call(TableView<FileNode> param) {
                return new TableRow<FileNode>() {
                    @Override
                    protected void updateItem(FileNode item, boolean empty) {
                        super.updateItem(item, empty);

                        setOnMouseClicked(click -> {
                            if (click.getClickCount() == 2) {
                                CatalogNode cn = fileList.getSelectionModel().getSelectedItem().getContextNode();
                                if (cn.isDir()) {
                                    currentNode = cn;
                                    showFiles();
                                    goBackButton.setDisable(false);
                                } else {
                                    try {
                                        uncompressAndOpen(cn);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        });
                    }
                };
            }
        });

        fileList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) uncompressPart.setDisable(true);
            else uncompressPart.setDisable(false);
        });
    }

    @FXML
    public void fileInfoAction() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/trashsoftware/winBwz/fxml/fileInfoUI.fxml"), bundle);
        Parent root = loader.load();
        Stage stage = new Stage();
        stage.setTitle("WinLZZ");
        stage.setScene(new Scene(root));
        stage.setResizable(false);

        FileInfoUI fi = loader.getController();
        fi.setInfo(unPacker);
        stage.show();
    }

    @FXML
    public void uncompressAllAction() throws IOException {
        checkPassword();
        if (unPacker.isPasswordSet()) uncompressHandler(null, true);
    }

    @FXML
    public void uncompressPartAction() throws IOException {
        checkPassword();
        if (unPacker.isPasswordSet())
            uncompressHandler(fileList.getSelectionModel().getSelectedItem().getContextNode(), false);
    }

    private void uncompressAndOpen(CatalogNode openNode) throws IOException {
        checkPassword();
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/trashsoftware/winBwz/fxml/uncompressingUI.fxml"), bundle);
        Parent root = loader.load();
        Stage stage = new Stage();

        stage.setTitle("WinLZZ");
        stage.setScene(new Scene(root));
        stage.setResizable(false);

        UncompressingUI uui = loader.getController();
        uui.setStage(stage, this);
        uui.setGrandParent(parent);
        uui.setParametersOpen(unPacker, openNode);
        uui.setThreadNumber(threadNumberBox.getSelectionModel().getSelectedItem());

        stage.show();

        uui.startUncompress();
    }

    @FXML
    public void testAction() throws IOException {
        checkPassword();
        if (unPacker.isPasswordSet()) testHandler();
    }

    private void checkPassword() {
        if (unPacker.getEncryptLevel() == 1 && !unPacker.isPasswordSet()) passwordInputAction(false);
    }

    private void uncompressHandler(CatalogNode cn, boolean isAll) throws IOException {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setInitialDirectory(LoaderManager.getCacheSaver().readLastSelectedDir());
        File selected = dc.showDialog(null);
        if (selected != null) {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/trashsoftware/winBwz/fxml/uncompressingUI.fxml"), bundle);
            Parent root = loader.load();
            Stage stage = new Stage();

            stage.setTitle("WinLZZ");
            stage.setScene(new Scene(root));
            stage.setResizable(false);

            UncompressingUI uui = loader.getController();
            uui.setStage(stage, this);
            uui.setGrandParent(parent);
            if (isAll) uui.setParameters(unPacker, selected);
            else uui.setParameters(unPacker, selected, cn);
            uui.setThreadNumber(threadNumberBox.getSelectionModel().getSelectedItem());

            stage.show();

            uui.startUncompress();
        }
    }

    private void testHandler() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/trashsoftware/winBwz/fxml/uncompressingUI.fxml"), bundle);
        Parent root = loader.load();
        Stage stage = new Stage();

        stage.setTitle("WinLZZ");
        stage.setScene(new Scene(root));

        UncompressingUI uui = loader.getController();
        uui.setStage(stage, this);
        uui.setGrandParent(parent);
        uui.setTest();
        uui.setParameters(unPacker);
        uui.setThreadNumber(threadNumberBox.getSelectionModel().getSelectedItem());

        stage.show();
        uui.startUncompress();
    }

    void close() {
        try {
            unPacker.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Util.deleteDir(tempDir);
        this.stage.close();
        parent.refreshAction();
    }

    private void passwordInputAction(boolean isName) {
        Pane root = new Pane();

        Stage stage = new Stage();
        stage.setTitle(bundle.getString("inputPassword"));
        stage.setResizable(false);
        Scene scene = new Scene(root);
        stage.setScene(scene);

        VBox box = new VBox();
        box.setAlignment(Pos.CENTER);
        box.setPrefSize(320.0, 120.0);
        box.setPadding(new Insets(10.0, 10.0, 10.0, 10.0));
        box.setSpacing(5.0);

        PasswordField pwdField = new PasswordField();
        Label prompt = new Label();
        Button confirm = new Button(bundle.getString("confirm"));
        box.getChildren().addAll(new Label(bundle.getString("pleaseInputPassword")), pwdField, prompt, confirm);

        root.getChildren().addAll(box);

        if (isName) {
            stage.setOnCloseRequest(e -> close());
            confirm.setOnAction(event -> {
                try {
                    unPacker.setPassword(pwdField.getText());
                    showContext();
                    stage.close();
                } catch (WrongPasswordException wpe) {
                    prompt.setText(bundle.getString("wrongPassword"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } else {
            confirm.setOnAction(event -> {
                try {
                    unPacker.setPassword(pwdField.getText());
                    stage.close();
                } catch (WrongPasswordException wpe) {
                    prompt.setText(bundle.getString("wrongPassword"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        pwdField.setOnAction(event -> confirm.fire());  // press enter in password field

        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(this.stage);
        stage.showAndWait();
    }

    private void backButtonHoverListener() {
        goBackButton.hoverProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                Tooltip tt = new Tooltip();
                tt.setText(bundle.getString("backToPrevDir"));
                goBackButton.setTooltip(tt);
            } else {
                goBackButton.setTooltip(null);
            }
        });
    }

    private void infoButtonHoverListener() {
        infoButton.hoverProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                Tooltip tt = new Tooltip();
                tt.setText(bundle.getString("fileInfo"));
                infoButton.setTooltip(tt);
            } else {
                infoButton.setTooltip(null);
            }
        });
    }
}


