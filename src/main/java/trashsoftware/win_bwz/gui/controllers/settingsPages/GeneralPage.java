package trashsoftware.win_bwz.gui.controllers.settingsPages;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;
import trashsoftware.win_bwz.gui.GUIClient;
import trashsoftware.win_bwz.gui.controllers.SettingsMain;
import trashsoftware.win_bwz.gui.graphicUtil.InfoBoxes;
import trashsoftware.win_bwz.resourcesPack.configLoader.GeneralLoaders;
import trashsoftware.win_bwz.resourcesPack.configLoader.NamedLocale;

import java.io.IOException;
import java.util.List;
import java.util.ResourceBundle;

public class GeneralPage extends SettingsPage {

    @FXML
    ComboBox<NamedLocale> languageBox;

    private SettingsMain parent;
    private ResourceBundle bundle;

    public GeneralPage(SettingsMain parent) throws IOException {
        bundle = GUIClient.getBundle();
        this.parent = parent;
        FXMLLoader loader = new FXMLLoader(getClass()
                .getResource("/trashsoftware/win_bwz/fxml/settingsPages/general.fxml"),
                bundle);
        loader.setRoot(this);
        loader.setController(this);

        loader.load();
        addControls(languageBox);

        initLanguageBox();
    }

    @Override
    public void saveChanges() {
        if (statusSaver.hasChanged(languageBox)) {
            NamedLocale selectedLocale = languageBox.getSelectionModel().getSelectedItem();
            GeneralLoaders.writeConfig("locale", selectedLocale.getConfigValue());
            statusSaver.store(languageBox);

            if (InfoBoxes.showConfirmBox(
                    "WinLZZ",
                    bundle.getString("pleaseConfirm"),
                    bundle.getString("needRestartApply"),
                    bundle.getString("restartNow"),
                    bundle.getString("restartLater")
            )) {
                parent.closeWindow();
                parent.askParentToRestart();
            }
        }
    }

    private void initLanguageBox() {
        List<NamedLocale> localeList = GeneralLoaders.getAllLocales();
        for (NamedLocale locale : localeList) {
            languageBox.getItems().add(locale);
            if (GUIClient.getBundle().getLocale().equals(locale.getLocale())) {
                languageBox.getSelectionModel().selectLast();
            }
        }
        statusSaver.store(languageBox);
    }
}
