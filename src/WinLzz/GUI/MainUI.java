package WinLzz.GUI;

import WinLzz.GraphicUtil.*;
import WinLzz.ResourcesPack.ConfigLoader.GeneralLoaders;
import WinLzz.ResourcesPack.Languages.LanguageLoader;
import WinLzz.Utility.Util;
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
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;


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
    private MenuItem languageSetting, about, licence, changelogView, openInDesktop;

    private Label placeHolder = new Label();
    private ContextMenu rightPopupMenu;

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
            backButtonListener();
            try {
                expandTill(f);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }


    /* Actions and handlers */

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
    private void licenceAction() {
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
    private void changelogAction() throws IOException {
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
    private void languageSelection() {
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
        lsStage.setAlwaysOnTop(true);
        lsStage.showAndWait();
    }


    @FXML
    private void compressMode() throws Exception {
        ObservableList<RegularFileNode> selections = table.getSelectionModel().getSelectedItems();
        File[] selected = new File[selections.size()];
        for (int i = 0; i < selections.size(); i++) selected[i] = selections.get(i).getFile();
        if (selected.length > 0) {
            GeneralLoaders.writeLastSelectionDir(selected[0]);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("compressUI.fxml"));

            Parent root = loader.load();
            Stage stage = new Stage();

            stage.setTitle("WinLZZ");
            stage.setScene(new Scene(root));
            stage.setResizable(false);

            CompressUI cui = loader.getController();
            cui.setDir(selected);
            cui.setStage(stage);
            cui.setParent(this);
            cui.load(lanLoader);
            stage.show();
        }
    }

    @FXML
    private void backAction() {
        currentSelection = new RegularFileNode(currentSelection.getFile().getParentFile(), lanLoader);
        fillTable();
        backButtonListener();
    }

    @FXML
    private void openAction() throws Exception {
        RegularFileNode rfn = table.getSelectionModel().getSelectedItem();
        if (rfn.getFile().exists()) {
            switch (rfn.getExtension()) {
                case "pz":
                    uncompressMode(rfn.getFile());
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
        } else {
            Alert alert2 = new Alert(Alert.AlertType.WARNING);
            alert2.setTitle(lanLoader.get(60));
            alert2.setHeaderText(lanLoader.get(61));
            alert2.setContentText(lanLoader.get(64));
            alert2.showAndWait();
            refreshAction();
        }
    }

    /**
     * Refreshes the file table.
     */
    @FXML
    public void refreshAction() {
        String dir = currentDirLabel.getText();
        if (dir.length() > 0) currentSelection = new RegularFileNode(new File(dir), lanLoader);
        else currentSelection = null;
        fillTable();
    }

    @FXML
    private void desktopOpenAction() {
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
            GeneralLoaders.writeLastSelectionDir(selected);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("uncompressUI.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle(selected.getName());
            stage.setScene(new Scene(root));
            stage.setResizable(false);

            UncompressUI uui = loader.getController();
            uui.setPackFile(selected);
            uui.setStage(stage);
            uui.setParent(this);
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

    private void showFileProperty() throws IOException {
        ObservableList<RegularFileNode> files = table.getSelectionModel().getSelectedItems();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("filePropertiesUI.fxml"));

        Parent root = loader.load();
        Stage stage = new Stage();
        stage.setTitle(lanLoader.get(863));
        stage.setScene(new Scene(root));
        stage.setResizable(false);

        FilePropertiesUI pui = loader.getController();

        File[] fileArray = new File[files.size()];
        for (int i = 0; i < fileArray.length; i++)
            fileArray[i] = files.get(i).getFile();
        InfoNode node;
        if (fileArray.length == 1) node = new InfoNode(fileArray[0]);
        else node = new InfoNode(fileArray);
        pui.setFiles(node);
        pui.setLanLoader(lanLoader);
        pui.display();

        stage.show();
    }

    private void deleteAction() {
        Collection<RegularFileNode> selections = table.getSelectionModel().getSelectedItems();
        File[] files = new File[selections.size()];
        for (int i = 0; i < files.length; i++)
            files[i] = ((ObservableList<RegularFileNode>) selections).get(i).getFile();

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("WinLZZ");
        confirmation.setHeaderText(lanLoader.get(3));

        StringBuilder builder = new StringBuilder(lanLoader.get(73));
        builder.append(" ");
        for (File f : files) builder.append(f.getName()).append(lanLoader.get(76)).append(" ");
        builder.delete(builder.length() - 2, builder.length());
        builder.append(" ?");
        confirmation.setContentText(builder.toString());
        confirmation.showAndWait();

        if (confirmation.getResult() == ButtonType.OK) {
            ArrayList<File> failed = new ArrayList<>();
            for (File f : files) if (!Util.recursiveDelete(f)) failed.add(f);
            if (!failed.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("WinLZZ");
                alert.setHeaderText(lanLoader.get(74));
                StringBuilder sb = new StringBuilder(lanLoader.get(75));
                sb.append(" ");
                for (File f : failed) sb.append(f.getName()).append(lanLoader.get(76)).append(" ");
                sb.delete(sb.length() - 2, sb.length());
                alert.setContentText(sb.toString());

                alert.showAndWait();
            }
            refreshAction();
        }
    }

    /* Listeners */

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
                                if (item != null) {
                                    if (item.getFile().isDirectory()) {
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

                            setRightPopupMenu();

                            if (click.getButton() == MouseButton.SECONDARY && item != null)
                                rightPopupMenu.show(table, click.getScreenX(), click.getScreenY());
                            else rightPopupMenu.hide();
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
                                if (isNowHovered && !isEmpty() && getText().length() > 40) {
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

    private void backButtonListener() {
        if (currentSelection != null && !currentSelection.isRoot()) backButton.setDisable(false);
        else backButton.setDisable(true);
    }

    /* Setters / functions */

    /**
     * Fills the file detail table when a directory is selected.
     */
    private void fillTable() {
        refreshButton.setDisable(false);
        table.getItems().clear();
        ArrayList<RegularFileNode> nonDirectories = new ArrayList<>();
        if (currentSelection != null) {
            currentDirLabel.setText(currentSelection.getFullPath());
            File node = currentSelection.getFile();
            try {
                for (File f : Objects.requireNonNull(node.listFiles())) {
                    if (f.isDirectory()) table.getItems().add(new RegularFileNode(f, lanLoader));
                    else nonDirectories.add(new RegularFileNode(f, lanLoader));
                }
                table.getItems().addAll(nonDirectories);
                GeneralLoaders.writeLastDir(currentSelection.getFile());
            } catch (NullPointerException npe) {
                placeHolder.setText(lanLoader.get(63));
                return;
            }
        } else {
            currentDirLabel.setText("");
            for (File d : File.listRoots())
                table.getItems().add(new RegularFileNode(d, lanLoader));
            GeneralLoaders.writeLastDir(null);
        }
        if (table.getItems().size() == 0) placeHolder.setText(lanLoader.get(355));
        else placeHolder.setText("");
    }

    private void expandTill(File file) throws FileNotFoundException {
        String fullPath = file.getAbsolutePath();
        if (fullPath.contains(File.separator)) {
            String[] parts = fullPath.split(String.format("%s%s", "\\", File.separator));
            if (parts[0].endsWith(":")) parts[0] = parts[0] + File.separator;
            FileTreeItem current = (FileTreeItem) rootTree.getRoot();
            for (String part : parts) {
                current = searchMatched(current, part);
                current.setExpanded(true);
            }
        }
    }


    /**
     * Perform binary search to find the child that matches the name.
     *
     * @param parent     the parent TreeItem.
     * @param nameToFind name to be found.
     * @return the matched TreeItem.
     * @throws FileNotFoundException if the file does not exist, or some unexpected error occurs.
     */
    private static FileTreeItem searchMatched(FileTreeItem parent, String nameToFind) throws FileNotFoundException {
        String name = nameToFind.toLowerCase();
        List<TreeItem<File>> children = parent.getChildren();
        int begin = 0;
        int end = children.size();
        int mid;
        FileTreeItem fti;
        while (begin < end) {
            mid = (begin + end) / 2;
            fti = (FileTreeItem) children.get(mid);
            String fileName = fti.getValue().getName();
            if (fileName.length() == 0) fileName = fti.getValue().getAbsolutePath();
            int nameCompare = Util.stringCompare(name, fileName.toLowerCase());
            if (nameCompare < 0) end = mid;
            else if (nameCompare > 0) begin = mid;
            else return fti;
        }
        throw new FileNotFoundException("No such file exists: " + name);
    }


    /**
     * Sets up the content of the TreeView object "rootTree".
     */
    private void setTree() {
        FileTreeItem rootNode = new FileTreeItem(new File(System.getenv("COMPUTERNAME")));
        for (File file : File.listRoots()) {
            FileTreeItem rootItems = new FileTreeItem(new SpecialFile(file.getAbsolutePath()));
            rootNode.getChildren().add(rootItems);
        }
        rootTree.setRoot(rootNode);
    }

    private void setRightPopupMenu() {
        if (rightPopupMenu == null) {
            rightPopupMenu = new ContextMenu();
            MenuItem open = new MenuItem(lanLoader.get(11));
            open.setOnAction(e -> {
                try {
                    openAction();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            });
            MenuItem openDir = new MenuItem(lanLoader.get(72));
            openDir.setOnAction(e -> desktopOpenAction());
            MenuItem compress = new MenuItem(lanLoader.get(10));
            compress.setOnAction(e -> {
                try {
                    compressMode();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            });
            MenuItem delete = new MenuItem(lanLoader.get(71));
            delete.setOnAction(e -> deleteAction());
            MenuItem property = new MenuItem(lanLoader.get(70));
            property.setOnAction(e -> {
                try {
                    showFileProperty();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            });

            rightPopupMenu.getItems().addAll(open, openDir, compress, delete, property);
        }
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
        openInDesktop.setText(lanLoader.get(32));
    }
}
