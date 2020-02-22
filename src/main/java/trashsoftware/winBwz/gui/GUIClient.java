package trashsoftware.winBwz.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import trashsoftware.winBwz.gui.controllers.MainUI;
import trashsoftware.winBwz.resourcesPack.UTF8Control;
import trashsoftware.winBwz.resourcesPack.configLoader.GeneralLoaders;

import java.util.ResourceBundle;

/**
 * Graphic User Interface of WinLZZ.
 *
 * @author zbh
 * @see javafx.application.Application
 */
public class GUIClient extends Application {

    private static ResourceBundle bundle;

    /**
     * WinLZZ graphics user interface.
     *
     * @param args arguments.
     */
    public static void client(String[] args) {
        launch(args);
    }

    /**
     * Launches the GUI.
     *
     * @param primaryStage the <code>javafx.Stage</code>
     * @throws Exception if any exceptions are raised during the execution
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        showMainUi(primaryStage);
    }

    private void showMainUi(Stage primaryStage) throws Exception {
        bundle = ResourceBundle.getBundle("trashsoftware.winBwz.bundles.LangBundle",
                GeneralLoaders.getCurrentLocale(), new UTF8Control());
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/trashsoftware/winBwz/fxml/mainUI.fxml"), bundle);

        Parent root = loader.load();
        MainUI controller = loader.getController();
        controller.setStageAndParent(primaryStage, this);

        primaryStage.setTitle("WinLZZ");
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static ResourceBundle getBundle() {
        return bundle;
    }

    public void restart(Stage oldStage) {
        oldStage.close();
        Platform.runLater(() -> {
            try {
                showMainUi(new Stage());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
