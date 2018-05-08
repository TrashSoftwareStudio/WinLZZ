package WinLzz.ResourcesPack.Languages;

import WinLzz.ResourcesPack.ConfigLoader.GeneralLoaders;

import java.io.*;
import java.util.HashMap;

public class LanguageLoader {

    private final static String[] allLanguages = new String[]{"chs", "eng"};

    private final static String[] allLanguageNames = new String[]{"简体中文", "English"};

    private final static String defaultLanguage = "chs";

    private HashMap<Integer, String> texts = new HashMap<>();

    private String currentLanguage;

    public LanguageLoader() {
        currentLanguage = GeneralLoaders.getConfig("language");
        if (currentLanguage == null) currentLanguage = defaultLanguage;
        try {
            load();
        } catch (IOException e) {
            throw new RuntimeException("Cannot load language");
        }
    }

    public boolean changeLanguage(String newLanguageName) {
        currentLanguage = allLanguages[arrayLocate(newLanguageName, allLanguageNames)];
        try {
            GeneralLoaders.writeConfig("language", currentLanguage);
            load();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void load() throws IOException {
        if (!containsLanguage(currentLanguage)) throw new RuntimeException("No Such Language");
        InputStream is = getClass().getResourceAsStream(currentLanguage + ".txt");
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "utf-8"));
        String line;
        while ((line = br.readLine()) != null) if (line.length() > 0 && line.charAt(0) != '#' && line.contains("=")) {
            String[] split = line.split("=");
            if (split.length > 1) texts.put(Integer.valueOf(split[0]), split[1]);
            else texts.put(Integer.valueOf(split[0]), "");
        }
    }

    public String[] getAllLanguageNames() {
        return allLanguageNames;
    }

    public String get(int code) {
        return texts.get(code);
    }

    public String getCurrentLanguage() {
        return allLanguageNames[arrayLocate(currentLanguage, allLanguages)];
    }

    private static boolean containsLanguage(String s) {
        return arrayLocate(s, LanguageLoader.allLanguages) != -1;
    }

    private static int arrayLocate(String s, String[] array) {
        for (int i = 0; i < array.length; i++) if (array[i].equals(s)) return i;
        return -1;
    }
}
