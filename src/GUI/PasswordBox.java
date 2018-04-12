package GUI;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class PasswordBox implements Initializable {

    @FXML
    private PasswordField setPassword;

    @FXML
    private PasswordField confirmPassword;

    @FXML
    private TextField plainPasswordField;

    @FXML
    private CheckBox encryptNameBox;

    @FXML
    private CheckBox showPasswordBox;

    @FXML
    private Label confirmLabel;

    @FXML
    private Label promptLabel;

    private Stage stage;

    private CompressUI parent;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        promptLabel.setText("");
        showPasswordListener();
        plainPasswordListener();
        setPasswordListener();
    }

    public void setParent(CompressUI parent) {
        this.parent = parent;
    }

    void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    void confirmAction() {
        String p1 = setPassword.getText();
        String p2 = confirmPassword.getText();
        if (p1.isEmpty() && p2.isEmpty()) {
            parent.setPassword("");
            parent.setEncryptLevel(0);
            stage.close();
        } else if (p1.equals(p2)) {
            parent.setPassword(p1);
            if (encryptNameBox.isSelected()) parent.setEncryptLevel(2);
            else parent.setEncryptLevel(1);
            stage.close();
        } else {
            promptLabel.setText("两次输入不一致");
        }
    }

    private void showPasswordListener() {
        showPasswordBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                setPassword.setVisible(false);
                setPassword.setManaged(false);
                confirmPassword.setVisible(false);
                confirmLabel.setVisible(false);
                plainPasswordField.setVisible(true);
                plainPasswordField.setManaged(true);

                promptLabel.setText("");
                confirmPassword.setText(setPassword.getText());
            } else {
                setPassword.setVisible(true);
                setPassword.setManaged(true);
                confirmPassword.setVisible(true);
                confirmLabel.setVisible(true);
                plainPasswordField.setVisible(false);
                plainPasswordField.setManaged(false);
            }
        });
    }

    private void plainPasswordListener() {
        plainPasswordField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (showPasswordBox.isSelected()) {
                setPassword.setText(newValue);
                confirmPassword.setText(newValue);
            }
        });
    }

    private void setPasswordListener() {
        setPassword.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!showPasswordBox.isSelected()) plainPasswordField.setText(newValue);
        });
    }
}
