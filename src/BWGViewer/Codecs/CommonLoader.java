package BWGViewer.Codecs;

import java.io.IOException;

public interface CommonLoader {

    String getType();
    void load() throws IOException, UnsupportedFormatException;
    byte[] getContent();
    int getWidth();
    int getHeight();
    int getDigits();
    long getCreationTime();
}
