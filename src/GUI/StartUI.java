/*
 * Copyright (C) 2017-2018, Trash Software Studio. All rights reserved.
 */
/*
 * Author: Bohan Zhang(a65123731@gmail.com)
 * Attributes to testing: Zhaoheng Yang(yzh8687@gmail.com)
 */

package GUI;

import ResourcesPack.ConfigLoader.GeneralLoaders;
import ResourcesPack.Languages.LanguageLoader;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.*;
import java.net.URL;
import java.util.ResourceBundle;

public class StartUI implements Initializable {

    public static final String version = "0.6.0";

    private final static String LICENCE = "    WinLZZ\n" +
            "    Copyright (C) 2017-2018  Trash Software Studio\n" +
            "\n" +
            "    This program is free software: you can redistribute it and/or modify\n" +
            "    it under the terms of the GNU General Public License as published by\n" +
            "    the Free Software Foundation, either version 3 of the License, or\n" +
            "    (at your option) any later version.\n" +
            "\n" +
            "    This program is distributed in the hope that it will be useful,\n" +
            "    but WITHOUT ANY WARRANTY; without even the implied warranty of\n" +
            "    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n" +
            "    GNU General Public License for more details.\n" +
            "\n" +
            "    You should have received a copy of the GNU General Public License\n" +
            "    along with this program.  If not, see <https://www.gnu.org/licenses/>.";

    @FXML
    private Button compressButton, uncompressButton;

    @FXML
    private Menu settingsMenu, helpMenu;

    @FXML
    private MenuItem languageSetting, about, licence, changelogView;

    private LanguageLoader lanLoader;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        lanLoader = new LanguageLoader();
        fillText();
    }

    @FXML
    public void compressMode() throws Exception {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setInitialDirectory(GeneralLoaders.readLastDir());
        File selected = dc.showDialog(null);
        if (selected != null) {
            GeneralLoaders.writeLastDir(selected);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("compressUI.fxml"));

            Parent root = loader.load();
            Stage stage = new Stage();

            stage.setTitle("WinLZZ");
            stage.setScene(new Scene(root));

            CompressUI cui = loader.getController();
            cui.setDir(selected);
            cui.setStage(stage);
            cui.load(lanLoader);
            stage.show();
        }
    }

    @FXML
    public void uncompressMode() throws Exception {
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter pzFilter =
                new FileChooser.ExtensionFilter( lanLoader.get(50) + " (*.pz)", "*.pz");
        fileChooser.getExtensionFilters().addAll(pzFilter);
        fileChooser.setInitialDirectory(GeneralLoaders.readLastDir());
        File selected = fileChooser.showOpenDialog(null);
        if (selected != null) {
            GeneralLoaders.writeLastDir(selected);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("uncompressUI.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("WinLZZ");
            stage.setScene(new Scene(root));

            UncompressUI uui = loader.getController();
            uui.setPackFile(selected);
            uui.setStage(stage);
            uui.setLanLoader(lanLoader);

            stage.setOnCloseRequest(event -> uui.close());
            stage.show();
            try {
                uui.loadContext();
            } catch (Exception e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle(lanLoader.get(51));
                alert.setHeaderText(lanLoader.get(52));
                alert.setContentText(lanLoader.get(53));
                alert.showAndWait();
                stage.close();
            }
        }
    }

    @FXML
    public void aboutAction() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("aboutUI.fxml"));
        Parent root = loader.load();
        AboutUI aui = loader.getController();
        aui.setLanLoader(lanLoader);
        Stage stage = new Stage();
        stage.setTitle("WinLZZ");
        stage.setScene(new Scene(root));
        stage.initStyle(StageStyle.UTILITY);
        stage.show();
    }

    @FXML
    public void licenceAction() {
        Pane root = new Pane();
        Stage dialog = new Stage();
        Scene scene = new Scene(root);
        dialog.setTitle(lanLoader.get(17));
        dialog.setScene(scene);

        VBox pane = new VBox();
        pane.setFillWidth(true);
        pane.setPrefSize(480.0, 280.0);
        pane.setAlignment(Pos.CENTER);

        Label label = new Label(LICENCE);
        label.setTextAlignment(TextAlignment.CENTER);
        pane.getChildren().add(label);
        root.getChildren().add(pane);
        dialog.initStyle(StageStyle.UTILITY);

        dialog.show();
    }

    @FXML
    public void changelogAction() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("changelogViewer.fxml"));
        Parent root = loader.load();
        ChangelogViewer clv = loader.getController();
        clv.setLanLoader(lanLoader);
        Stage stage = new Stage();
        stage.setTitle("WinLZZ");
        stage.setScene(new Scene(root));
        stage.initStyle(StageStyle.UTILITY);
        stage.show();
    }

    @FXML
    public void languageSelection() {
        Stage lsStage = new Stage();
        HBox pane = new HBox();
        pane.setSpacing(10.0);
        pane.setPadding(new Insets(10.0));
        ComboBox<String> languageBox = new ComboBox<>();
        languageBox.getItems().addAll(lanLoader.getAllLanguageNames());
        languageBox.getSelectionModel().select(lanLoader.getCurrentLanguage());

        Button confirm = new Button(lanLoader.get(15));  // Confirm
        confirm.setOnAction(e -> {
            if (!lanLoader.changeLanguage(languageBox.getSelectionModel().getSelectedItem()))
                System.out.println("Failed to change language");
            fillText();
            lsStage.close();
        });
        pane.getChildren().addAll(languageBox, confirm);
        Scene scene = new Scene(pane);
        lsStage.setScene(scene);
        lsStage.show();
    }

    private void fillText() {
        compressButton.setText(lanLoader.get(10));
        uncompressButton.setText(lanLoader.get(11));
        settingsMenu.setText(lanLoader.get(12));
        helpMenu.setText(lanLoader.get(13));
        languageSetting.setText(lanLoader.get(14));
        about.setText(lanLoader.get(16));
        licence.setText(lanLoader.get(17));
        changelogView.setText(lanLoader.get(18));
    }
}
