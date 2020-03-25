package trashsoftware.winBwz.gui.widgets;

import javafx.scene.layout.Pane;
import trashsoftware.winBwz.gui.controllers.MainUI;
import trashsoftware.winBwz.gui.graphicUtil.RegularFileNode;

import java.io.File;
import java.util.List;

public abstract class FileView extends Pane {

    protected MainUI parent;
    protected String  currentDir;

    public void setParent(MainUI parent) {
        this.parent = parent;
    }

    public abstract List<RegularFileNode> getSelections();

    public abstract RegularFileNode getSelection();

    public abstract void drawFiles(File directory);

    public String getCurrentDir() {
        return currentDir;
    }

    public void setDir(String newDir) {
        currentDir = newDir;
    }
}
