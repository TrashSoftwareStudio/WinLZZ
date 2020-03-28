package trashsoftware.winBwz.gui.graphicUtil;

import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import trashsoftware.winBwz.gui.controllers.MainUI;
import trashsoftware.winBwz.gui.widgets.FileView;
import trashsoftware.winBwz.gui.widgets.GridFileView;
import trashsoftware.winBwz.gui.widgets.TableFileView;

import java.io.File;
import java.util.List;

public class FileManagerPage extends HBox {

    public static final int TABLE_VIEW = 1;
    public static final int GRID_VIEW = 2;

    protected String currentDir;

    private FileView activeView;

    private MainUI parent;

    private int currentViewMethod = GRID_VIEW;

    public FileManagerPage(MainUI parent, String dir) {
        this.parent = parent;
        this.currentDir = dir;
        refresh();
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

    public int getCurrentViewMethod() {
        return currentViewMethod;
    }

    public void switchViewMethod() {
        if (currentViewMethod == TABLE_VIEW) currentViewMethod = GRID_VIEW;
        else currentViewMethod = TABLE_VIEW;
        activeView = null;
        refresh();
    }

    public String getCurrentDir() {
        return currentDir;
    }

    public void setDir(String newDir) {
        currentDir = newDir;
        // TODO: store dir to cache
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
        if (currentDir.equals("")) {
            return new File(System.getenv("COMPUTERNAME")).getName();
        } else {
            String s = new File(currentDir).getName();
            return s.length() == 0 ? currentDir : s;  // if dir is system hard drive, File.getName() returns ""
        }
    }

    private void generateView() {
        if (currentViewMethod == TABLE_VIEW) {
            activeView = new TableFileView();
            activeView.setParentPage(this);
            getChildren().clear();
            getChildren().add(activeView);
        } else if (currentViewMethod == GRID_VIEW) {
            activeView = new GridFileView();
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
