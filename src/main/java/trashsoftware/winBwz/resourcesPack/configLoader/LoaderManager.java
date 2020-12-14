package trashsoftware.winBwz.resourcesPack.configLoader;

public class LoaderManager {

    private static CacheSaver cacheSaver;

    public static void startCacheSaver() {
        if (cacheSaver != null) {
            cacheSaver.stop();
        }
        cacheSaver = new CacheSaver();
    }

    public static void stopCacheSaver() {
        if (cacheSaver != null) {
            cacheSaver.store();
            cacheSaver.stop();
        }
    }

    public static CacheSaver getCacheSaver() {
        return cacheSaver;
    }
}
