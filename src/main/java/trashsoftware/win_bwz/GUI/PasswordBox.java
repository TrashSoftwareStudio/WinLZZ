package trashsoftware.win_bwz.GUI;

import trashsoftware.win_bwz.ResourcesPack.Languages.LanguageLoader;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class PasswordBox implements Initializable {

    @FXML
    private PasswordField setPassword, confirmPassword;

    @FXML
    private TextField plainPasswordField;

    @FXML
    private CheckBox encryptNameBox, showPasswordBox;

    @FXML
    private ComboBox<String> algorithmBox, passwordAlgBox;

    @FXML
    private Label inputPassword, confirmLabel, promptLabel, algLabel, passAlgLabel;

    @FXML
    private Button confirmButton;

    private String[] algorithms = new String[]{"BZSE", "ZSE"};
    private String[] passAlgorithms = new String[]{"SHA-256", "SHA-384", "SHA-512"};

    private Stage stage;
    private CompressUI parent;
    private LanguageLoader lanLoader;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        promptLabel.setText("");
        showPasswordListener();
        plainPasswordListener();
        setPasswordListener();
        fillBoxes();
    }

    /**
     * Sets up the parent {@code CompressUI} which launches this {@code PasswordBox} instance.
     *
     * @param parent the parent {@code CompressUI} which launches this {@code PasswordBox} instance
     */
    void setParent(CompressUI parent) {
        this.parent = parent;
    }

    void setLanLoader(LanguageLoader lanLoader) {
        this.lanLoader = lanLoader;
        fillText();
    }

    void setStage(Stage stage) {
        this.stage = stage;
    }

    private void fillBoxes() {
        algorithmBox.getItems().addAll(algorithms);
        passwordAlgBox.getItems().addAll(passAlgorithms);
        algorithmBox.getSelectionModel().select(0);
        passwordAlgBox.getSelectionModel().select(0);
    }

    @FXML
    void confirmAction() {
        String p1 = setPassword.getText();
        String p2 = confirmPassword.getText();
        String alg = algorithmBox.getSelectionModel().getSelectedItem().toLowerCase();
        String passAlg = passwordAlgBox.getSelectionModel().getSelectedItem().toLowerCase();
        if (p1.isEmpty() && p2.isEmpty()) {
            parent.setPassword("");
            parent.setEncryption(0, alg, passAlg);
            stage.close();
        } else if (p1.equals(p2)) {
            parent.setPassword(p1);
            if (encryptNameBox.isSelected()) parent.setEncryption(2, alg, passAlg);
            else parent.setEncryption(1, alg, passAlg);
            stage.close();
        } else {
            promptLabel.setText(lanLoader.get(404));
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

    private void fillText() {
        inputPassword.setText(lanLoader.get(400));
        confirmLabel.setText(lanLoader.get(401));
        showPasswordBox.setText(lanLoader.get(402));
        encryptNameBox.setText(lanLoader.get(403));
        confirmButton.setText(lanLoader.get(1));
        algLabel.setText(lanLoader.get(405));
        passAlgLabel.setText(lanLoader.get(406));
    }
}
