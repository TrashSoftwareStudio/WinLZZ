package WinLzz.ResourcesPack.Info;

import java.io.*;

public class ChangelogReader {

    /**
     * Reads and returns the changelog text.
     *
     * @return the changelog text
     * @throws IOException if the changelog file is not readable
     */
    public String readChangelog() throws IOException {
        InputStream is = getClass().getResourceAsStream("Changelog.txt");
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "utf-8"));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) builder.append(line).append('\n');
        return builder.toString();
    }
}
