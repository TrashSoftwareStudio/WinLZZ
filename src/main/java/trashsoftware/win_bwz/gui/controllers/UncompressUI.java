package trashsoftware.win_bwz.gui.controllers;

import trashsoftware.win_bwz.gui.graphicUtil.FileNode;
import trashsoftware.win_bwz.packer.*;
import trashsoftware.win_bwz.resourcesPack.configLoader.GeneralLoaders;
import trashsoftware.win_bwz.resourcesPack.languages.LanguageLoader;
import trashsoftware.win_bwz.utility.Util;
import trashsoftware.win_bwz.encrypters.WrongPasswordException;
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
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class UncompressUI implements Initializable {

    static final File tempDir = new File("temp");

    @FXML
    private Label dirText, threadNumberLabel;

    @FXML
    private Button goBackButton, uncompressPart, uncompressAll, infoButton, testButton, annotationButton;

    @FXML
    private TableView<FileNode> fileList;

    @FXML
    private TableColumn<FileNode, String> nameColumn, origSizeColumn, typeColumn;

    @FXML
    private ComboBox<Integer> threadNumberBox;

    private Stage stage;
    private MainUI parent;
    private File packFile;
    private UnPacker unPacker;
    private ContextNode currentNode;
    private LanguageLoader lanLoader;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
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

    void setLanLoader(LanguageLoader lanLoader) {
        this.lanLoader = lanLoader;
        fileList.setPlaceholder(new Label(lanLoader.get(350)));
        fillText();
    }

    void setStage(Stage stage) {
        this.stage = stage;
    }

    void setPackFile(File packFile) {
        this.packFile = packFile;
    }

    void loadContext() throws Exception {
        int sigCheck = UnPacker.checkSignature(packFile.getAbsolutePath());
        if (sigCheck == 0) {
            unPacker = new UnPacker(packFile.getAbsolutePath());
            unPacker.setLanguageLoader(lanLoader);

            try {
                unPacker.readInfo();
            } catch (UnsupportedVersionException uve) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle(lanLoader.get(351));
                alert.setHeaderText(lanLoader.get(352));
                alert.setContentText(lanLoader.get(353) + Packer.primaryVersion + lanLoader.get(354) +
                        unPacker.versionNeeded());
                alert.showAndWait();
                stage.close();
                return;
            }
        } else if (sigCheck == 1) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(lanLoader.get(370));
            alert.setHeaderText(lanLoader.get(371));
            alert.setContentText(String.format("%s %s", lanLoader.get(372), probableFirstName()));
            alert.showAndWait();
            stage.close();
            return;
        } else if (sigCheck == 2) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(lanLoader.get(351));
            alert.setHeaderText(lanLoader.get(361));
            alert.setContentText(lanLoader.get(362));
            alert.showAndWait();
            stage.close();
            return;
        }
        dirText.setText("");
        setThreadNumberBox();
        if (unPacker.getEncryptLevel() != 2) showContext();
        else passwordInputAction(true);
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
            unPacker.readMap();
        } catch (ChecksumDoesNotMatchException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(lanLoader.get(60));
            alert.setHeaderText(lanLoader.get(351));
            alert.setContentText(lanLoader.get(363));
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
        ArrayList<ContextNode> contexts = currentNode.getChildren();
        if (contexts.isEmpty()) {
            fileList.setPlaceholder(new Label(lanLoader.get(355)));
        } else {
            fileList.setPlaceholder(new Label(lanLoader.get(350)));
            for (ContextNode cn : currentNode.getChildren()) fileList.getItems().add(new FileNode(cn, lanLoader));
        }
        String path = currentNode.getPath();
        if (path.length() > 0) path = path.substring(1);
        dirText.setText(path);
    }

    @FXML
    private void goBackAction() {
        if (currentNode.getParent().getParent() == null) {
            dirText.setText("");
            fileList.getItems().clear();
            fileList.getItems().add(new FileNode(currentNode, lanLoader));
            goBackButton.setDisable(true);
        } else {
            currentNode = currentNode.getParent();
            showFiles();
        }
    }

    @FXML
    private void annotationAction() {
        Stage st = new Stage();
        ScrollPane root = new ScrollPane();
        root.setPrefSize(400.0, 300.0);
        Scene scene = new Scene(root);
        st.setScene(scene);
        st.setTitle(lanLoader.get(900));

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
                                ContextNode cn = fileList.getSelectionModel().getSelectedItem().getContextNode();
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
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/trashsoftware/win_bwz/fxml/fileInfoUI.fxml"));
        Parent root = loader.load();
        Stage stage = new Stage();
        stage.setTitle("WinLZZ");
        stage.setScene(new Scene(root));
        stage.setResizable(false);

        FileInfoUI fi = loader.getController();
        fi.setInfo(packFile, unPacker);
        fi.setLanLoader(lanLoader);
        fi.setItems();
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

    private void uncompressAndOpen(ContextNode openNode) throws IOException {
        checkPassword();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/trashsoftware/win_bwz/fxml/uncompressingUI.fxml"));
        Parent root = loader.load();
        Stage stage = new Stage();

        stage.setTitle("WinLZZ");
        stage.setScene(new Scene(root));
        stage.setResizable(false);

        UncompressingUI uui = loader.getController();
        uui.setStage(stage, this);
        uui.setGrandParent(parent);
        uui.setLanLoader(lanLoader);
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

    private void uncompressHandler(ContextNode cn, boolean isAll) throws IOException {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setInitialDirectory(GeneralLoaders.readLastDir());
        File selected = dc.showDialog(null);
        if (selected != null) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/trashsoftware/win_bwz/fxml/uncompressingUI.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();

            stage.setTitle("WinLZZ");
            stage.setScene(new Scene(root));
            stage.setResizable(false);

            UncompressingUI uui = loader.getController();
            uui.setStage(stage, this);
            uui.setGrandParent(parent);
            uui.setLanLoader(lanLoader);
            if (isAll) uui.setParameters(unPacker, selected);
            else uui.setParameters(unPacker, selected, cn);
            uui.setThreadNumber(threadNumberBox.getSelectionModel().getSelectedItem());

            stage.show();

            uui.startUncompress();
        }
    }

    private void testHandler() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/trashsoftware/win_bwz/fxml/uncompressingUI.fxml"));
        Parent root = loader.load();
        Stage stage = new Stage();

        stage.setTitle("WinLZZ");
        stage.setScene(new Scene(root));

        UncompressingUI uui = loader.getController();
        uui.setStage(stage, this);
        uui.setLanLoader(lanLoader);
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
        stage.setTitle(lanLoader.get(356));
        stage.initStyle(StageStyle.UTILITY);
        Scene scene = new Scene(root);
        stage.setScene(scene);

        VBox box = new VBox();
        box.setAlignment(Pos.CENTER);
        box.setPrefSize(180.0, 100.0);
        box.setPadding(new Insets(10.0, 10.0, 10.0, 10.0));
        box.setSpacing(5.0);

        PasswordField pwdField = new PasswordField();
        Label prompt = new Label();
        Button confirm = new Button(lanLoader.get(1));
        box.getChildren().addAll(new Label(lanLoader.get(357)), pwdField, prompt, confirm);

        root.getChildren().addAll(box);

        if (isName) {
            stage.setOnCloseRequest(e -> close());
            confirm.setOnAction(event -> {
                try {
                    unPacker.setPassword(pwdField.getText());
                    showContext();
                    stage.close();
                } catch (WrongPasswordException wpe) {
                    prompt.setText(lanLoader.get(358));
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
                    prompt.setText(lanLoader.get(358));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        stage.showAndWait();
    }

    private void backButtonHoverListener() {
        goBackButton.hoverProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                Tooltip tt = new Tooltip();
                tt.setText(lanLoader.get(359));
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
                tt.setText(lanLoader.get(360));
                infoButton.setTooltip(tt);
            } else {
                infoButton.setTooltip(null);
            }
        });
    }

    private void fillText() {
        nameColumn.setText(lanLoader.get(300));
        typeColumn.setText(lanLoader.get(301));
        origSizeColumn.setText(lanLoader.get(302));
        testButton.setText(lanLoader.get(303));
        threadNumberLabel.setText(lanLoader.get(304));
        uncompressPart.setText(lanLoader.get(305));
        uncompressAll.setText(lanLoader.get(306));
        annotationButton.setText(lanLoader.get(900));
    }
}

