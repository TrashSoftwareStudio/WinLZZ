package trashsoftware.winBwz.gui.widgets;

import javafx.beans.property.DoubleProperty;
import javafx.scene.layout.Pane;
import trashsoftware.winBwz.gui.controllers.MainUI;
import trashsoftware.winBwz.gui.graphicUtil.FileManagerPage;
import trashsoftware.winBwz.gui.graphicUtil.RegularFileNode;

import java.io.File;
import java.util.List;

public abstract class FileView extends Pane {

    protected FileManagerPage parentPage;

    public FileView() {
    }

    public void setParentPage(FileManagerPage parentPage) {
        this.parentPage = parentPage;
    }

    public abstract List<RegularFileNode> getSelections();

    public abstract RegularFileNode getSelection();

    public abstract void drawFiles();

    public abstract DoubleProperty contentPrefHeightProperty();

    public abstract DoubleProperty contentPrefWidthProperty();

    protected MainUI getFileManager() {
        return parentPage.getParentMainUi();
    }
}
