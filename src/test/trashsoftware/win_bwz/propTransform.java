package trashsoftware.win_bwz;

import java.io.*;

public class propTransform {

    public static void main(String[] args) throws Exception {
        String zh = "zhCn.properties";
        String en = "enUs.properties";

        String enIn = "engT.txt";

        BufferedReader zhR = new BufferedReader(new FileReader(zh));
        BufferedReader enR = new BufferedReader(new FileReader(enIn));

        BufferedWriter w = new BufferedWriter(new FileWriter(en));
        String zhLine;
        String enLine;
        while ((zhLine = zhR.readLine()) != null) {
            enLine = enR.readLine();
            if (zhLine.length() > 0) {
                String[] zhSplit = zhLine.split("=");
                String[] enSplit = enLine.split("=");
                if (zhSplit.length == 2 && enSplit.length == 2) {
                    String outLine = zhSplit[0] + "=" + enSplit[1] + "\n";
                    w.write(outLine);
                    continue;
                }
            }
            w.write(enLine + "\n");
        }

        zhR.close();
        enR.close();
        w.flush();
        w.close();
    }
}
