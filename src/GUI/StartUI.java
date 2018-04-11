/*
 * Copyright (C) 2017-2018, Trash Software Studio. All rights reserved.
 */

/*
 * Author: Bohan Zhang(a65123731@gmail.com)
 * Tester: Zhaoheng Yang(yzh8687@gmail.com)
 */

package GUI;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
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

    public static final String version = "0.5.1";

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

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    @FXML
    public void compressMode() throws Exception {

        DirectoryChooser dc = new DirectoryChooser();
        dc.setInitialDirectory(readLastDir());
        File selected = dc.showDialog(null);
        if (selected != null) {
            writeLastDir(selected);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("compressUI.fxml"));

            Parent root = loader.load();
            Stage stage = new Stage();

            stage.setTitle("WinLZZ");
            stage.setScene(new Scene(root));

            CompressUI cui = loader.getController();
            cui.setDir(selected);
            cui.setStage(stage);
            stage.show();
        }
    }

    @FXML
    public void uncompressMode() throws Exception {
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter pzFilter =
                new FileChooser.ExtensionFilter("LZZ 压缩包 (*.pz)", "*.pz");
        fileChooser.getExtensionFilters().addAll(pzFilter);
        fileChooser.setInitialDirectory(readLastDir());
        File selected = fileChooser.showOpenDialog(null);
        if (selected != null) {

            writeLastDir(selected);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("uncompressUI.fxml"));

            Parent root = loader.load();

            Stage stage = new Stage();

            stage.setTitle("WinLZZ");
            stage.setScene(new Scene(root));

            UncompressUI uui = loader.getController();
            uui.setPackFile(selected);
            uui.setStage(stage);

            stage.setOnCloseRequest(event -> uui.close());

            stage.show();
            try {
                uui.loadContext();
            } catch (Exception e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("无法打开文件");
                alert.setHeaderText("无法读取该压缩文件");
                alert.setContentText("该文件已经损坏");

                alert.showAndWait();

                stage.close();
            }
        }
    }

    @FXML
    public void aboutAction() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("aboutUI.fxml"));

        Parent root = loader.load();

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
        dialog.setTitle("开源许可证");
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

        Stage stage = new Stage();

        stage.setTitle("WinLZZ");
        stage.setScene(new Scene(root));
        stage.initStyle(StageStyle.UTILITY);

        stage.show();
    }

    static File readLastDir() {
        File pref = new File("pref.ini");
        try {
            InputStreamReader isr = new InputStreamReader(new FileInputStream(pref));
            BufferedReader br = new BufferedReader(isr);
            String dir = br.readLine();
            File f = new File(dir);
            if (f.exists()) {
                return f;
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static void writeLastDir(File lastDir) throws IOException {
        File pref = new File("pref.ini");
        if (!pref.exists()) {
            if (!pref.createNewFile()) {
                throw new IOException("Cannot create preference file");
            }
        }
        String path = lastDir.getParentFile().getAbsolutePath();
        BufferedWriter out = new BufferedWriter(new FileWriter(pref));
        out.write(path);
        out.flush();
        out.close();
    }
}
