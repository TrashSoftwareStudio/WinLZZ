package trashsoftware.winBwz.gui.graphicUtil;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.util.Optional;

public class InfoBoxes {

    public static boolean showConfirmBox(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, header);

        alert.setTitle(title);
        alert.setContentText(content);

        alert.showAndWait();

        return alert.getResult() == ButtonType.OK;
    }

    public static boolean showConfirmBox(String title, String header, String content, String okMsg, String cancelMsg) {
        ButtonType okBtn = new ButtonType(okMsg, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType(cancelMsg, ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert= new Alert(Alert.AlertType.CONFIRMATION, header, okBtn, cancelBtn);;

        alert.setTitle(title);
        alert.setContentText(content);

        Optional<ButtonType> result = alert.showAndWait();

        return result.orElse(cancelBtn) == okBtn;
    }
}
