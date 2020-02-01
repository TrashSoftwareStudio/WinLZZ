package trashsoftware.win_bwz.gui.graphicUtil;

import javafx.scene.control.Button;

/**
 * An extension of <code>Button</code>, which represents a directory level.
 *
 * @author zbh
 * @see javafx.scene.control.Button
 * @since 0.8
 */
public class DirButton extends Button {

    private String fullPath;

    /**
     * Constructor of a new <code>DirButton</code> instance.
     *
     * @param fullPath    the full path of the represented directory
     * @param showingName the name to be shown on the button surface
     */
    public DirButton(String fullPath, String showingName) {
        super();
        this.fullPath = fullPath;
        setText(showingName);
    }

    /**
     * Returns the full path.
     *
     * @return the full path
     */
    public String getFullPath() {
        return fullPath;
    }
}
