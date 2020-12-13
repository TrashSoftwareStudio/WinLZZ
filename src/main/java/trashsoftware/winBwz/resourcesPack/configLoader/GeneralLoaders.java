package trashsoftware.winBwz.resourcesPack.configLoader;

import javafx.scene.image.Image;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;
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
public abstract class GeneralLoaders {

    private static final String PREF_FILE = "pref.cfg";
    private static final String CACHE_DIR = "cache";
    private static final String CACHE = CACHE_DIR + File.separator + "cache.json";
    private static final String OPENED_DIRS_CACHE = CACHE_DIR + File.separator + "dirs.json";
    private static final String THUMBNAIL_DIR = CACHE_DIR + File.separator + "thumbnails";

    private static JSONObject cache;

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

    private static JSONObject readAllCache() {
        if (cache == null) {
            JSONParser parser = new JSONParser();
            try {
                JSONObject obj = (JSONObject) parser.parse(new FileReader(CACHE));
                if (obj == null) cache = new JSONObject();
                else cache = obj;
            } catch (IOException | ParseException e) {
                cache = new JSONObject();
            }
        }
        return cache;
    }

    private static void storeCacheToFile(JSONObject jsonObject) {
        try {
            FileWriter fileWriter = new FileWriter(CACHE);
            fileWriter.write(jsonObject.toJSONString());
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public static void writeCache(String key, Object value) {
        JSONObject object = readAllCache();
        object.put(key, value);
        storeCacheToFile(object);
    }

    public static boolean readBoolean(String key) {
        JSONObject object = readAllCache();
        Object obj = object.get(key);
        if (obj instanceof Boolean) {
            return (boolean) obj;
        } else if (obj instanceof String) {
            return Boolean.parseBoolean((String) obj);
        } else return false;
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
     * Returns the last opened directory from the pref file.
     *
     * @return the directory of last file selection.
     */
    public static File readLastSelectedDir() {
        JSONObject object = readAllCache();
        Object curDir = object.get("dir");
        if (curDir == null) {
            return null;
        } else if (curDir instanceof String) {
            File f = new File((String) curDir);
            if (f.exists()) return f;
            else return null;
        } else {
            return null;
        }
    }

    /**
     * Writes the directory of the last opened file to the pref file.
     *
     * @param lastDir the directory of the last opened file.
     */
    public static void writeLastSelectionDir(File lastDir) {
        String path = lastDir.getParentFile().getAbsolutePath();
        writeCache("dir", path);
    }

    @SuppressWarnings("unchecked")
    public static void saveOpeningDirs(List<String> opening) {
        JSONObject obj = readAllCache();
        obj.put("opening", opening);
        storeCacheToFile(obj);
    }

    @SuppressWarnings("unchecked")
    public static List<String> getOpeningDirs() {
        JSONObject obj = readAllCache();
        JSONArray opening = (JSONArray) obj.get("opening");
        if (opening == null) {
            return new ArrayList<>();
        } else {
            return opening;
        }
    }

    /**
     * Writes the last opened directory to the cache.
     *
     * @param lastDir the last opened directory.
     */
    public static void writeLastDir(File lastDir) {
        if (lastDir == null) {
            try {
                deleteCache("dir");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            String path = lastDir.getAbsolutePath();
            writeCache("dir", path);
        }
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

    private static void deleteCache(String item) throws IOException {
        JSONObject object = readAllCache();
        object.remove(item);
        FileWriter fileWriter = new FileWriter(CACHE);
        fileWriter.write(object.toJSONString());
        fileWriter.flush();
        fileWriter.close();
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
     * Returns a {@code List} of existing {@code File}'s that was opened for annotation.
     *
     * @return a {@code List} of existing {@code File}'s that was opened for annotation.
     */
    public static List<File> getHistoryAnnotation() {
        JSONObject object = readAllCache();
        ArrayList<File> files = new ArrayList<>();
        Object array = object.get("annotations");
        if (array instanceof JSONArray) {
            for (Object obj : (JSONArray) array) {
                if (obj instanceof String) {
                    File f = new File((String) obj);
                    if (f.exists()) files.add(f);
                }
            }
        }
        return files;
    }

    /**
     * Records a new {@code File} as a new annotation file to the pref file.
     * <p>
     * If there exists more than 9 records, the oldest record will be removed.
     *
     * @param file the new annotation file
     */
    @SuppressWarnings("unchecked")
    public static void addHistoryAnnotation(File file) {
        JSONObject object = readAllCache();
        Object sub = object.get("annotations");
        if (sub instanceof JSONArray) {
            JSONArray array = (JSONArray) sub;
            String absPath = file.getAbsolutePath();
            removeDuplicate(array, absPath);
            array.add(0, absPath);
            while (array.size() > 9) {
                array.remove(array.size() - 1);
            }
            writeCache("annotations", array);
        } else {
            JSONArray array = new JSONArray();
            String absPath = file.getAbsolutePath();
            array.add(absPath);
            writeCache("annotations", array);
        }
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

    /**
     * Removes the duplicate elements in a {@code JSONArray}.
     *
     * @param files the input {@code JSONArray}
     * @param file  the {@code String} that to be searched in <code>files</code>.
     *              All elements in <code>files</code> that equals <code>file</code> will be removed
     */
    @SuppressWarnings("unchecked")
    private static void removeDuplicate(JSONArray files, String file) {
        files.removeIf(file1 -> file1.equals(file));
    }

    private static String readFileToString(String fileName) throws IOException {
        StringBuilder builder = new StringBuilder();
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        String line;
        while ((line = br.readLine()) != null) {
            builder.append(line).append('\n');
        }
        br.close();
        return builder.toString();
    }
}
