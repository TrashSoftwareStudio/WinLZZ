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

    public static final String version = "0.7.0";

    public static void main(String[] args) {
        if (args.length == 0) GUIClient.client(args);
        else Console.console(args);
    }
}
