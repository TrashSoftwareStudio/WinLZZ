package trashsoftware.win_bwz.resourcesPack.languages;

import trashsoftware.win_bwz.resourcesPack.configLoader.GeneralLoaders;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * The text displaying program.
 *
 * @author zbh
 * @since 0.6
 */
public class LanguageLoader {

    /**
     * Abbreviation codes of all languages.
     */
    private final static String[] allLanguages = new String[]{"chs", "eng"};

    /**
     * Name of all languages in each languages themselves.
     */
    private final static String[] allLanguageNames = new String[]{"简体中文", "English"};

    private final static String defaultLanguage = "chs";

    private HashMap<Integer, String> texts = new HashMap<>();

    /**
     * The abbreviation code of the current loading language.
     */
    private String currentLanguage;

    /**
     * Creates a new {@code LanguageLoader} instance.
     */
    public LanguageLoader() {
        currentLanguage = GeneralLoaders.getConfig("language");
        if (currentLanguage == null) currentLanguage = defaultLanguage;
        try {
            load();
        } catch (IOException e) {
            throw new RuntimeException("Cannot load language");
        }
    }

    /**
     * Change to a new language.
     *
     * @param newLanguageName the abbreviation code of the new language
     * @return {@code true} if the change succeed
     */
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
        InputStream is = new FileInputStream(currentLanguage + ".txt");
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        String line;
        while ((line = br.readLine()) != null) if (line.length() > 0 && line.charAt(0) != '#' && line.contains("=")) {
            String[] split = line.split("=");
            if (split.length > 1) texts.put(Integer.valueOf(split[0]), split[1]);
            else texts.put(Integer.valueOf(split[0]), "");
        }
    }

    /**
     * Returns an array of all name of languages, in their own language.
     *
     * @return an array of all name of languages, in their own language.
     */
    public String[] getAllLanguageNames() {
        return allLanguageNames;
    }

    /**
     * Returns the text corresponds to the integer id, in the current loading language.
     *
     * @param code the id of the text to be displayed
     * @return the corresponding text in the current loading language
     */
    @Deprecated
    public String get(int code) {
        return texts.get(code);
    }

    /**
     * Returns the name of the current loading language.
     *
     * @return the name of the current loading language
     */
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
