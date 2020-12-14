package trashsoftware.winBwz.resourcesPack.configLoader;

import javafx.scene.image.Image;
import trashsoftware.winBwz.resourcesPack.NamedLocale;
import trashsoftware.winBwz.resourcesPack.UTF8Control;

import java.io.*;
import java.util.*;

/**
 * Common config or resources loaders.
 *
 * @author zbh
 * @since 0.6
 */
public class GeneralLoaders {

    private static final String PREF_FILE = "pref.cfg";
    static final String CACHE_DIR = "cache";
    private static final String THUMBNAIL_DIR = CACHE_DIR + File.separator + "thumbnails";

    /**
     * Returns the current selected locale.
     *
     * @return the current selected locale
     */
    public static Locale getCurrentLocale() {
        String localeName = getConfig("locale");
        if (localeName == null) {
            return Locale.getDefault();
        } else {
            String[] lanCountry = localeName.split("_");
            return new Locale(lanCountry[0], lanCountry[1]);
        }
    }

    /**
     * Returns a list of all supported locales.
     *
     * @return a {@code List} list of all supported locales
     */
    public static List<NamedLocale> getAllLocales() {
        List<NamedLocale> locales = new ArrayList<>();
        ResourceBundle resourceBundle = ResourceBundle.getBundle(
                "trashsoftware.winBwz.bundles.Languages",
                new UTF8Control()
        );
        Enumeration<String> keys = resourceBundle.getKeys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            String[] lanLocale = key.split("_");
            NamedLocale namedLocale = new NamedLocale(lanLocale[0], lanLocale[1], resourceBundle.getString(key));
            locales.add(namedLocale);
        }
        return locales;
    }

    /**
     * Reads all configs into a {@code HashMap<String, String>}.
     *
     * @return a {@code HashMap<String, String>} containing all configs saved in pref.ini
     */
    private static HashMap<String, String> readAllConfigs() {
        File pref = new File(PREF_FILE);
        try {
            InputStreamReader isr = new InputStreamReader(new FileInputStream(pref));
            BufferedReader br = new BufferedReader(isr);
            HashMap<String, String> config = new HashMap<>();
            String line;
            while ((line = br.readLine()) != null) if (line.contains("=")) {
                String[] array = line.split("=");
                config.put(array[0], array[1]);
            }
            return config;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns the configuration corresponding to its name <code>item</code>.
     *
     * @param item the name of the configuration to be returned
     * @return the configuration corresponding to its name <code>item</code>.
     */
    public static String getConfig(String item) {
        HashMap<String, String> configs = readAllConfigs();
        if (configs != null) return configs.get(item);
        else return null;
    }

    /**
     * Writes a configuration into the pref file.
     *
     * @param item   the name of the configuration to be written
     * @param config the configuration
     */
    public static void writeConfig(String item, String config) {
        HashMap<String, String> configs = readAllConfigs();
        if (configs == null) configs = new HashMap<>();
        configs.put(item, config);
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(PREF_FILE));
            for (String key : configs.keySet()) {
                out.write(key + "=" + configs.get(key));
                out.write('\n');
            }
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void deleteConfig(String item) throws IOException {
        HashMap<String, String> configs = readAllConfigs();
        if (configs == null) configs = new HashMap<>();
        configs.remove(item);
        BufferedWriter out = new BufferedWriter(new FileWriter("pref.ini"));
        for (String key : configs.keySet()) {
            out.write(key + "=" + configs.get(key));
            out.write('\n');
        }
        out.flush();
        out.close();
    }

    /**
     * Returns the cached thumbnail file of the image with absolute path {@code imageFileOrigName}
     *
     * @param imageFileOrigName the absolute path of the original image
     * @return the cached thumbnail, {@code null} if does not exist
     */
    public static Image getThumbnailByOrigName(String imageFileOrigName) {
        String thumbnailName = nameToThumbnailName(imageFileOrigName);
        return getThumbnail(thumbnailName);
    }

    /**
     * Returns the cached thumbnail file with absolute path {@code thumbnailName}
     *
     * @param thumbnailName the absolute path of the thumbnail image
     * @return the cached thumbnail, {@code null} if does not exist
     */
    public static Image getThumbnail(String thumbnailName) {
        File file = new File(thumbnailName);
        if (file.exists()) {
            try {
                FileInputStream inputStream = new FileInputStream(file);
                Image image = new Image(inputStream);
                inputStream.close();
                return image;
            } catch (IOException e) {
                return null;
            }
        } else {
            File thumbDir = new File(THUMBNAIL_DIR);
            if (!thumbDir.exists())
                if (!thumbDir.mkdirs())
                    System.out.println("Failed to create thumbnail directory");
            return null;
        }
    }

    public static String nameToThumbnailName(String origName) {
        String name = origName.replace(File.separatorChar, '.').replace(':', ';');
        return String.format("%s%s~%s.thumbnail", THUMBNAIL_DIR, File.separator, name);
    }
}
