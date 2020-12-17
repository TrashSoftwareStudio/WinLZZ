package trashsoftware.winBwz.packer;

import trashsoftware.winBwz.core.Constants;
import trashsoftware.winBwz.gui.graphicUtil.AnnotationNode;
import trashsoftware.winBwz.utility.LengthOutputStream;
import trashsoftware.winBwz.utility.Util;

import java.io.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipPacker extends Packer {

    static final int bufferSize = 8192;
    private int level = 6;
    private boolean interrupted = false;
    private final byte[] buffer = new byte[bufferSize];

    private final long startTime;
    private long timeOffset;
    private String annotation;

    private FileNode[] roots;

    /**
     * Length processed, not compressed.
     * <p>
     * When the compression finish, this should be equal to {@code totalOrigLength}
     */
    private long passedLengthBytes;

    public ZipPacker(File[] inFiles) {
        super(inFiles);

        startTime = System.currentTimeMillis();
    }

    @Override
    public void interrupt() {
        interrupted = true;
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
        this.annotation = new String(annotation.getAnnotation());
    }

    @Override
    public void build() {
        roots = new FileNode[inFiles.length];
        for (int i = 0 ; i < inFiles.length; i++) {
            roots[i] = traverseFile(inFiles[i]);
        }
        totalOrigLengthWrapper.set(totalLength);
    }

    @Override
    public void pack(String outFileName, int windowSize, int bufferSize) throws Exception {
        if (interrupted) return;
        timeOffset = System.currentTimeMillis() - startTime;
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new ZipTimerTask(), 0, 1000 / Constants.LZZ_GUI_UPDATES_PER_S);

        LengthOutputStream los = new LengthOutputStream(new FileOutputStream(outFileName));
        ZipOutputStream zipOutputStream = new ZipOutputStream(los);
        try {
            zipOutputStream.setComment(annotation);
            zipOutputStream.setLevel(level);

            step.set(bundle.getString("compressing"));
            for (FileNode root : roots) {
                compress(zipOutputStream, los, root, root.file.getName());
            }

        } finally {
            timer.cancel();
            zipOutputStream.flush();
            zipOutputStream.close();
        }
    }

    private FileNode traverseFile(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            FileNode[] res = null;
            if (children != null) {
                res = new FileNode[children.length];
                for (int i = 0 ; i < children.length; i++) {
                    res[i] = traverseFile(children[i]);
                }
            }
            return new FileNode(file, res);
        } else {
            totalLength += file.length();
            return new FileNode(file);
        }
    }

    private void compress(ZipOutputStream zipOut, LengthOutputStream zipOutBase,
                          FileNode file, String baseName) throws IOException {
        if (interrupted) return;
        if (file.isDir) {
            FileNode[] children = file.children;
            if (children != null) {
                if (children.length == 0) {
                    zipOut.putNextEntry(new ZipEntry(baseName + File.separator));
                    zipOut.closeEntry();
                } else {
                    for (FileNode file1 : children) {
                        compress(zipOut, zipOutBase, file1, baseName + File.separator + file1.file.getName());
                    }
                }
            }
        } else {
            this.file.setValue(String.format("%s\\\n%s", file.file.getParent(), file.file.getName()));
            ZipEntry entry = new ZipEntry(baseName);
            zipOut.putNextEntry(entry);
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file.file));

            int read;
            while ((read = bis.read(buffer)) > 0 && !interrupted) {
                zipOut.write(buffer, 0, read);
                passedLengthBytes += read;
                compressedLength = zipOutBase.getWrittenLength();
            }

            bis.close();
            zipOut.closeEntry();
        }
    }

    class ZipTimerTask extends TimerTask {
        private int accumulator;
        private long lastUpdateProgress;

        @Override
        public void run() {
            progress.set(passedLengthBytes);
            accumulator++;
            if (accumulator % Constants.LZZ_GUI_UPDATES_PER_S == 0) {
                double finished = ((double) passedLengthBytes) / totalLength;
                double rounded = (double) Math.round(finished * 1000) / 10;
                percentage.set(String.valueOf(rounded));
                int newUpdated = (int) (passedLengthBytes - lastUpdateProgress);
                lastUpdateProgress = passedLengthBytes;
                int ratioInt = newUpdated / 1024;
                ratio.set(String.valueOf(ratioInt));

                long timeUsedV = accumulator * 1000L / Constants.LZZ_GUI_UPDATES_PER_S;
                timeUsed.set(Util.secondToString((timeUsedV + timeOffset) / 1000));
                long expectTime = (totalLength - passedLengthBytes) / ratioInt / 1024;
                timeExpected.set(Util.secondToString(expectTime));

                passedLength.set(Util.sizeToReadable(passedLengthBytes));
                cmpLength.set(Util.sizeToReadable(compressedLength));
            }
        }
    }

    private static class FileNode {
        private final File file;
        private final boolean isDir;
        private final FileNode[] children;

        FileNode(File file, FileNode[] children) {
            this.file = file;
            this.isDir = true;
            this.children = children;
        }

        FileNode(File file) {
            this.file = file;
            this.isDir = false;
            this.children = null;
        }
    }
}
