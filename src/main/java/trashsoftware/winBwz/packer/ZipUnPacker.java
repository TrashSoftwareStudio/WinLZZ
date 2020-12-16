package trashsoftware.winBwz.packer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class ZipUnPacker extends UnPacker {

    private int dirCount;
    private int fileCount;
    private ZipInputStream zis;
    private ZipCatalogNode root;

    public ZipUnPacker(String packName, ResourceBundle resourceBundle) {
        super(packName, resourceBundle);
    }

    @Override
    public void readInfo() throws IOException {
        ZipFile zipFile = new ZipFile(packName);

        zis = new ZipInputStream(new FileInputStream(packName));
    }

    @Override
    public void readFileStructure() throws Exception {
        traverseFileStructure(zis);
    }

    private void traverseFileStructure(ZipInputStream zis) throws IOException {
        ZipEntry zipEntry;
        while ((zipEntry = zis.getNextEntry()) != null) {
            String[] paths = zipEntry.getName().split(Pattern.quote(File.separator));
            System.out.println(Arrays.toString(paths));
            createCatalogIfNone(paths);
            zis.closeEntry();
        }
    }

    private void createCatalogIfNone(String[] paths) {
        if (root == null) {
            root = new ZipCatalogNode(paths[0], true);
        }
        ZipCatalogNode active = root;
        if (paths.length == 1) {
            active.addChild(new ZipCatalogNode(paths[0], false));
            return;
        }
        StringBuilder joinedPath = new StringBuilder(paths[0]);
        for (int i = 1; i < paths.length - 1; i++) {
            joinedPath.append(File.separator).append(paths[i]);
            String jp = joinedPath.toString();
            boolean found = false;
            for (CatalogNode cn : active.getChildren()) {
                if (cn.getPath().equals(jp)) {
                    active = (ZipCatalogNode) cn;
                    found = true;
                    break;
                }
            }
            if (!found) {
                ZipCatalogNode zcn = new ZipCatalogNode(jp, true);
                active.addChild(zcn);
                active = zcn;
                dirCount++;
            }
        }
        String fileName = joinedPath.append(File.separator).append(paths[paths.length - 1]).toString();
        active.addChild(new ZipCatalogNode(fileName, false));
        fileCount++;
    }

    @Override
    public boolean testPack() {
        return false;
    }

    @Override
    public void unCompressAll(String targetDir) throws Exception {

    }

    @Override
    public void unCompressFrom(String targetDir, CatalogNode node) throws Exception {

    }

    @Override
    public String getAlg() {
        return null;
    }

    @Override
    public String getAnnotation() {
        return null;
    }

    @Override
    public ZipCatalogNode getRootNode() {
        return root;
    }

    @Override
    public void interrupt() {

    }

    @Override
    public long getTotalOrigSize() {
        return 0;
    }

    @Override
    public long getDisplayArchiveLength() {
        return 0;
    }

    @Override
    public boolean isSeparated() {
        return false;
    }

    @Override
    public long getCreationTime() {
        return 0;
    }

    @Override
    public int getFileCount() {
        return fileCount;
    }

    @Override
    public int getDirCount() {
        return dirCount;
    }

    @Override
    public void setThreads(int threads) {

    }

    @Override
    public void setPassword(String password) throws Exception {

    }

    @Override
    public boolean isPasswordSet() {
        return true;
    }

    @Override
    public int getEncryptLevel() {
        return 0;
    }

    @Override
    public void close() throws IOException {
        zis.close();
    }
}
