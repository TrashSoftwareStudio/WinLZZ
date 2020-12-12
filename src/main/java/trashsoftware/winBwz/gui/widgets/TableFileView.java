package trashsoftware.winBwz.gui.widgets;

import javafx.beans.property.DoubleProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.util.Callback;
import trashsoftware.trashGraphics.core.ImageViewer;
import trashsoftware.winBwz.gui.GUIClient;
import trashsoftware.winBwz.gui.graphicUtil.ReadableSize;
import trashsoftware.winBwz.gui.graphicUtil.RegularFileNode;
import trashsoftware.winBwz.resourcesPack.configLoader.GeneralLoaders;
import trashsoftware.winBwz.utility.Util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

public class TableFileView extends FileView {

    @FXML
    private TableView<RegularFileNode> table;

    @FXML
    private TableColumn<RegularFileNode, String> nameCol, typeCol, timeCol;

    @FXML
    private TableColumn<RegularFileNode, ReadableSize> sizeCol;

    private final ResourceBundle bundle = GUIClient.getBundle();

    private final Label placeHolder = new Label();

    public TableFileView() {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/trashsoftware/winBwz/fxml/widgets/tableFileView.fxml"),
                bundle);
        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate view");
        }

        initialize();
    }

    @Override
    public DoubleProperty contentPrefHeightProperty() {
        return table.prefHeightProperty();
    }

    @Override
    public DoubleProperty contentPrefWidthProperty() {
        return table.prefWidthProperty();
    }

    private void initialize() {
        setNameColHoverFactory();
        setSizeColHoverFactory();

        nameCol.setCellValueFactory(new PropertyValueFactory<>("Name"));
        typeCol.setCellValueFactory(new PropertyValueFactory<>("Type"));
        sizeCol.setCellValueFactory(new PropertyValueFactory<>("Size"));
        timeCol.setCellValueFactory(new PropertyValueFactory<>("LastModified"));
        setTableListener();
        table.setPlaceholder(placeHolder);

        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    @Override
    public List<RegularFileNode> getSelections() {
        return table.getSelectionModel().getSelectedItems();
    }

    @Override
    public RegularFileNode getSelection() {
        return table.getSelectionModel().getSelectedItem();
    }

    @Override
    public void drawFiles() {
        table.getItems().clear();
        if (parentPage.getCurrentDir().equals("")) {  // system root dir
            for (File d : File.listRoots())
                table.getItems().add(new RegularFileNode(d, bundle));
        } else {
            ArrayList<RegularFileNode> nonDirectories = new ArrayList<>();
            File directory = new File(parentPage.getCurrentDir());  // TODO: check existence
            for (File f : Objects.requireNonNull(directory.listFiles())) {
                if (f.isDirectory()) table.getItems().add(new RegularFileNode(f, bundle));
                else nonDirectories.add(new RegularFileNode(f, bundle));
            }
            table.getItems().addAll(nonDirectories);
        }
    }

    /**
     * Sets up the selection listener of the TableView object "table".
     */
    private void setTableListener() {
        table.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            getFileManager().selectFile(newValue);
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
//                                        parentPage.forwardDir();
                                        parentPage.setDir(item.getFullPath());
                                        getFileManager().gotoDirectory();
                                    } else {
                                        try {
                                            getFileManager().openAction();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }

                            if (click.getButton() == MouseButton.SECONDARY) {
                                if (item == null) table.getSelectionModel().clearSelection();
                                getFileManager().changeRightMenu();
                                getFileManager().getRightPopupMenu().show(table, click.getScreenX(), click.getScreenY());
                            } else getFileManager().getRightPopupMenu().hide();
                        });
                    }
                };
            }
        });
    }

    /**
     * Sets up the hover property listener of the TableColumn object "nameCol".
     */
    private void setSizeColHoverFactory() {
        sizeCol.setCellFactory((TableColumn<RegularFileNode, ReadableSize> tc) ->
                new TableCell<RegularFileNode, ReadableSize>() {
                    @Override
                    protected void updateItem(ReadableSize item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty) {
                            setText(null);
                        } else {
                            setText(item.toString());
                            hoverProperty().addListener((ObservableValue<? extends Boolean> obs, Boolean wasHovered,
                                                         Boolean isNowHovered) -> {
                                if (isNowHovered && !isEmpty()) {
                                    String sizeInByte = getItem().getSizeInByte(bundle);
                                    Tooltip tp = new Tooltip();
                                    tp.setText(sizeInByte);
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
                        if (isNowHovered && !isEmpty()) {
                            String fileNameText = getText();
                            int dotIndex = fileNameText.lastIndexOf('.');
                            String ext = "";
                            if (dotIndex >= 0) {
                                ext = fileNameText.substring(dotIndex + 1);
                            }
                            if (Util.arrayContains(ImageViewer.ALL_FORMATS_READ, ext, false)) {
                                showThumbnail(fileNameText);
                            } else if (fileNameText.length() > 40) {
                                Tooltip tp = new Tooltip();
                                tp.setText(getText());
                                table.setTooltip(tp);
                            }
                        } else {
                            table.setTooltip(null);
                        }
                    });
                }
            }


        });
    }

    private void showThumbnail(String fileName) {
        String fullName = parentPage.getCurrentDir() + File.separator + fileName;
        Image thumbImage = GeneralLoaders.getThumbnailByOrigName(fullName);
        if (thumbImage == null) {
            String thumbName = createThumbnail(fullName);
            thumbImage = GeneralLoaders.getThumbnail(thumbName);
            if (thumbImage == null) {  // something went wrong
                System.out.println("Cannot produce thumbnail");
                return;
            }
        }
        ImageView imageView = new ImageView(thumbImage);
        Tooltip tp = new Tooltip();
        tp.setGraphic(imageView);
        table.setTooltip(tp);
    }

    private String createThumbnail(String fullName) {
        String thumbName = GeneralLoaders.nameToThumbnailName(fullName);
        if (!ImageViewer.produceThumbnail(fullName, thumbName, 200)) {
            System.out.println("Failed to produce thumbnail");
        }
        return thumbName;
    }

//    private void setRightPopupMenu() {
//        openR = new MenuItem(bundle.getString("open"));
//        openR.setOnAction(e -> {
//            try {
//                getFileManager().openAction();
//            } catch (Exception e1) {
//                e1.printStackTrace();
//            }
//        });
//
//        openDirR = new MenuItem(bundle.getString("openLocation"));
//        openDirR.setOnAction(e -> getFileManager().desktopOpenAction());
//
//        compressR = new MenuItem(bundle.getString("compress"));
//        compressR.setOnAction(e -> {
//            try {
//                getFileManager().compressMode();
//            } catch (Exception e1) {
//                e1.printStackTrace();
//            }
//        });
//
//        copyR = new MenuItem(bundle.getString("copy"));
//        copyR.setOnAction(e -> getFileManager().copyAction());
//
//        cutR = new MenuItem(bundle.getString("cut"));
//        cutR.setOnAction(e -> getFileManager().cutAction());
//
//        pasteR = new MenuItem(bundle.getString("paste"));
//        pasteR.setOnAction(e -> getFileManager().pasteAction());
//
//        deleteR = new MenuItem(bundle.getString("delete"));
//        deleteR.setOnAction(e -> getFileManager().deleteAction());
//
//        renameR = new MenuItem(bundle.getString("rename"));
//        renameR.setOnAction(e -> getFileManager().renameAction());
//
//        propertyR = new MenuItem(bundle.getString("properties"));
//        propertyR.setOnAction(e -> {
//            try {
//                getFileManager().showFileProperty();
//            } catch (IOException e1) {
//                e1.printStackTrace();
//            }
//        });
//    }
//
//    private void changeRightMenu() {
//        rightPopupMenu.getItems().clear();
//        int selectionNumber = table.getSelectionModel().getSelectedItems().size();
//        if (currentDir.length() == 0) {
//            // The current opening directory is the system root
//            if (selectionNumber != 0) rightPopupMenu.getItems().addAll(openR, openDirR, new SeparatorMenuItem(),
//                    propertyR);
//        } else {
//            // The current opening directory is a regular directory
//            if (selectionNumber == 0)
//                rightPopupMenu.getItems().addAll(pasteR, propertyR);
//            else if (selectionNumber == 1)
//                rightPopupMenu.getItems().addAll(openR, openDirR, new SeparatorMenuItem(), compressR,
//                        new SeparatorMenuItem(), copyR, cutR, pasteR, new SeparatorMenuItem(), deleteR, renameR,
//                        new SeparatorMenuItem(), propertyR);
//            else
//                rightPopupMenu.getItems().addAll(openR, openDirR, new SeparatorMenuItem(), compressR,
//                        new SeparatorMenuItem(), copyR, cutR, pasteR, new SeparatorMenuItem(), deleteR,
//                        new SeparatorMenuItem(), propertyR);
//        }
//    }
}
