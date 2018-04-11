package Info;

import java.io.*;

public class ChangelogReader {

    public String readChangelog() throws IOException {
        InputStream is = this.getClass().getResourceAsStream("/Info/Changelog.txt");
        BufferedInputStream br = new BufferedInputStream(is);
        byte[] b = new byte[32768];
        int read;
        if ((read = br.read(b)) == -1) {
            throw new IOException("Error occurs during loading resources");
        }
        byte[] result = new byte[read];
        System.arraycopy(b, 0, result, 0, read);
        return new String(result, "utf-8");
    }
}
