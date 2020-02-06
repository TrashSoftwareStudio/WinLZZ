package trashsoftware.win_bwz.gui.graphicUtil;

import trashsoftware.win_bwz.gui.controllers.settingsPages.Page;

public class SettingsItem {

    private String name;
    private Page page;

    public SettingsItem(String name, Page page) {
        this.name = name;
        this.page = page;
    }

    public Page getPage() {
        return page;
    }

    @Override
    public String toString() {
        return name;
    }
}
