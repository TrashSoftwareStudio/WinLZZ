module WinLZZ {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;

    opens trashsoftware.win_bwz.gui.controllers;
    opens trashsoftware.win_bwz.gui.graphicUtil;

    exports trashsoftware.win_bwz.gui;
    exports trashsoftware.win_bwz.gui.controllers;
}