package trashsoftware.winBwz.packer.zip;

import trashsoftware.winBwz.core.Constants;
import trashsoftware.winBwz.packer.CatalogNode;
import trashsoftware.winBwz.packer.UnPacker;
import trashsoftware.winBwz.utility.LengthOutputStream;
import trashsoftware.winBwz.utility.Security;
import trashsoftware.winBwz.utility.Util;

import java.io.*;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class ZipUnPacker extends UnPacker {

    private final String testTempName;
    private final byte[] buffer = new byte[ZipPacker.bufferSize];
    private int dirCount;
    private int fileCount;
    private long totalOrigSize;
    private long compressedSize;
    private long netCompressedSize;
    private long creationTime;
    private String annotation;
    private long passedLengthBytes;
    private ZipFile zipFile;
    private ZipCatalogNode root;

    public ZipUnPacker(String packName, ResourceBundle resourceBundle) {
        super(packName, resourceBundle);

        testTempName = packName + ".zt";
    }

    @Override
    public void readInfo() throws IOException {
        zipFile = new ZipFile(packName);
        compressedSize = new File(packName).length();
        annotation = zipFile.getComment();

        ZipInputStream zis = new ZipInputStream(new FileInputStream(packName));
        ZipEntry entry = zis.getNextEntry();
        if (entry != null) {
            creationTime = entry.getTime();
        }
        zis.closeEntry();
        zis.close();
    }

    @Override
    public void readFileStructure() throws Exception {
        traverseFileStructure();
    }

    private void traverseFileStructure() throws IOException {
        root = new ZipCatalogNode("", null);
        ZipInputStream zis = new ZipInputStream(new FileInputStream(packName));
        ZipEntry zipEntry;
        while ((zipEntry = zis.getNextEntry()) != null) {
            createCatalogIfNone(zipEntry);
            zis.closeEntry();
            totalOrigSize += zipEntry.getSize();
            netCompressedSize += zipEntry.getCompressedSize();
        }
        zis.close();
    }

    private void createCatalogIfNone(ZipEntry zipEntry) {
        String[] paths = zipEntry.getName().split(Pattern.quote(File.separator));
        ZipCatalogNode active = root;
        StringBuilder joinedPath = new StringBuilder();
        for (int i = 0; i < paths.length - 1; i++) {
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
                ZipCatalogNode zcn = new ZipCatalogNode(jp, active);
                active.addChild(zcn);
                active = zcn;
                dirCount++;
            }
        }
        String fileName = joinedPath.append(File.separator).append(paths[paths.length - 1]).toString();
        active.addChild(new ZipCatalogNode(fileName, active, zipEntry));
        if (zipEntry.isDirectory()) dirCount++;
        else fileCount++;
    }

    @Override
    public boolean testPack() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new ZupTimerTask(), 0, 1000 / Constants.GUI_UPDATES_PER_S);
        try {
            totalProgress.set(totalOrigSize);
            progress.set(0);
            passedLengthBytes = 0;
            return testEntry(root);
        } catch (IOException e) {
            return false;
        } finally {
            timer.cancel();
        }
    }

    private boolean testEntry(ZipCatalogNode zcn) throws IOException {
        if (zcn.isDir()) {
            for (CatalogNode child : zcn.getChildren()) {
                if (!testEntry((ZipCatalogNode) child)) return false;
            }
            return true;
        } else {
            ZipEntry entry = zcn.getEntry();
            long crc = entry.getCrc();
            unCompressSingle(entry, testTempName);
            long realCrc = Security.generateCRC32(testTempName);
            Util.deleteFile(testTempName);
            return crc == realCrc;
        }
    }

    private void unCompressSingle(ZipEntry entry, String targetFile) throws IOException {
        LengthOutputStream fos = new LengthOutputStream(new FileOutputStream(targetFile));
//        long lastProgress = progress.get();
        long lastProgress = passedLengthBytes;

        InputStream is = zipFile.getInputStream(entry);
        int read;
        while ((read = is.read(buffer)) > 0) {
            fos.write(buffer, 0, read);
            passedLengthBytes = lastProgress + fos.getWrittenLength();
//            progress.set(lastProgress + fos.getWrittenLength());
        }
        is.close();

        fos.flush();
        fos.close();
        passedLengthBytes = lastProgress + fos.getWrittenLength();
    }

    @Override
    public void unCompressAll(String targetDir) throws Exception {
        for (CatalogNode cn : root.getChildren()) unCompressFrom(targetDir, cn);
    }

    @Override
    public void unCompressFrom(String targetDir, CatalogNode node) throws Exception {
        String dirOffset;
        if (!node.getPath().contains(File.separator)) dirOffset = "";
        else dirOffset = node.getPath().substring(0, node.getPath().lastIndexOf(File.separator));
        step.setValue(bundle.getString("uncIng"));
        totalProgress.set(origSizeFrom((ZipCatalogNode) node));
        progress.set(0);
        passedLengthBytes = 0;
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new ZupTimerTask(), 0, 1000 / Constants.GUI_UPDATES_PER_S);
        try {
            traversalExtract(targetDir, (ZipCatalogNode) node, dirOffset);
        } finally {
            timer.cancel();
        }
    }

    private long origSizeFrom(ZipCatalogNode node) {
        if (node.isDir()) {
            long total = 0;
            for (CatalogNode cn : node.getChildren()) {
                total += origSizeFrom((ZipCatalogNode) cn);
            }
            return total;
        } else {
            return node.getSize();
        }
    }

    private void traversalExtract(String targetDir, ZipCatalogNode cn, String dirOffset) throws IOException {
        String path = targetDir + File.separator + cn.getPath().substring(dirOffset.length());
        File f = new File(path);
        if (cn.isDir()) {
            if (!f.exists()) {
                if (!f.mkdirs()) System.out.println("Failed to create directory " + f.getAbsolutePath());
            }
            for (CatalogNode scn : cn.getChildren())
                traversalExtract(targetDir, (ZipCatalogNode) scn, dirOffset);
        } else {
            currentFile.setValue(cn.getPath().substring(1));
            unCompressSingle(cn.getEntry(), path);
        }
    }

    @Override
    public String getAlg() {
        return "deflate";
    }

    @Override
    public String getAnnotation() {
        return annotation;
    }

    @Override
    public ZipCatalogNode getRootNode() {
        return root;
    }

    @Override
    public void interrupt() {

    }

    /**
     * Get the total file size after compression, except the zip header.
     *
     * @return the total file size after compression without the zip header
     */
    public long getNetCompressedSize() {
        return netCompressedSize;
    }

    @Override
    public long getTotalOrigSize() {
        return totalOrigSize;
    }

    @Override
    public long getDisplayArchiveLength() {
        return compressedSize;
    }

    @Override
    public boolean isSeparated() {
        return false;
    }

    @Override
    public long getCreationTime() {
        return creationTime;
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
        if (zipFile != null)
            zipFile.close();
    }

    class ZupTimerTask extends TimerTask {
        private int accumulator;
        private long lastUpdateProgress;

        @Override
        public void run() {
            progress.set(passedLengthBytes);
//            long position = progress.get();
            accumulator++;
            if (accumulator % Constants.GUI_UPDATES_PER_S == 0) {
                double finished = ((double) passedLengthBytes) / totalProgress.get();
                double rounded = (double) Math.round(finished * 1000) / 10;
                percentage.set(String.valueOf(rounded));
                int newUpdated = (int) (passedLengthBytes - lastUpdateProgress);
                lastUpdateProgress = passedLengthBytes;
                int ratioInt = newUpdated / 1024;
                ratio.set(String.valueOf(ratioInt));

                long timeUsedV = accumulator * 1000L / Constants.GUI_UPDATES_PER_S;
                timeUsed.set(Util.secondToString(timeUsedV / 1000));
                long expectTime = (totalProgress.get() - passedLengthBytes) / ratioInt / 1024;
                timeExpected.set(Util.secondToString(expectTime));

                passedLength.set(Util.sizeToReadable(passedLengthBytes));
//                cmpLength.set(Util.sizeToReadable(compressedLength));
            }
        }
    }
}
