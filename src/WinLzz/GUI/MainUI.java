package WinLzz.GUI;

import BWGViewer.GUI.Launcher;
import WinLzz.GraphicUtil.FileTreeItem;
import WinLzz.GraphicUtil.ReadableSize;
import WinLzz.GraphicUtil.RegularFileNode;
import WinLzz.GraphicUtil.SpecialFile;
import WinLzz.ResourcesPack.ConfigLoader.GeneralLoaders;
import WinLzz.ResourcesPack.Languages.LanguageLoader;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public class MainUI implements Initializable {

    private final static String LICENCE = "    WinLZZ\n" +
            "    Copyright (C) 2017-2018  Trash Software Studio\n" +
            "\n" +
            "    This program is free software: you can redistribute it and/or modify\n" +
            "    it under the terms of the GNU General Public License as published by\n" +
            "    the Free Software Foundation, either version 3 of the License, or\n" +
            "    (at your option) any later version.\n" +
            "\n" +
            "    This program is distributed in the hope that it will be useful,\n" +
            "    but WITHOUT ANY WARRANTY; without even the implied warranty of\n" +
            "    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n" +
            "    GNU General Public License for more details.\n" +
            "\n" +
            "    You should have received a copy of the GNU General Public License\n" +
            "    along with this program.  If not, see <https://www.gnu.org/licenses/>.";

    @FXML
    private TreeView<File> rootTree;

    @FXML
    private TableView<RegularFileNode> table;

    @FXML
    private TableColumn<RegularFileNode, String> nameCol, typeCol, timeCol;

    @FXML
    private TableColumn<RegularFileNode, ReadableSize> sizeCol;

    @FXML
    private Button backButton, refreshButton, compressButton, uncompressButton;

    @FXML
    private Menu settingsMenu, toolMenu, helpMenu;

    @FXML
    private Label currentDirLabel;

    @FXML
    private MenuItem languageSetting, about, licence, changelogView, bwgViewer, openInDesktop;

    private Label placeHolder = new Label();

    private LanguageLoader lanLoader = new LanguageLoader();
    private RegularFileNode currentSelection;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        setTree();
        setTreeListener();
        rootTree.getRoot().setExpanded(true);
        fillText();
        setNameColHoverFactory();
        nameCol.setCellValueFactory(new PropertyValueFactory<>("Name"));
        typeCol.setCellValueFactory(new PropertyValueFactory<>("Type"));
        sizeCol.setCellValueFactory(new PropertyValueFactory<>("Size"));
        timeCol.setCellValueFactory(new PropertyValueFactory<>("LastModified"));
        setTableListener();
        table.setPlaceholder(placeHolder);
        File f = GeneralLoaders.readLastDir();
        if (f != null) {
            currentSelection = new RegularFileNode(f, lanLoader);
            fillTable();
        }
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }


    /**
     * Sets up the content of the TreeView object "rootTree".
     */
    private void setTree() {
        TreeItem<File> rootNode = new TreeItem<>(new File(System.getenv("COMPUTERNAME")));
        for (File file : File.listRoots()) {
            FileTreeItem rootItems = new FileTreeItem(new SpecialFile(file.getAbsolutePath()));
            rootNode.getChildren().add(rootItems);
        }
        rootTree.setRoot(rootNode);
    }


    @FXML
    public void aboutAction() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("aboutUI.fxml"));
        Parent root = loader.load();
        AboutUI aui = loader.getController();
        aui.setLanLoader(lanLoader);
        Stage stage = new Stage();
        stage.setScene(new Scene(root));
        stage.setResizable(false);
        stage.show();
    }

    @FXML
    public void licenceAction() {
        Pane root = new Pane();
        Stage dialog = new Stage();
        Scene scene = new Scene(root);
        dialog.setTitle(lanLoader.get(17));
        dialog.setScene(scene);

        VBox pane = new VBox();
        pane.setFillWidth(true);
        pane.setPrefSize(480.0, 280.0);
        pane.setAlignment(Pos.CENTER);

        Label label = new Label(LICENCE);
        label.setTextAlignment(TextAlignment.CENTER);
        pane.getChildren().add(label);
        root.getChildren().add(pane);
        dialog.setResizable(false);

        dialog.show();
    }

    @FXML
    public void changelogAction() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("changelogViewer.fxml"));
        Parent root = loader.load();
        ChangelogViewer clv = loader.getController();
        clv.setLanLoader(lanLoader);
        Stage stage = new Stage();
        stage.setTitle("WinLZZ");
        stage.setScene(new Scene(root));
        stage.setResizable(false);
        stage.show();
    }

    @FXML
    public void languageSelection() {
        Stage lsStage = new Stage();
        HBox pane = new HBox();
        pane.setSpacing(10.0);
        pane.setPadding(new Insets(10.0));
        ComboBox<String> languageBox = new ComboBox<>();
        languageBox.getItems().addAll(lanLoader.getAllLanguageNames());
        languageBox.getSelectionModel().select(lanLoader.getCurrentLanguage());

        Button confirm = new Button(lanLoader.get(15));  // Confirm
        confirm.setOnAction(e -> {
            if (!lanLoader.changeLanguage(languageBox.getSelectionModel().getSelectedItem()))
                System.out.println("Failed to change language");
            fillText();
            lsStage.close();
            fillTable();
        });
        pane.getChildren().addAll(languageBox, confirm);
        Scene scene = new Scene(pane);
        lsStage.setScene(scene);
        lsStage.setResizable(false);
        lsStage.show();
    }


    @FXML
    public void compressMode() throws Exception {
        ObservableList<RegularFileNode> selections = table.getSelectionModel().getSelectedItems();
        File[] selected = new File[selections.size()];
        for (int i = 0; i < selections.size(); i++) selected[i] = selections.get(i).getFile();
        if (selected.length > 0) {
            GeneralLoaders.writeLastDir(selected[0]);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("compressUI.fxml"));

            Parent root = loader.load();
            Stage stage = new Stage();

            stage.setTitle("WinLZZ");
            stage.setScene(new Scene(root));
            stage.setResizable(false);

            CompressUI cui = loader.getController();
            cui.setDir(selected);
            cui.setStage(stage);
            cui.load(lanLoader);
            stage.show();
        }
    }

    @FXML
    public void backAction() {
        currentSelection = new RegularFileNode(currentSelection.getFile().getParentFile(), lanLoader);
        fillTable();
        backButtonListener();
    }

    @FXML
    public void openAction() throws Exception {
        RegularFileNode rfn = table.getSelectionModel().getSelectedItem();
        switch (rfn.getExtension()) {
            case "pz":
                uncompressMode(rfn.getFile());
                break;
            case "bwg":
                bwgImageViewer(rfn.getFile());
                break;
            default:
                try {
                    Desktop.getDesktop().open(rfn.getFile());
                } catch (IOException ioe) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle(lanLoader.get(60));
                    alert.setHeaderText(lanLoader.get(61));
                    alert.setContentText(lanLoader.get(62));
                    alert.showAndWait();
                }
                break;
        }
    }

    @FXML
    public void bwgViewerAction() throws IOException {
        bwgImageViewer(null);
    }

    @FXML
    public void refreshAction() {
        String dir = currentDirLabel.getText();
        if (dir.length() > 0) currentSelection = new RegularFileNode(new File(dir), lanLoader);
        else currentSelection = null;
        fillTable();
    }

    @FXML
    public void desktopOpenAction() {
        try {
            String dir = currentDirLabel.getText();
            if (dir.length() > 0) {
                Desktop.getDesktop().open(new File(dir));
            } else {
                Runtime.getRuntime().exec("cmd /c start explorer");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void uncompressMode(File selected) throws Exception {
        if (selected != null) {
            GeneralLoaders.writeLastDir(selected);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("uncompressUI.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("WinLZZ");
            stage.setScene(new Scene(root));
            stage.setResizable(false);

            UncompressUI uui = loader.getController();
            uui.setPackFile(selected);
            uui.setStage(stage);
            uui.setLanLoader(lanLoader);

            stage.setOnCloseRequest(event -> uui.close());
            stage.show();
            try {
                uui.loadContext();
            } catch (Exception e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle(lanLoader.get(51));
                alert.setHeaderText(lanLoader.get(52));
                alert.setContentText(lanLoader.get(53));
                alert.showAndWait();
                stage.close();
            }
        }
    }


    /**
     * Sets up the selection listener of the TableView object "table".
     */
    private void setTableListener() {
        table.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                File file = newValue.getFile();
                if (file.isDirectory()) {
                    currentSelection = newValue;
                    uncompressButton.setDisable(true);
                } else {
                    uncompressButton.setDisable(false);
                }
                compressButton.setDisable(false);
            } else {
                compressButton.setDisable(true);
                uncompressButton.setDisable(true);
            }
            backButtonListener();
        });

        table.setRowFactory(new Callback<>() {
            @Override
            public TableRow<RegularFileNode> call(TableView<RegularFileNode> param) {
                return new TableRow<>() {
                    @Override
                    protected void updateItem(RegularFileNode item, boolean empty) {
                        super.updateItem(item, empty);

                        setOnMouseClicked(click -> {
                            if (click.getClickCount() == 2) {
                                RegularFileNode rfn = table.getSelectionModel().getSelectedItem();
                                if (rfn != null) {
                                    if (rfn.getFile().isDirectory()) {
                                        fillTable();
                                    } else {
                                        try {
                                            openAction();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        });
                    }
                };
            }
        });
    }


    /**
     * Sets up the hover property listener of the TableColumn object "nameCol".
     */
    private void setNameColHoverFactory() {
        nameCol.setCellFactory(new Callback<>() {
            @Override
            public TableCell<RegularFileNode, String> call(TableColumn<RegularFileNode, String> param) {
                return new TableCell<>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty) {
                            setText(null);
                        } else {
                            setText(item);
                            hoverProperty().addListener((ObservableValue<? extends Boolean> obs, Boolean wasHovered,
                                                         Boolean isNowHovered) -> {
                                if (isNowHovered && !isEmpty()) {
                                    Tooltip tp = new Tooltip();
                                    tp.setText(getText());
                                    table.setTooltip(tp);
                                } else {
                                    table.setTooltip(null);
                                }
                            });
                        }
                    }
                };
            }
        });
    }


    /**
     * Sets up the change listener of the directory tree.
     */
    private void setTreeListener() {
        rootTree.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                if (newValue.getValue().exists())
                    currentSelection = new RegularFileNode(newValue.getValue(), lanLoader);
                else currentSelection = null;
                fillTable();
            } else {
                currentSelection = null;
            }
            backButtonListener();
        });
    }


    private void bwgImageViewer(File pictureFile) throws IOException {
        new Launcher().launch(pictureFile);
    }


    /**
     * Fills the file detail table when a directory is selected.
     */
    private void fillTable() {
        refreshButton.setDisable(false);
        table.getItems().clear();
        if (currentSelection != null) {
            currentDirLabel.setText(currentSelection.getFullPath());
            File node = currentSelection.getFile();
            try {
                for (File f : Objects.requireNonNull(node.listFiles())) {
                    RegularFileNode fdc;
                    if (f.isDirectory()) fdc = new RegularFileNode(f, lanLoader);
                    else fdc = new RegularFileNode(f, lanLoader);
                    table.getItems().add(fdc);
                }
            } catch (NullPointerException npe) {
                placeHolder.setText(lanLoader.get(63));
                return;
            }
        } else {
            currentDirLabel.setText("");
            for (File d : File.listRoots())
                table.getItems().add(new RegularFileNode(d, lanLoader));
        }
        if (table.getItems().size() == 0) placeHolder.setText(lanLoader.get(355));
        else placeHolder.setText("");
    }

    private void backButtonListener() {
        if (currentSelection != null && !currentSelection.isRoot()) backButton.setDisable(false);
        else backButton.setDisable(true);
    }

    private void fillText() {
        nameCol.setText(lanLoader.get(20));
        typeCol.setText(lanLoader.get(21));
        sizeCol.setText(lanLoader.get(22));
        timeCol.setText(lanLoader.get(23));
        compressButton.setText(lanLoader.get(10));
        uncompressButton.setText(lanLoader.get(11));
        settingsMenu.setText(lanLoader.get(12));
        helpMenu.setText(lanLoader.get(13));
        languageSetting.setText(lanLoader.get(14));
        about.setText(lanLoader.get(16));
        licence.setText(lanLoader.get(17));
        changelogView.setText(lanLoader.get(18));
        toolMenu.setText(lanLoader.get(30));
        bwgViewer.setText(lanLoader.get(31));
        openInDesktop.setText(lanLoader.get(32));
    }
}
