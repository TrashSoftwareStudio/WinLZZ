package Main;

import Console.Console;
import GUI.GUIClient;

/**
 * Main program of WinLZZ Graphic User Interface.
 *
 * @author zbh
 * @since 2018-01-20
 */
public class Main {

    public static final String version = "0.6.2";

    public static void main(String[] args) {
        if (args.length == 0) GUIClient.client(args);
        else Console.console(args);
    }
}
