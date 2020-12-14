package trashsoftware.winBwz.resourcesPack.configLoader;

import org.json.JSONException;
import org.json.JSONObject;
import trashsoftware.winBwz.resourcesPack.EventLogger;

import java.io.*;
import java.util.*;

public class CacheSaver {

    public static final int AUTO_SAVE_INTERVAL = 3000;

    private static final String CACHE = GeneralLoaders.CACHE_DIR + File.separator + "cache.json";
    //    private static final String OPENED_DIRS_CACHE = CACHE_DIR + File.separator + "dirs.json";

    private final Map<String, Object> map;
    private final Timer timer;

    CacheSaver() {
        map = load();
        timer = new Timer("Cache saver thread");
        // skips the first interval, because all data are just loaded from file
        timer.schedule(new AutoSaveTask(), AUTO_SAVE_INTERVAL, AUTO_SAVE_INTERVAL);
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

    /**
     * Removes the duplicate elements in a {@code JSONArray}.
     *
     * @param files the input {@code JSONArray}
     * @param file  the {@code String} that to be searched in <code>files</code>.
     *              All elements in <code>files</code> that equals <code>file</code> will be removed
     */
    private static void removeDuplicate(List<Object> files, String file) {
        files.removeIf(file1 -> file1.equals(file));
    }

    public void writeCache(String key, Object value) {
        map.put(key, value);
    }

    public void deleteCache(String key) {
        map.remove(key);
    }

    public List<String> getOpeningDirs() {
        Object obj = map.get("opening");
        if (obj instanceof List) {
            return (List<String>) obj;
        } else return new ArrayList<>();
    }

    public void saveOpeningDirs(List<String> dirs) {
        map.put("opening", dirs);
    }

    /**
     * Returns the last opened directory from the pref file.
     *
     * @return the directory of last file selection.
     */
    public File readLastSelectedDir() {
        Object curDir = map.get("dir");
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
    public void writeLastSelectionDir(File lastDir) {
        String path = lastDir.getParentFile().getAbsolutePath();
        map.put("dir", path);
    }

    public boolean readBoolean(String key) {
        Object obj = map.get(key);
        if (obj instanceof Boolean) return (boolean) obj;
        else if (obj instanceof String) return Boolean.parseBoolean((String) obj);
        else return false;
    }

    public long readLong(String key, long defaultValue) {
        Object obj = map.get(key);
        if (obj instanceof Long) {
            return (long) obj;
        } else if (obj instanceof Integer) {
            return (int) obj;
        } else if (obj instanceof String) {
            try {
                return Long.parseLong((String) obj);
            } catch (NumberFormatException nfe) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public int readInt(String key, int defaultValue) {
        return (int) readLong(key, defaultValue);
    }

    /**
     * Returns a {@code List} of existing {@code File}'s that was opened for annotation.
     *
     * @return a {@code List} of existing {@code File}'s that was opened for annotation.
     */
    @SuppressWarnings("unchecked")
    public List<File> getHistoryAnnotation() {
        ArrayList<File> files = new ArrayList<>();
        Object array = map.get("annotations");
        if (array instanceof List) {
            for (Object obj : (List<Object>) array) {
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
    public void addHistoryAnnotation(File file) {
        Object sub = map.get("annotations");
        if (sub instanceof List) {
            List<Object> array = (List<Object>) sub;
            String absPath = file.getAbsolutePath();
            removeDuplicate(array, absPath);
            array.add(0, absPath);
            while (array.size() > 9) {
                array.remove(array.size() - 1);
            }
            map.put("annotations", array);
        } else {
            List<String> array = new ArrayList<>();
            String absPath = file.getAbsolutePath();
            array.add(absPath);
            map.put("annotations", array);
        }
    }

    public void stop() {
        timer.cancel();
    }

    private Map<String, Object> load() {
        try {
            String content = readFileToString(CACHE);
            JSONObject root = new JSONObject(content);
            return root.toMap();
        } catch (IOException | JSONException e) {
            return new HashMap<>();
        }
    }

    void store() {
        JSONObject root = new JSONObject(map);
        String content = root.toString(2);
        try {
            FileWriter fileWriter = new FileWriter(CACHE);
            fileWriter.write(content);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            EventLogger.log(e);
        }
    }

    class AutoSaveTask extends TimerTask {
        @Override
        public void run() {
            store();
        }
    }
}
