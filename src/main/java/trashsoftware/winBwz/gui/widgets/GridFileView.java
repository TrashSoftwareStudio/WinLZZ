package trashsoftware.winBwz.gui.widgets;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import trashsoftware.winBwz.gui.GUIClient;
import trashsoftware.winBwz.gui.graphicUtil.RegularFileNode;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

public class GridFileView extends FileView {

    private static final int ICONS_PER_ROW = 6;

    private ResourceBundle bundle = GUIClient.getBundle();

    private int r, c;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private GridPane basePane;

    public GridFileView() {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/trashsoftware/winBwz/fxml/widgets/gridFileView.fxml"),
                bundle);
        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate view", e);
        }

        initialize();
    }

    private void initialize() {

    }

    @Override
    public List<RegularFileNode> getSelections() {
        return null;
    }

    @Override
    public RegularFileNode getSelection() {
        return null;
    }

    @Override
    public void drawFiles() {
        basePane.getChildren().clear();
        r = c = 0;
        if (parentPage.getCurrentDir().equals("")) {  // system root dir
            for (File d : File.listRoots())
                addToGrid(new RegularFileNode(d, bundle));
        } else {
            ArrayList<RegularFileNode> nonDirectories = new ArrayList<>();
            File directory = new File(parentPage.getCurrentDir());  // TODO: check existence
            for (File f : Objects.requireNonNull(directory.listFiles())) {
                if (f.isDirectory()) addToGrid(new RegularFileNode(f, bundle));
                else nonDirectories.add(new RegularFileNode(f, bundle));
            }
            for (RegularFileNode rfn : nonDirectories) addToGrid(rfn);
        }
    }

    private void addToGrid(RegularFileNode rfn) {
        Image icon = getIcon(rfn.getFile());
        ImageView iconView = new ImageView(icon);
        iconView.setFitWidth(50.0);
        iconView.setFitHeight(50.0);

        VBox smallBox = new VBox();
        String name = rfn.getName();
        if (name.length() > 10) name = "..." + name.substring(name.length() - 10);
        smallBox.getChildren().addAll(iconView, new Label(name));
        basePane.add(smallBox, c++, r);
        if (c == ICONS_PER_ROW) {
            c = 0;
            r++;
        }
    }

    private Image getIcon(File file) {
        Icon icon = FileSystemView.getFileSystemView().getSystemIcon(file);
        BufferedImage bi = new BufferedImage(icon.getIconWidth() * 2, icon.getIconHeight() * 2, BufferedImage.TYPE_INT_RGB);
//        Graphics g = bi.getGraphics();
        Graphics2D g2d = bi.createGraphics();
        g2d.setPaint(new Color(255, 255, 255));
        g2d.fillRect(0, 0, bi.getWidth(), bi.getHeight());
        icon.paintIcon(null, g2d, 0, 0);
//        g.setColor(Color.WHITE);
        g2d.dispose();
        return SwingFXUtils.toFXImage(bi, null);
    }
}
