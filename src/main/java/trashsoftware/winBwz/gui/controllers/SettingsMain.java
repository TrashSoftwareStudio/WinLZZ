package trashsoftware.winBwz.gui.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import trashsoftware.winBwz.gui.controllers.settingsPages.GeneralPage;
import trashsoftware.winBwz.gui.controllers.settingsPages.NavigatorPage;
import trashsoftware.winBwz.gui.controllers.settingsPages.Page;
import trashsoftware.winBwz.gui.controllers.settingsPages.SettingsPage;
import trashsoftware.winBwz.gui.graphicUtil.SettingsItem;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class SettingsMain implements Initializable {

    @FXML
    TreeView<SettingsItem> treeView;

    @FXML
    ScrollPane contentPane;

    @FXML
    Button okButton, cancelButton, applyButton;

    private Stage thisStage;
    private MainUI mainUI;
    private ResourceBundle bundle;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.bundle = resources;

        setTreeViewListener();
        setUpItems();

        contentPane.setFitToWidth(true);
    }

    void setStageAndParent(Stage stage, MainUI mainUI) {
        this.thisStage = stage;
        this.mainUI = mainUI;
    }

    @FXML
    void cancelAction() {
        closeWindow();
    }

    public void closeWindow() {
        thisStage.close();
    }

    public void askParentToRestart() {
        mainUI.restartAction();
    }

    private void setUpItems() {
        TreeItem<SettingsItem> root = new TreeItem<>();

        try {
            NavigatorPage mainPage = new NavigatorPage();
            root.setValue(new SettingsItem(bundle.getString("settings"), mainPage));

            GeneralPage generalPage = new GeneralPage(this);
            root.getChildren().add(new TreeItem<>(
                    new SettingsItem(bundle.getString("general"), generalPage)));

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        root.setExpanded(true);
        treeView.setRoot(root);
    }

    private void setTreeViewListener() {
        treeView.getSelectionModel().selectedItemProperty()
                .addListener((observableValue, settingsItemTreeItem, t1) -> {
                    Page page = t1.getValue().getPage();
                    if (page instanceof SettingsPage) {
                        showPage((SettingsPage) page);
                    } else if (page instanceof NavigatorPage) {
                        showNavigatorPage(t1);
                    }
                });
    }

    private void showNavigatorPage(TreeItem<SettingsItem> t1) {
        VBox root = new VBox();
        for (TreeItem<SettingsItem> treeItem : t1.getChildren()) {
            Hyperlink link = new Hyperlink(treeItem.getValue().toString());
            link.setOnAction(e -> treeView.getSelectionModel().select(treeItem));
            root.getChildren().add(link);
        }
        contentPane.setContent(root);
    }

    private void showPage(SettingsPage settingsPage) {
        settingsPage.setApplyButtonStatusChanger(applyButton);
        applyButton.setOnAction(e -> {
            settingsPage.saveChanges();
            applyButton.setDisable(true);
        });
        okButton.setOnAction(e -> {
            settingsPage.saveChanges();
            closeWindow();
        });

        contentPane.setContent(settingsPage);
    }
}
