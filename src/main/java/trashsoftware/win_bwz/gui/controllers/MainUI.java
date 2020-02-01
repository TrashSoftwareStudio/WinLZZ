package trashsoftware.win_bwz.gui.controllers;

import trashsoftware.win_bwz.gui.graphicUtil.*;
import trashsoftware.win_bwz.resourcesPack.configLoader.GeneralLoaders;
import trashsoftware.win_bwz.resourcesPack.languages.LanguageLoader;
import trashsoftware.win_bwz.utility.Util;
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
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

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
    private HBox currentDirBox;

    @FXML
    private MenuItem languageSetting, about, licence, changelogView, openInDesktop, pasteHere;

    private String currentDir;

    private boolean isClickingDirBox;

    private ResourceBundle bundle;

    /**
     * {@code MenuItem}'s in right-click popup menu.
     */
    private MenuItem openR, openDirR, compressR, copyR, cutR, pasteR, deleteR, renameR, propertyR;

    private Label placeHolder = new Label();
    private ContextMenu rightPopupMenu = new ContextMenu();

    private LanguageLoader lanLoader = new LanguageLoader();
    private RegularFileNode currentSelection;

    private char[] nameExclusion = new char[]{'\\', '/', ':', '*', '?', '"', '<', '>', '|'};
    private FileMover[] clipBoard;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.bundle = resources;
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
        setRightPopupMenu();
        changeClipBoardStatus();
    }

    /* Actions and handlers */

    @FXML
    public void aboutAction() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/trashsoftware/win_bwz/fxml/aboutUI.fxml"), bundle);
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
        dialog.setTitle(bundle.getString("license"));
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
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/trashsoftware/win_bwz/fxml/changelogViewer.fxml"), bundle);
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

        Button confirm = new Button(bundle.getString("confirm"));  // Confirm
        confirm.setOnAction(e -> {
            if (!lanLoader.changeLanguage(languageBox.getSelectionModel().getSelectedItem()))
                System.out.println("Failed to change language");
            fillText();
            lsStage.close();
            fillTable();
            setRightPopupMenu();
            changeRightMenu();
            changeClipBoardStatus();
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
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/trashsoftware/win_bwz/fxml/compressUI.fxml"), bundle);

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
                        alert.setTitle(bundle.getString("error"));
                        alert.setHeaderText(bundle.getString("cannotOpenThisFile"));
                        alert.setContentText(bundle.getString("occupiedByOtherApp"));
                        alert.showAndWait();
                    }
                    break;
            }
        } else {
            Alert alert2 = new Alert(Alert.AlertType.WARNING);
            alert2.setTitle(bundle.getString("error"));
            alert2.setHeaderText(bundle.getString("cannotOpenThisFile"));
            alert2.setContentText(bundle.getString("fileDoesNotExist"));
            alert2.showAndWait();
            refreshAction();
        }
    }

    /**
     * Refreshes the file table.
     */
    @FXML
    public void refreshAction() {
        String dir = currentDir;
        if (dir.length() > 0) currentSelection = new RegularFileNode(new File(dir), lanLoader);
        else currentSelection = null;
        fillTable();
    }

    @FXML
    private void desktopOpenAction() {
        try {
            String dir = currentDir;
            if (dir.length() > 0) {
                Desktop.getDesktop().open(new File(dir));
            } else {
                Runtime.getRuntime().exec("cmd /c start explorer");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void refreshDirButtons() {
        currentDirBox.getChildren().clear();
        if (currentDir.length() > 0) {
            String split = Pattern.quote(System.getProperty("file.separator"));
            String[] dirs = currentDir.split(split);
            StringBuilder cumulativeDir = new StringBuilder();
            for (String pattern : dirs) {
                cumulativeDir.append(pattern).append(File.separator);
                DirButton db = new DirButton(cumulativeDir.toString(), pattern);
                db.setOnAction(e -> {
                    currentDir = db.getFullPath();
                    isClickingDirBox = true;
                    fillTable();
                });
                currentDirBox.getChildren().add(db);
            }
        }
    }

    private void uncompressMode(File selected) throws Exception {
        if (selected != null) {
            GeneralLoaders.writeLastSelectionDir(selected);

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/trashsoftware/win_bwz/fxml/uncompressUI.fxml"), bundle);
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
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/trashsoftware/win_bwz/fxml/filePropertiesUI.fxml"), bundle);

        Parent root = loader.load();
        Stage stage = new Stage();
        stage.setTitle(lanLoader.get(863));
        stage.setScene(new Scene(root));
        stage.setResizable(false);

        FilePropertiesUI pui = loader.getController();

        ObservableList<RegularFileNode> files = table.getSelectionModel().getSelectedItems();
        File[] fileArray = new File[files.size()];
        for (int i = 0; i < fileArray.length; i++)
            fileArray[i] = files.get(i).getFile();
        InfoNode node;
        if (fileArray.length == 1) {
            node = new InfoNode(fileArray[0]);
        } else if (fileArray.length == 0) node = new InfoNode(new File(currentDir));
        else node = new InfoNode(fileArray);
        pui.setFiles(node);
        pui.setLanLoader(lanLoader);
        pui.display();
        stage.setOnCloseRequest(e -> pui.interrupt());

        stage.show();
    }

    private void deleteAction() {
        ObservableList<RegularFileNode> selections = table.getSelectionModel().getSelectedItems();
        File[] files = new File[selections.size()];
        for (int i = 0; i < files.length; i++)
            files[i] = selections.get(i).getFile();

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

    private void renameAction() {
        File f = table.getSelectionModel().getSelectedItem().getFile();
        VBox vbox = new VBox();
        Stage st = new Stage();
        st.setTitle(f.getName());
        Scene sc = new Scene(vbox);
        st.initStyle(StageStyle.UTILITY);
        st.setScene(sc);

        TextField nameField = new TextField(f.getName());
        Label prompt = new Label("\n\n");
        HBox hbox = new HBox();
        Button confirm = new Button(bundle.getString("confirm"));
        confirm.setOnAction(e -> {
            String newName = nameField.getText();
            if (Util.charArrayContains(nameExclusion, newName)) {
                prompt.setText(String.format("%s\n%s", lanLoader.get(90), new String(nameExclusion)));
            } else {
                if (!newName.equals(f.getName())) {
                    File newFile = new File(String.format("%s%s%s", f.getParent(), File.separator, newName));
                    if (newFile.exists()) {
                        prompt.setText(lanLoader.get(89) + "\n\n");
                        return;
                    } else {
                        if (!f.renameTo(newFile)) {
                            prompt.setText(lanLoader.get(91) + "\n\n");
                            return;
                        }
                    }
                }
                refreshAction();
                st.close();
            }
        });

        Button cancel = new Button(lanLoader.get(2));
        cancel.setOnAction(e -> st.close());

        hbox.getChildren().addAll(confirm, cancel);
        hbox.setSpacing(5.0);
        hbox.setAlignment(Pos.CENTER_RIGHT);
        vbox.getChildren().addAll(nameField, prompt, hbox);
        vbox.setSpacing(5.0);
        vbox.setPadding(new Insets(10.0));
        vbox.setPrefSize(280.0, 120.0);
        vbox.setAlignment(Pos.CENTER_LEFT);

        st.setAlwaysOnTop(true);
        st.showAndWait();
    }

    private void copyAction() {
        List<RegularFileNode> selections = table.getSelectionModel().getSelectedItems();
        clipBoard = new FileMover[selections.size()];
        for (int i = 0; i < clipBoard.length; i++) clipBoard[i] = new FileMover(selections.get(i).getFile(), true);
        changeClipBoardStatus();
    }

    private void cutAction() {
        List<RegularFileNode> selections = table.getSelectionModel().getSelectedItems();
        clipBoard = new FileMover[selections.size()];
        for (int i = 0; i < clipBoard.length; i++) clipBoard[i] = new FileMover(selections.get(i).getFile(), false);
        changeClipBoardStatus();
    }

    /**
     * Pastes the files on {@code clipBoard} to the current opening directory.
     */
    @FXML
    private void pasteHereAction() {
        File destDir = new File(currentDir);
        paste(destDir);
    }

    /**
     * Pastes the files on {@code clipBoard} to the selected directory, if there is exactly one selection and it
     * is a directory.
     */
    private void pasteAction() {
        List<RegularFileNode> selections = table.getSelectionModel().getSelectedItems();
        if (selections.size() == 1 && selections.get(0).getFile().isDirectory()) paste(selections.get(0).getFile());
        else pasteHereAction();
    }

    private void paste(File destDir) {
        for (FileMover fm : clipBoard) {
            File destFile = fm.getDestFile(destDir);
            if (destFile.exists()) {
                int result = replaceFileBox(destFile, fm.getFile());
                if (result == 0) {
                    Util.deleteFile(destFile);
                    if (!fm.pasteTo(destFile)) pasteFailed(fm);
                } else if (result == 2) {
                    if (!fm.pasteTo(new File(Util.getCopyName(destFile.getAbsolutePath())))) pasteFailed(fm);
                }
            } else {
                if (!fm.pasteTo(destFile)) pasteFailed(fm);
            }
            refreshAction();
        }
        if (!clipBoard[0].isCopy()) clipBoard = null;  // To prevent the second time "cut and paste"
        changeClipBoardStatus();
    }

    private int replaceFileBox(File existFile, File foreignFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/trashsoftware/win_bwz/fxml/replaceUI.fxml"));

            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle(lanLoader.get(3));
            stage.setScene(new Scene(root));
            stage.setResizable(false);

            ReplaceUI rui = loader.getController();
            rui.setStage(stage);
            rui.setLanLoader(lanLoader);
            rui.setFiles(existFile, foreignFile);

            stage.showAndWait();

            return rui.getResult();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Internal error");
        }
    }

    private void pasteFailed(FileMover fm) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(lanLoader.get(60));
        alert.setHeaderText(lanLoader.get(86));
        alert.setContentText(String.format("%s %s", lanLoader.get(87), fm.getFile().getAbsolutePath()));
        alert.showAndWait();
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

        table.setRowFactory(new Callback<TableView<RegularFileNode>, TableRow<RegularFileNode>>() {
            @Override
            public TableRow<RegularFileNode> call(TableView<RegularFileNode> param) {
                return new TableRow<RegularFileNode>() {
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

                            if (click.getButton() == MouseButton.SECONDARY) {
                                if (item == null) table.getSelectionModel().clearSelection();
                                changeRightMenu();
                                rightPopupMenu.show(table, click.getScreenX(), click.getScreenY());
                            } else rightPopupMenu.hide();
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
        nameCol.setCellFactory((TableColumn<RegularFileNode, String> tc) -> new TableCell<RegularFileNode, String>() {
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
        if (isClickingDirBox) {
            isClickingDirBox = false;
            File f = new File(currentDir);
            fillFrom(f);
        } else if (currentSelection != null) {
            currentDir = currentSelection.getFullPath();
            File node = currentSelection.getFile();
            try {
                fillFrom(node);
            } catch (NullPointerException npe) {
                placeHolder.setText(lanLoader.get(63));
            }
        } else {
            currentDir = "";
            for (File d : File.listRoots())
                table.getItems().add(new RegularFileNode(d, lanLoader));
            GeneralLoaders.writeLastDir(null);
        }
        refreshDirButtons();
        if (table.getItems().size() == 0) placeHolder.setText(lanLoader.get(355));
        else placeHolder.setText("");
    }

    private void fillFrom(File node) {
        ArrayList<RegularFileNode> nonDirectories = new ArrayList<>();
        for (File f : Objects.requireNonNull(node.listFiles())) {
            if (f.isDirectory()) table.getItems().add(new RegularFileNode(f, lanLoader));
            else nonDirectories.add(new RegularFileNode(f, lanLoader));
        }
        table.getItems().addAll(nonDirectories);
        GeneralLoaders.writeLastDir(currentSelection.getFile());

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
        openR = new MenuItem(lanLoader.get(11));
        openR.setOnAction(e -> {
            try {
                openAction();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });

        openDirR = new MenuItem(lanLoader.get(72));
        openDirR.setOnAction(e -> desktopOpenAction());

        compressR = new MenuItem(lanLoader.get(10));
        compressR.setOnAction(e -> {
            try {
                compressMode();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });

        copyR = new MenuItem(lanLoader.get(77));
        copyR.setOnAction(e -> copyAction());

        cutR = new MenuItem(lanLoader.get(78));
        cutR.setOnAction(e -> cutAction());

        pasteR = new MenuItem(lanLoader.get(79));
        pasteR.setOnAction(e -> pasteAction());

        deleteR = new MenuItem(lanLoader.get(71));
        deleteR.setOnAction(e -> deleteAction());

        renameR = new MenuItem(lanLoader.get(88));
        renameR.setOnAction(e -> renameAction());

        propertyR = new MenuItem(lanLoader.get(70));
        propertyR.setOnAction(e -> {
            try {
                showFileProperty();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        });
    }

    private void changeRightMenu() {
        rightPopupMenu.getItems().clear();
        int selectionNumber = table.getSelectionModel().getSelectedItems().size();
        if (currentDir.length() == 0) {
            // The current opening directory is the system root
            if (selectionNumber != 0) rightPopupMenu.getItems().addAll(openR, openDirR, new SeparatorMenuItem(),
                    propertyR);
        } else {
            // The current opening directory is a regular directory
            if (selectionNumber == 0)
                rightPopupMenu.getItems().addAll(pasteR, propertyR);
            else if (selectionNumber == 1)
                rightPopupMenu.getItems().addAll(openR, openDirR, new SeparatorMenuItem(), compressR,
                        new SeparatorMenuItem(), copyR, cutR, pasteR, new SeparatorMenuItem(), deleteR, renameR,
                        new SeparatorMenuItem(), propertyR);
            else
                rightPopupMenu.getItems().addAll(openR, openDirR, new SeparatorMenuItem(), compressR,
                        new SeparatorMenuItem(), copyR, cutR, pasteR, new SeparatorMenuItem(), deleteR,
                        new SeparatorMenuItem(), propertyR);
        }
    }

    private void changeClipBoardStatus() {
        if (clipBoard == null) {
            pasteR.setDisable(true);
            pasteHere.setDisable(true);
        } else {
            pasteR.setDisable(false);
            pasteHere.setDisable(false);
        }
    }

    private void fillText() {
//        nameCol.setText(lanLoader.get(20));
//        typeCol.setText(lanLoader.get(21));
//        sizeCol.setText(lanLoader.get(22));
//        timeCol.setText(lanLoader.get(23));
//        compressButton.setText(lanLoader.get(10));
//        uncompressButton.setText(lanLoader.get(11));
//        settingsMenu.setText(lanLoader.get(12));
//        helpMenu.setText(lanLoader.get(13));
//        languageSetting.setText(lanLoader.get(14));
//        about.setText(lanLoader.get(16));
//        licence.setText(lanLoader.get(17));
//        changelogView.setText(lanLoader.get(18));
//        toolMenu.setText(lanLoader.get(30));
//        openInDesktop.setText(lanLoader.get(32));
//        pasteHere.setText(lanLoader.get(31));
    }
}