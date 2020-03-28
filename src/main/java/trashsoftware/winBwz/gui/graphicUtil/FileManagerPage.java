package trashsoftware.winBwz.gui.graphicUtil;

import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import trashsoftware.winBwz.gui.controllers.MainUI;
import trashsoftware.winBwz.gui.widgets.FileView;
import trashsoftware.winBwz.gui.widgets.TableFileView;

import java.io.File;
import java.util.List;

public class FileManagerPage extends HBox {

    public static final int TABLE_VIEW = 1;
    public static final int GRID_VIEW = 2;

    protected String currentDir;

    private FileView activeView;

    private MainUI parent;

    private int currentViewMethod = TABLE_VIEW;

    public FileManagerPage() {

    }

    public MainUI getParentMainUi() {
        return parent;
    }

    public void setParent(MainUI parent) {
        this.parent = parent;
    }

    public void setViewMethod(int viewMethod) {
        this.currentViewMethod = viewMethod;
        refresh();
    }

    public String getCurrentDir() {
        return currentDir;
    }

    public void setDir(String newDir) {
        currentDir = newDir;
    }

    public List<RegularFileNode> getSelections() {
        return activeView.getSelections();
    }

    public RegularFileNode getSelection() {
        return activeView.getSelection();
    }

    public void refresh() {
        if (activeView == null) {
            generateView();
        }
        activeView.drawFiles();
    }

    public String getName() {
        return currentDir;
//        if (currentDir.contains(File.separator)) {
//            if (currentDir.endsWith(File.separator)) {
//                return currentDir.substring(
//                        currentDir.substring(0, currentDir.length() - 1).lastIndexOf(File.separator));
//            } else {
//                return currentDir.substring(currentDir.lastIndexOf(File.separator));
//            }
//        } else {
//            return currentDir;
//        }
    }

    private void generateView() {
        if (currentViewMethod == TABLE_VIEW) {
            activeView = new TableFileView();
            activeView.setParentPage(this);
            getChildren().clear();
            getChildren().add(activeView);
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof FileManagerPage && ((FileManagerPage) o).currentDir.equals(this.currentDir);
    }
}
