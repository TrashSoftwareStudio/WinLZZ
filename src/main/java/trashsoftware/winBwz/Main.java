package trashsoftware.winBwz;

import trashsoftware.winBwz.console.Console;
import trashsoftware.winBwz.gui.GUIClient;

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
    public static final String VERSION = "1.0 Alpha 8";

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
