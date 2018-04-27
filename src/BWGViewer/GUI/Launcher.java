package BWGViewer.GUI;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class Launcher {

    public void launch(File pictureFile) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("viewer.fxml"));

        Stage viewerStage = new Stage();

        Parent root = loader.load();
        viewerStage.setTitle("BWGViewer");
        Scene scene = new Scene(root);
        viewerStage.setScene(scene);
        viewerStage.setMaximized(true);
        viewerStage.setResizable(false);
        viewerStage.show();

        Viewer viewer = loader.getController();
        viewer.setStage(viewerStage);
        viewer.setScene(scene);
        if (pictureFile != null) {
            viewer.setPictureFile(pictureFile);
            viewer.loadPicture();
        }
    }
}
