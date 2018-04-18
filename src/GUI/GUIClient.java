package GUI;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class GUIClient extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("startUI.fxml"));

        Parent root = loader.load();
        primaryStage.setTitle("WinLZZ");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }
}
