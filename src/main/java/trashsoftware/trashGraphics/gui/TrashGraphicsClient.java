package trashsoftware.trashGraphics.gui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import trashsoftware.winBwz.gui.controllers.MainUI;

import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;

public class TrashGraphicsClient {

    public void runClient(ResourceBundle bundle, MainUI parent) throws IOException {
        Stage stage = new Stage();

        create(stage, bundle, parent);

        stage.show();
    }

    public void runClientWithImage(ResourceBundle bundle, MainUI parent, File image) throws IOException {
        Stage stage = new Stage();

        TgMainUI controller = create(stage, bundle, parent);
        controller.setInitImage(image);

        stage.show();
        controller.showLoadingBlocker();
    }

    private TgMainUI create(Stage stage, ResourceBundle bundle, MainUI parent) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/trashsoftware/trashGraphics/fxml/tgMainUi.fxml"), bundle);
        Parent root = loader.load();

        stage.setTitle("TrashGraphics");
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setMaximized(true);

        TgMainUI controller = loader.getController();
        controller.setStage(stage, scene, parent);

        return controller;
    }
}
