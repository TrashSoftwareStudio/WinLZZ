package ResourcesPack.ConfigLoader;

import java.io.*;
import java.util.HashMap;

public abstract class GeneralLoaders {

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

    public static String getConfig(String item) {
        HashMap<String, String> configs = readAllConfigs();
        if (configs != null) return configs.get(item);
        else return null;
    }

    public static File readLastDir() {
        String dir = getConfig("dir");
        if (dir == null) return null;
        else {
            File f = new File(dir);
            if (f.exists()) return f;
            else return null;
        }
    }

    public static void writeLastDir(File lastDir) throws IOException {
        String path = lastDir.getParentFile().getAbsolutePath();
        writeConfig("dir", path);
    }

    public static void writeConfig(String item, String config) throws IOException {
        HashMap<String, String> configs = readAllConfigs();
        if (configs == null) configs = new HashMap<>();
        configs.put(item, config);
        BufferedWriter out = new BufferedWriter(new FileWriter("pref.ini"));
        for (String key : configs.keySet()) {
            out.write(key + "=" + configs.get(key));
            out.write('\n');
        }
        out.flush();
        out.close();
    }
}
