package trashsoftware.winBwz.gui.controllers.settingsPages;

import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;

import java.util.ArrayList;
import java.util.List;

public abstract class SettingsPage extends Page {

    private List<ComboBox> comboBoxes = new ArrayList<>();
    private List<CheckBox> checkBoxes = new ArrayList<>();

    StatusSaver statusSaver = new StatusSaver();

    /**
     * Saves all stated changes to configuration file.
     */
    public abstract void saveChanges();

//    /**
//     * Returns the title to be displayed on the main page of settings.
//     *
//     * @return the title to be displayed on the main page of settings
//     */
//    public abstract String getTitle();

    public void setApplyButtonStatusChanger(Button applyButton) {
        for (ComboBox comboBox : comboBoxes) {
            comboBox.getSelectionModel().selectedIndexProperty().addListener(((observableValue, number, t1) -> {
                if (isAnyStatusChanged()) applyButton.setDisable(false);
                else applyButton.setDisable(true);
            }));
        }
        for (CheckBox checkBox : checkBoxes) {
            checkBox.selectedProperty().addListener(((observableValue, aBoolean, t1) -> {
                if (isAnyStatusChanged()) applyButton.setDisable(false);
                else applyButton.setDisable(true);
            }));
        }
    }

    private boolean isAnyStatusChanged() {
        for (ComboBox comboBox : comboBoxes) {
            if (statusSaver.hasChanged(comboBox)) return true;
        }
        for (CheckBox checkBox : checkBoxes) {
            if (statusSaver.hasChanged(checkBox)) return true;
        }
        return false;
    }

    /**
     * Adds all controllable {@code Control}'s to page.
     *
     * This method should be called just after {@code FXMLLoader.load} in the constructor of any sub-classes of this.
     *
     * @param controls array of controllable {@code Control}'s
     */
    void addControls(Control... controls) {
        for (Control control : controls) {
            if (control instanceof ComboBox) comboBoxes.add((ComboBox) control);
            else if (control instanceof CheckBox) checkBoxes.add((CheckBox) control);

            else throw new RuntimeException("Unrecognizable Control");
        }
    }
}
