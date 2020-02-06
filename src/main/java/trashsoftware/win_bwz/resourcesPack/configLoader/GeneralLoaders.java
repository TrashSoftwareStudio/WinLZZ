package trashsoftware.win_bwz.resourcesPack.configLoader;

import java.io.*;
import java.util.*;

/**
 * Common config or resources loaders.
 *
 * @author zbh
 * @since 0.6
 */
public abstract class GeneralLoaders {

    public static Locale getCurrentLocale() {
        String localeName = getConfig("locale");
        if (localeName == null) {
            return new Locale("zh", "CN");
        } else {
            String[] lanCountry = localeName.split("_");
            return new Locale(lanCountry[0], lanCountry[1]);
        }
    }

    public static List<NamedLocale> getAllLocales() {
        List<NamedLocale> locales = new ArrayList<>();
        ResourceBundle resourceBundle = ResourceBundle.getBundle("trashsoftware.deepSearcher2.bundles.Languages");
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
        File pref = new File("pref.ini");
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
     * Returns the last opened directory from the pref file.
     *
     * @return the last opened directory.
     */
    public static File readLastDir() {
        String dir = getConfig("dir");
        if (dir == null) return null;
        else {
            File f = new File(dir);
            if (f.exists()) return f;
            else return null;
        }
    }

    /**
     * Writes the directory of the last opened file to the pref file.
     *
     * @param lastDir the directory of the last opened file.
     */
    public static void writeLastSelectionDir(File lastDir) {
        String path = lastDir.getParentFile().getAbsolutePath();
        writeConfig("dir", path);
    }

    /**
     * Writes the last opened directory to the pref file.
     *
     * @param lastDir the last opened directory.
     */
    public static void writeLastDir(File lastDir) {
        if (lastDir == null) {
            try {
                deleteConfig("dir");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            String path = lastDir.getAbsolutePath();
            writeConfig("dir", path);
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
            BufferedWriter out = new BufferedWriter(new FileWriter("pref.ini"));
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

    @SuppressWarnings("all")
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
        String last = getConfig("annotations");
        ArrayList<File> files = new ArrayList<>();
        if (last != null) {
            String[] annotations;
            if (last.contains("?")) annotations = last.split("[?]");
            else annotations = new String[]{last};

            for (String s : annotations) {
                File f = new File(s);
                if (f.exists()) files.add(f);
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
    public static void addHistoryAnnotation(File file) {
        List<File> files = getHistoryAnnotation();
        removeDuplicate(files, file);

        StringBuilder builder = new StringBuilder();
        builder.append(file.getAbsolutePath());
        if (files.size() > 9) files.remove(files.size() - 1);
        for (File f : files) builder.append("?").append(f.getAbsolutePath());
        writeConfig("annotations", builder.toString());

    }

    /**
     * Removes the duplicate elements in a {@code List<File>}.
     *
     * @param files the input {@code List<File>}
     * @param file  the {@code File} that to be searched in <code>files</code>.
     *              All elements in <code>files</code> that equals <code>file</code> will be removed
     */
    private static void removeDuplicate(List<File> files, File file) {
        Iterator<File> iterator = files.iterator();
        while (iterator.hasNext()) if (iterator.next().equals(file)) iterator.remove();
    }
}
