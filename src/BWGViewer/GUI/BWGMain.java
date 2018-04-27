package BWGViewer.GUI;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class BWGMain extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("viewer.fxml"));

        Parent root = loader.load();
        primaryStage.setTitle("BWGViewer");
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.setResizable(false);
        primaryStage.show();

        Viewer viewer = loader.getController();
        viewer.setStage(primaryStage);
        viewer.setScene(scene);
    }
}
