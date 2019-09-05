package trashsoftware.win_bwz;

import trashsoftware.win_bwz.console.Console;
import trashsoftware.win_bwz.gui.GUIClient;

/**
 * main program of WinLZZ.
 *
 * @author zbh
 * @since 2018-01-20
 */
public class Main {

    /**
     * The software version, will be displayed in GUI.
     */
    public static final String version = "1.0 SNAPSHOT";

    /**
     * The main function of WinLZZ.
     *
     * @param args arguments
     */
    public static void main(String[] args) {
        if (args.length == 0) GUIClient.client(args);
        else Console.console(args);
    }
}
