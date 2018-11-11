package Main;

import WinLzz.Console.Console;
import WinLzz.GUI.GUIClient;

/**
 * Main program of WinLZZ.
 *
 * @author zbh
 * @since 2018-01-20
 */
public class Main {

    /**
     * The software version, will be displayed in GUI.
     */
    public static final String version = "0.8";

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
