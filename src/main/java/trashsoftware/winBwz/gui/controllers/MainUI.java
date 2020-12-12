package trashsoftware.winBwz.gui.controllers;

import javafx.scene.layout.RowConstraints;
import trashsoftware.trashGraphics.core.ImageViewer;
import trashsoftware.trashGraphics.gui.TrashGraphicsClient;
import trashsoftware.winBwz.gui.GUIClient;
import trashsoftware.winBwz.gui.graphicUtil.*;
import trashsoftware.winBwz.resourcesPack.configLoader.GeneralLoaders;
import trashsoftware.winBwz.utility.Util;
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
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

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

//    @FXML
//    private TableView<RegularFileNode> table;
//
//    @FXML
//    private TableColumn<RegularFileNode, String> nameCol, typeCol, timeCol;
//
//    @FXML
//    private TableColumn<RegularFileNode, ReadableSize> sizeCol;

    @FXML
    private Button backButton, refreshButton;

    /**
     * Toolbar buttons
     */
    @FXML
    private Button compressButton, uncompressButton, iconListButton;

    @FXML
    private Button showHideToolbarBtn;

    @FXML
    private HBox currentDirBox;

    @FXML
    private MenuItem pasteHere;

    @FXML
    private RowConstraints toolbarRow;

    @FXML
    private HBox toolbar;

    @FXML
    private TabPane rootTabPane;

//    @FXML
//    private TableFileView tableFileView;

//    private FileManagerPage getActiveFileViewPage(;

//    private String currentDir;

    private boolean isClickingDirBox;

    private ResourceBundle bundle;

    /**
     * {@code MenuItem}'s in right-click popup menu.
     */
    private MenuItem openR, openDirR, compressR, copyR, cutR, pasteR, deleteR, renameR, propertyR;
    private Stage thisStage;
    private GUIClient guiClient;

    //    private Label placeHolder = new Label();
    private final ContextMenu rightPopupMenu = new ContextMenu();

    private RegularFileNode currentSelection;

    private static final char[] nameExclusion = new char[]{'\\', '/', ':', '*', '?', '"', '<', '>', '|'};
    private FileMover[] clipBoard;

    private boolean toolbarShown = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.bundle = resources;

//        activeFileViewPage = tableFileView;
//        activeFileViewPage.setParent(this);

        setTree();
        setTreeListener();
        setTabPaneListener();
        rootTree.getRoot().setExpanded(true);
//        setNameColHoverFactory();
//        setSizeColHoverFactory();

//        nameCol.setCellValueFactory(new PropertyValueFactory<>("Name"));
//        typeCol.setCellValueFactory(new PropertyValueFactory<>("Type"));
//        sizeCol.setCellValueFactory(new PropertyValueFactory<>("Size"));
//        timeCol.setCellValueFactory(new PropertyValueFactory<>("LastModified"));
//        setTableListener();
//        table.setPlaceholder(placeHolder);

        List<String> lastOpenedDirs = GeneralLoaders.getOpeningDirs();
        for (String dir : lastOpenedDirs) {
            createTabByPath(dir, false);
        }
        if (!lastOpenedDirs.isEmpty()) {
            try {
                expandTill(lastOpenedDirs.get(0));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

//        File f = GeneralLoaders.readLastDir();
//        if (f != null) {
//            currentSelection = new RegularFileNode(f, bundle);
//            activeFileViewPage.setDir(currentSelection.getFullPath());
//            fillTable();
//            changeBackBtnStatus();
//            try {
//                expandTill(f);
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            }
//        }

//        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        setRightPopupMenu();
        changeClipBoardStatus();
    }

    public void setStageAndParent(Stage stage, GUIClient guiClient) {
        this.thisStage = stage;
        this.guiClient = guiClient;
    }

    /* Actions and handlers */

    @FXML
    void aboutAction() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/trashsoftware/winBwz/fxml/aboutUI.fxml"), bundle);
        Parent root = loader.load();
        Stage stage = new Stage();
        stage.setTitle("WinLZZ");
        stage.setScene(new Scene(root));
        stage.setResizable(false);
        stage.show();
    }

    @FXML
    void licenceAction() {
        Pane root = new Pane();
        Stage dialog = new Stage();
        Scene scene = new Scene(root);
        dialog.setTitle(bundle.getString("license"));
        dialog.setScene(scene);

        VBox pane = new VBox();
        pane.setFillWidth(true);
        pane.setPrefSize(540.0, 320.0);
        pane.setAlignment(Pos.CENTER);

        Label label = new Label(LICENCE);
        label.setTextAlignment(TextAlignment.CENTER);
        pane.getChildren().add(label);
        root.getChildren().add(pane);
        dialog.setResizable(false);

        dialog.show();
    }

    @FXML
    void changelogAction() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/trashsoftware/winBwz/fxml/changelogViewer.fxml"), bundle);
        Parent root = loader.load();
        Stage stage = new Stage();
        stage.setTitle("WinLZZ");
        stage.setScene(new Scene(root));
        stage.setResizable(false);
        stage.show();
    }

    @FXML
    void openTrashGraphics() throws IOException {
        showTrashGraphics();
    }

    @FXML
    void settingsAction() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/trashsoftware/winBwz/fxml/settingsMain.fxml"), bundle);
        Parent root = loader.load();

        Stage stage = new Stage();
        stage.setTitle(bundle.getString("settings"));
        stage.setScene(new Scene(root));
        stage.setResizable(false);

        SettingsMain controller = loader.getController();
        controller.setStageAndParent(stage, this);

        stage.show();
    }

    @FXML
    public void compressMode() throws Exception {
//        ObservableList<RegularFileNode> selections = table.getSelectionModel().getSelectedItems();
        List<RegularFileNode> selections = getActiveFileViewPage().getSelections();
        File[] selected = new File[selections.size()];
        for (int i = 0; i < selections.size(); i++) selected[i] = selections.get(i).getFile();
        if (selected.length > 0) {
            GeneralLoaders.writeLastSelectionDir(selected[0]);
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/trashsoftware/winBwz/fxml/compressUI.fxml"), bundle);

            Parent root = loader.load();
            Stage stage = new Stage();

            stage.setTitle("WinLZZ");
            stage.setScene(new Scene(root));
            stage.setResizable(false);

            CompressUI cui = loader.getController();
            cui.setDir(selected);
            cui.setStage(stage);
            cui.setParent(this);
            cui.load();
            stage.show();
        }
    }

    @FXML
    void backAction() {
        currentSelection = new RegularFileNode(currentSelection.getFile().getParentFile(), bundle);
        fillTable();
        changeBackBtnStatus();
    }

    @FXML
    private void iconListAction() {
        FileManagerPage active = getActiveFileViewPage();
        if (active != null) {
            active.switchViewMethod();
            iconListButton.setText(active.getCurrentViewMethod() == FileManagerPage.TABLE_VIEW ?
                    bundle.getString("showInIcon") : bundle.getString("showInTable"));
        }
    }

    @FXML
    public void openAction() throws Exception {
//        RegularFileNode rfn = table.getSelectionModel().getSelectedItem();
        RegularFileNode rfn = getActiveFileViewPage().getSelection();
        if (rfn.getFile().exists()) {
            String ext = rfn.getExtension();
            if ("pz".equals(ext)) {
                uncompressMode(rfn.getFile());
            } else if (Util.arrayContains(ImageViewer.ALL_FORMATS_READ, ext, false)) {
                showTrashGraphicsWithImage(rfn.getFile());
            } else {
                try {
                    Desktop.getDesktop().open(rfn.getFile());
                } catch (IOException ioe) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle(bundle.getString("error"));
                    alert.setHeaderText(bundle.getString("cannotOpenThisFile"));
                    alert.setContentText(bundle.getString("occupiedByOtherApp"));
                    alert.showAndWait();
                }
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
        String dir = getActiveFileViewPage().getCurrentDir();
        if (dir.length() > 0) currentSelection = new RegularFileNode(new File(dir), bundle);
        else currentSelection = null;
        fillTable();
    }

    @FXML
    public void desktopOpenAction() {
        try {
            String dir = getActiveFileViewPage().getCurrentDir();
            if (dir.length() > 0) {
                Desktop.getDesktop().open(new File(dir));
            } else {
                Runtime.getRuntime().exec("cmd /c start explorer");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void exitAction() {
        thisStage.close();
    }

    @FXML
    void restartAction() {
        guiClient.restart(thisStage);
    }

    /* Functional methods */

    private void showTrashGraphics() throws IOException {
        new TrashGraphicsClient().runClient(bundle, this);
    }

    private void showTrashGraphicsWithImage(File file) throws IOException {
        new TrashGraphicsClient().runClientWithImage(bundle, this, file);
    }

    private void refreshDirButtons() {
        currentDirBox.getChildren().clear();
        String currentDir = getActiveFileViewPage().getCurrentDir();
        if (currentDir.length() > 0) {
            String split = Pattern.quote(System.getProperty("file.separator"));
            String[] dirs = currentDir.split(split);
            StringBuilder cumulativeDir = new StringBuilder();
            for (String pattern : dirs) {
                cumulativeDir.append(pattern).append(File.separator);
                DirButton db = new DirButton(cumulativeDir.toString(), pattern);
                db.setOnAction(e -> {
                    getActiveFileViewPage().setDir(db.getFullPath());
                    isClickingDirBox = true;
                    fillTable();
                    changeDirRelatives();
                });
                currentDirBox.getChildren().add(db);
            }
        }
    }

    private void uncompressMode(File selected) throws Exception {
        if (selected != null) {
            GeneralLoaders.writeLastSelectionDir(selected);

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/trashsoftware/winBwz/fxml/uncompressUI.fxml"), bundle);
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle(selected.getName());
            stage.setScene(new Scene(root));
            stage.setResizable(false);

            UncompressUI uui = loader.getController();
            uui.setPackFile(selected);
            uui.setStage(stage);
            uui.setParent(this);
//            uui.setLanLoader(lanLoader);

            stage.setOnCloseRequest(event -> uui.close());
            stage.show();
            try {
                uui.loadContext();
            } catch (Exception e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle(bundle.getString("cannotOpenFile"));
                alert.setHeaderText(bundle.getString("cannotReadArchive"));
                alert.setContentText(bundle.getString("fileDamaged"));
                alert.showAndWait();
                stage.close();
            }
        }
    }

    public void showFileProperty() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/trashsoftware/winBwz/fxml/filePropertiesUI.fxml"), bundle);

        Parent root = loader.load();
        Stage stage = new Stage();
        stage.setTitle(bundle.getString("properties"));
        stage.setScene(new Scene(root));
        stage.setResizable(false);

        FilePropertiesUI pui = loader.getController();

//        ObservableList<RegularFileNode> files = table.getSelectionModel().getSelectedItems();
        List<RegularFileNode> files = getActiveFileViewPage().getSelections();
        File[] fileArray = new File[files.size()];
        for (int i = 0; i < fileArray.length; i++)
            fileArray[i] = files.get(i).getFile();
        InfoNode node;
        if (fileArray.length == 1) {
            node = new InfoNode(fileArray[0]);
        } else if (fileArray.length == 0) node = new InfoNode(new File(getActiveFileViewPage().getCurrentDir()));
        else node = new InfoNode(fileArray);
        pui.setFiles(node);
        pui.display();
        stage.setOnCloseRequest(e -> pui.interrupt());

        stage.show();
    }

    public void deleteAction() {
//        ObservableList<RegularFileNode> selections = table.getSelectionModel().getSelectedItems();
        List<RegularFileNode> selections = getActiveFileViewPage().getSelections();
        File[] files = new File[selections.size()];
        for (int i = 0; i < files.length; i++)
            files[i] = selections.get(i).getFile();

        StringBuilder builder = new StringBuilder(bundle.getString("confirmDelete"));
        builder.append(" ");
        for (File f : files) builder.append(f.getName()).append(bundle.getString("backDot")).append(" ");
        builder.delete(builder.length() - 2, builder.length());
        builder.append(" ?");

        boolean astConfirmDelete = InfoBoxes.showConfirmBox(
                "WinLZZ",
                bundle.getString("pleaseConfirm"),
                builder.toString()
        );

        if (astConfirmDelete) {
            ArrayList<File> failed = new ArrayList<>();
            for (File f : files) if (!Util.recursiveDelete(f)) failed.add(f);
            if (!failed.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("WinLZZ");
                alert.setHeaderText(bundle.getString("deleteFailed"));
                StringBuilder sb = new StringBuilder(bundle.getString("cannotDelete"));
                sb.append(" ");
                for (File f : failed) sb.append(f.getName()).append(bundle.getString("backDot")).append(" ");
                sb.delete(sb.length() - 2, sb.length());
                alert.setContentText(sb.toString());

                alert.showAndWait();
            }
            refreshAction();
        }
    }

    public void renameAction() {
//        File f = table.getSelectionModel().getSelectedItem().getFile();
        File f = getActiveFileViewPage().getSelection().getFile();
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
                prompt.setText(String.format("%s\n%s",
                        bundle.getString("fileNameNotAllowContain"),
                        new String(nameExclusion)));
            } else {
                if (!newName.equals(f.getName())) {
                    File newFile = new File(String.format("%s%s%s", f.getParent(), File.separator, newName));
                    if (newFile.exists()) {
                        prompt.setText(bundle.getString("fileAlreadyExist") + "\n\n");
                        return;
                    } else {
                        if (!f.renameTo(newFile)) {
                            prompt.setText(bundle.getString("cannotRenameFile") + "\n\n");
                            return;
                        }
                    }
                }
                refreshAction();
                st.close();
            }
        });

        Button cancel = new Button(bundle.getString("cancel"));
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

    public void copyAction() {
//        List<RegularFileNode> selections = table.getSelectionModel().getSelectedItems();
        List<RegularFileNode> selections = getActiveFileViewPage().getSelections();
        clipBoard = new FileMover[selections.size()];
        for (int i = 0; i < clipBoard.length; i++)
            clipBoard[i] = new FileMover(selections.get(i).getFile(), true);
        changeClipBoardStatus();
    }

    public void cutAction() {
//        List<RegularFileNode> selections = table.getSelectionModel().getSelectedItems();
        List<RegularFileNode> selections = getActiveFileViewPage().getSelections();
        clipBoard = new FileMover[selections.size()];
        for (int i = 0; i < clipBoard.length; i++) clipBoard[i] = new FileMover(selections.get(i).getFile(), false);
        changeClipBoardStatus();
    }

    /**
     * Pastes the files on {@code clipBoard} to the current opening directory.
     */
    @FXML
    private void pasteHereAction() {
        File destDir = new File(getActiveFileViewPage().getCurrentDir());
        paste(destDir);
    }

    @FXML
    private void showHideToolbarAction() {
        if (toolbarShown) {
            showHideToolbarBtn.setText("⮟");
            toolbarRow.setPrefHeight(0.0);
            toolbar.setVisible(false);
        } else {
            showHideToolbarBtn.setText("⮝");
            toolbarRow.setPrefHeight(50.0);
            toolbar.setVisible(true);
        }
        toolbarShown = !toolbarShown;
    }

    public void selectFile(RegularFileNode newValue) {
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
        changeBackBtnStatus();
    }

    /**
     * Pastes the files on {@code clipBoard} to the selected directory, if there is exactly one selection and it
     * is a directory.
     */
    public void pasteAction() {
//        List<RegularFileNode> selections = table.getSelectionModel().getSelectedItems();
        List<RegularFileNode> selections = getActiveFileViewPage().getSelections();
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
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/trashsoftware/winBwz/fxml/replaceUI.fxml"),
                            bundle);

            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle(bundle.getString("pleaseConfirm"));
            stage.setScene(new Scene(root));
            stage.setResizable(false);

            ReplaceUI rui = loader.getController();
            rui.setStage(stage);
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
        alert.setTitle(bundle.getString("error"));
        alert.setHeaderText(bundle.getString("pasteFailed"));
        alert.setContentText(String.format("%s %s", bundle.getString("cannotMove"), fm.getFile().getAbsolutePath()));
        alert.showAndWait();
    }

    /* Listeners */

    /**
     * Sets up the change listener of the directory tree.
     */
    private void setTreeListener() {
        rootTree.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                if (newValue.getValue().exists())
                    currentSelection = new RegularFileNode(newValue.getValue(), bundle);
                else currentSelection = null;
                showOneFilePage();
//                fillTable();
            } else {
                currentSelection = null;
            }
            changeBackBtnStatus();
        });
    }

    private void setTabPaneListener() {
        rootTabPane.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
            if (newValue != null) {
                refreshDirButtons();
            }
        }));
    }

    private void changeBackBtnStatus() {
        backButton.setDisable(currentSelection == null || currentSelection.isRoot());
    }

    private Tab createTabByPath(String path, boolean saveToCache) {
        FileManagerPage page = new FileManagerPage(this, path);

        Tab openedTab = new Tab(page.getName());
        openedTab.setContent(page);
        rootTabPane.getTabs().add(openedTab);

        openedTab.setOnCloseRequest(e -> {
            List<String> opening = getAllOpeningDirs();
            FileManagerPage operating = ((FileManagerPage) ((Tab) e.getSource()).getContent());
            opening.removeIf(d -> d.equals(operating.getCurrentDir()));
            GeneralLoaders.saveOpeningDirs(opening);
        });

        if (saveToCache)
            GeneralLoaders.saveOpeningDirs(getAllOpeningDirs());  // refreshes the cache

        return openedTab;
    }

    /* Setters / functions */

    public void showOneFilePage() {
        Tab openedTab = null;
        String path = currentSelection == null ? "" : currentSelection.getFullPath();
        for (Tab tab : rootTabPane.getTabs()) {
            FileManagerPage fmp = (FileManagerPage) tab.getContent();
            if (fmp.getCurrentDir().equals(path)) openedTab = tab;
        }
        if (openedTab == null) {
            // create a new tab
            openedTab = createTabByPath(path, true);
        }
        rootTabPane.getSelectionModel().select(openedTab);
    }

    /**
     * This method is called from file viewers, to go into a directory.
     */
    public void gotoDirectory() {
        fillTable();
        changeDirRelatives();
    }

    /**
     * Fills the file detail table when a directory is selected.
     */
    public void fillTable() {
        refreshButton.setDisable(false);
        if (isClickingDirBox) {
            isClickingDirBox = false;
            File tarFile = new File(getActiveFileViewPage().getCurrentDir());
            fillFromDir(tarFile);
        } else if (currentSelection != null) {
            File node = currentSelection.getFile();
            try {
                fillFromDir(node);
            } catch (NullPointerException npe) {
                npe.printStackTrace();
//                placeHolder.setText(bundle.getString("cannotAccess"));
            }
        } else {
            getActiveFileViewPage().refresh();
        }
        refreshDirButtons();
    }

    private void fillFromDir(File node) {
        getActiveFileViewPage().setDir(node.getAbsolutePath());
        getActiveFileViewPage().refresh();
//        ArrayList<RegularFileNode> nonDirectories = new ArrayList<>();
//        for (File f : Objects.requireNonNull(node.listFiles())) {
//            if (f.isDirectory()) table.getItems().add(new RegularFileNode(f, bundle));
//            else nonDirectories.add(new RegularFileNode(f, bundle));
//        }
//        table.getItems().addAll(nonDirectories);
    }

    private List<String> getAllOpeningDirs() {
        List<String> lst = new ArrayList<>();
        for (Tab tab : rootTabPane.getTabs()) {
            lst.add(((FileManagerPage) tab.getContent()).getCurrentDir());
        }
        return lst;
    }

    private void changeDirRelatives() {
        // Renames the active tab
        Tab selected = rootTabPane.getSelectionModel().getSelectedItem();
        FileManagerPage selectedFmp = (FileManagerPage) selected.getContent();
        selected.setText(selectedFmp.getName());

        GeneralLoaders.saveOpeningDirs(getAllOpeningDirs());  // refreshes the cache
    }

    private void expandTill(String fullPath) throws FileNotFoundException {
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

    public void setRightPopupMenu() {
        openR = new MenuItem(bundle.getString("open"));
        openR.setOnAction(e -> {
            try {
                openAction();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });

        openDirR = new MenuItem(bundle.getString("openLocation"));
        openDirR.setOnAction(e -> desktopOpenAction());

        compressR = new MenuItem(bundle.getString("compress"));
        compressR.setOnAction(e -> {
            try {
                compressMode();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });

        copyR = new MenuItem(bundle.getString("copy"));
        copyR.setOnAction(e -> copyAction());

        cutR = new MenuItem(bundle.getString("cut"));
        cutR.setOnAction(e -> cutAction());

        pasteR = new MenuItem(bundle.getString("paste"));
        pasteR.setOnAction(e -> pasteAction());

        deleteR = new MenuItem(bundle.getString("delete"));
        deleteR.setOnAction(e -> deleteAction());

        renameR = new MenuItem(bundle.getString("rename"));
        renameR.setOnAction(e -> renameAction());

        propertyR = new MenuItem(bundle.getString("properties"));
        propertyR.setOnAction(e -> {
            try {
                showFileProperty();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        });
    }

    public void changeRightMenu() {
        rightPopupMenu.getItems().clear();
        int selectionNumber = getActiveFileViewPage().getSelections().size();
        if (getActiveFileViewPage().getCurrentDir().length() == 0) {
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

    private FileManagerPage getActiveFileViewPage() {
        if (rootTabPane.getTabs().isEmpty()) throw new RuntimeException("No tab opened.");
        else return (FileManagerPage) rootTabPane.getSelectionModel().getSelectedItem().getContent();
    }

    private void changeClipBoardStatus() {
        // TODO: this
        if (clipBoard == null) {
            pasteR.setDisable(true);
            pasteHere.setDisable(true);
        } else {
            pasteR.setDisable(false);
            pasteHere.setDisable(false);
        }
    }

    public ContextMenu getRightPopupMenu() {
        return rightPopupMenu;
    }
}
