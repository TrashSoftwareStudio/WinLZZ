package trashsoftware.winBwz.packer;

import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import trashsoftware.winBwz.gui.graphicUtil.AnnotationNode;

import java.io.*;
import java.util.ResourceBundle;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipPacker extends Packer {

    private static final int bufferSize = 8192;
    private int level = 6;
    private boolean running = true;
    private byte[] buffer = new byte[bufferSize];

    /**
     * Length processed, not compressed.
     *
     * When the compression finish, this should be equal to {@code totalOrigLength}
     */
    private long passedLength;

    public ZipPacker(File[] inFiles) {
        super(inFiles);
    }

    @Override
    public void interrupt() {
        running = false;
    }

    @Override
    public void setCmpLevel(int level) {
        this.level = level;
    }

    @Override
    public void setAlgorithm(String algCode) {

    }

    @Override
    public void setThreads(int threads) {

    }

    @Override
    public void setPartSize(long partSize) {

    }

    @Override
    public void setEncrypt(String password, int encryptLevel, String encAlg, String passAlg) {

    }

    @Override
    public void setAnnotation(AnnotationNode annotation) throws IOException {

    }

    @Override
    public void build() {

    }

    @Override
    public void pack(String outFileName, int windowSize, int bufferSize) throws Exception {
        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(outFileName));
        zipOutputStream.setLevel(level);

        compress(zipOutputStream, inFiles[0], inFiles[0].getName());

        zipOutputStream.flush();
        zipOutputStream.close();
    }

    private void compress(ZipOutputStream zipOut, File file, String baseName) throws IOException {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                if (children.length == 0) {
                    zipOut.putNextEntry(new ZipEntry(baseName + File.separator));
                    zipOut.closeEntry();
                } else {
                    for (File file1 : children) {
                        compress(zipOut, file1, baseName + File.separator + file1.getName());
                    }
                }
            }
        } else {
            zipOut.putNextEntry(new ZipEntry(baseName));
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));

            int read;
            while ((read = bis.read(buffer)) > 0) {
                zipOut.write(buffer, 0, read);
                passedLength += read;
            }

            bis.close();
            zipOut.closeEntry();
        }
    }
}
