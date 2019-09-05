package trashsoftware.win_bwz.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Graphic User Interface of WinLZZ.
 *
 * @author zbh
 * @see javafx.application.Application
 */
public class GUIClient extends Application {

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
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/trashsoftware/win_bwz/fxml/mainUI.fxml"));

        Parent root = loader.load();
        primaryStage.setTitle("WinLZZ");
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(false);
        primaryStage.show();
    }
}
