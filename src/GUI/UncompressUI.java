package GUI;

import Packer.*;
import ZSE.WrongPasswordException;
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

    @FXML
    Label dirText;

    @FXML
    Button goBackButton;

    @FXML
    Button uncompressPart;

    @FXML
    Button infoButton;

    @FXML
    private TableView<FileNode> fileList;

    @FXML
    private TableColumn<FileNode, String> nameColumn;

    @FXML
    private TableColumn<FileNode, String> origSizeColumn;

    @FXML
    private TableColumn<FileNode, String> typeColumn;

    private Stage stage;

    private File packFile;

    private UnPacker unPacker;

    private ContextNode currentNode;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        fileList.setPlaceholder(new Label("正在读取..."));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("Name"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("Type"));
        origSizeColumn.setCellValueFactory(new PropertyValueFactory<>("Size"));

        fileListSelectionListener();
        backButtonHoverListener();
        infoButtonHoverListener();
    }

    void setStage(Stage stage) {
        this.stage = stage;
    }

    void setPackFile(File packFile) {
        this.packFile = packFile;
    }

    void loadContext() throws Exception {
        unPacker = new UnPacker(packFile.getAbsolutePath());
        try {
            unPacker.readInfo();
        } catch (UnsupportedVersionException uve) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("无法打开文件");
            alert.setHeaderText("不兼容的压缩文件版本");
            alert.setContentText("当前软件内核版本：" + Packer.version + "，解压所需内核版本：" + unPacker.versionNeeded());

            alert.showAndWait();

            stage.close();
            return;
        }
        dirText.setText("");
        if (unPacker.getEncryptLevel() != 2) {
            showContext();
        } else {
            passwordInputAction(true);
        }
    }

    private void showContext() {
        try {
            unPacker.readMap();
        } catch (Exception e) {
            e.printStackTrace();
        }
        currentNode = unPacker.getRootNode();
        fileList.getItems().add(new FileNode(currentNode));
    }

    private void showFiles() {
        fileList.getItems().clear();
        ArrayList<ContextNode> contexts = currentNode.getChildren();
        if (contexts.isEmpty()) {
            fileList.setPlaceholder(new Label("空文件夹"));
        } else {
            fileList.setPlaceholder(new Label("正在读取"));
            for (ContextNode cn : currentNode.getChildren()) {
                FileNode fn = new FileNode(cn);
                fileList.getItems().add(fn);
            }
        }
        dirText.setText(currentNode.getPath());
    }

    @FXML
    private void goBackAction() {
        if (currentNode.getParent() == null) {
            dirText.setText("");
            fileList.getItems().clear();
            fileList.getItems().add(new FileNode(currentNode));
            goBackButton.setDisable(true);
        } else {
            currentNode = currentNode.getParent();
            showFiles();
        }
    }

    private void fileListSelectionListener() {
        fileList.setRowFactory(new Callback<>() {
            @Override
            public TableRow<FileNode> call(TableView<FileNode> param) {
                return new TableRow<>() {
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
                                        uncompressPartAction();
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
            if (newValue == null) {
                uncompressPart.setDisable(true);
            } else {
                uncompressPart.setDisable(false);
            }
        });
    }

    @FXML
    public void fileInfoAction() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("fileInfoUI.fxml"));

        Parent root = loader.load();

        Stage stage = new Stage();

        stage.setTitle("WinLZZ");
        stage.setScene(new Scene(root));

        FileInfoUI fi = loader.getController();
        fi.setInfo(packFile, unPacker);

        fi.setItems();
        stage.show();
    }

    @FXML
    public void uncompressAllAction() throws IOException {
        checkPassword();
        uncompressHandler(unPacker.getRootNode());
    }

    @FXML
    public void uncompressPartAction() throws IOException {
        checkPassword();
        uncompressHandler(fileList.getSelectionModel().getSelectedItem().getContextNode());
    }

    @FXML
    public void testAction() throws IOException {
        checkPassword();
        testHandler();
    }

    private void checkPassword() {
        if (unPacker.getEncryptLevel() == 1 && !unPacker.isPasswordSet()) {
            passwordInputAction(false);
        }
    }

    private void uncompressHandler(ContextNode cn) throws IOException {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setInitialDirectory(StartUI.readLastDir());
        File selected = dc.showDialog(null);
        if (selected != null) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("uncompressingUI.fxml"));

            Parent root = loader.load();

            Stage stage = new Stage();

            stage.setTitle("WinLZZ");
            stage.setScene(new Scene(root));

            UncompressingUI uui = loader.getController();
            uui.setStage(stage);
            uui.setParameters(unPacker, selected, cn);

            stage.show();

            uui.startUncompress();
        }
    }

    private void testHandler() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("uncompressingUI.fxml"));

        Parent root = loader.load();

        Stage stage = new Stage();

        stage.setTitle("WinLZZ");
        stage.setScene(new Scene(root));

        UncompressingUI uui = loader.getController();
        uui.setStage(stage);
        uui.setTest();
        uui.setParameters(unPacker);

        stage.show();

        uui.startUncompress();
    }

    void close() {
        unPacker.close();
    }

    private void passwordInputAction(boolean isName) {
        Pane root = new Pane();

        Stage stage = new Stage();
        stage.setTitle("输入密码");
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
        Button confirm = new Button("确认");
        box.getChildren().addAll(new Label("请输入密码"), pwdField, prompt, confirm);

        root.getChildren().addAll(box);

        stage.setOnCloseRequest(e -> {
            close();
            this.stage.close();
        });

        if (isName) {
            confirm.setOnAction(event -> {
                try {
                    unPacker.setPassword(pwdField.getText());
                    showContext();
                    stage.close();
                } catch (WrongPasswordException wpe) {
                    prompt.setText("密码错误");
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
                    prompt.setText("密码错误");
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
                tt.setText("返回上级目录");
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
                tt.setText("文件信息");
                infoButton.setTooltip(tt);
            } else {
                infoButton.setTooltip(null);
            }
        });
    }
}


