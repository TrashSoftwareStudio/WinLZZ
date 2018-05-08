package WinLzz.GraphicUtil;

import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.ReadOnlyLongWrapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class InfoNode {

    long size;
    int dirCount;
    int fileCount;
    ReadOnlyLongWrapper sizeWrapper = new ReadOnlyLongWrapper();
    ReadOnlyIntegerWrapper dirCountWrapper = new ReadOnlyIntegerWrapper();
    ReadOnlyIntegerWrapper fileCountWrapper = new ReadOnlyIntegerWrapper();
    private File file;
    private File[] files;
    private long creationTime, modifiedTime, accessTime;
    boolean isRunning = true;

    /**
     * Constructor for single file.
     *
     * @param file the file.
     */
    public InfoNode(File file) {
        this.file = file;
    }

    /**
     * Constructor for multiple files;
     *
     * @param files file array
     */
    public InfoNode(File[] files) {
        this.files = files;
    }

    public void readInfo() throws IOException {
        if (isSingle()) {
            BasicFileAttributes bfa = Files.readAttributes(Paths.get(this.file.getAbsolutePath()), BasicFileAttributes.class);
            creationTime = bfa.creationTime().toMillis();
            modifiedTime = bfa.lastModifiedTime().toMillis();
            accessTime = bfa.lastAccessTime().toMillis();
        }
    }

    public void readDirs() {
        Thread timer = new Thread(new Timer(this));
        timer.start();
        if (isSingle()) traverse(file);
        else for (File f : files) traverse(f);
        isRunning = false;
    }

    private void traverse(File f) {
        if (f.isDirectory()) {
            dirCount += 1;
            File[] children = f.listFiles();
            if (children != null) for (File child : children) traverse(child);
        } else {
            fileCount += 1;
            size += f.length();
        }
    }

    public boolean isSingle() {
        return file != null;
    }

    public String getType() {
        if (isSingle()) {
            if (file.isDirectory()) return null;
            else {
                String oName = file.getName();
                if (oName.contains(".")) return (oName.substring(oName.lastIndexOf(".") + 1) + " ").toUpperCase();
                else return "";
            }
        } else {
            return null;
        }
    }

    public File getFile() {
        if (isSingle()) return file;
        else return files[0];
    }

    public long getSize() {
        return size;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public long getModifiedTime() {
        return modifiedTime;
    }

    public long getAccessTime() {
        return accessTime;
    }

    public void initializeProperties() {
        dirCountWrapper.set(1);
        fileCountWrapper.set(1);
        sizeWrapper.set(1);
        dirCountWrapper.set(0);
        fileCountWrapper.set(0);
        sizeWrapper.set(0);
    }

    public ReadOnlyIntegerProperty dirCountProperty() {
        return dirCountWrapper;
    }

    public ReadOnlyIntegerProperty fileCountProperty() {
        return fileCountWrapper;
    }

    public ReadOnlyLongProperty sizeProperty() {
        return sizeWrapper;
    }
}


class Timer implements Runnable {

    private InfoNode parent;

    Timer(InfoNode parent) {
        this.parent = parent;
    }

    @Override
    public void run() {
        while (parent.isRunning) {
            parent.sizeWrapper.set(parent.size);
            parent.fileCountWrapper.set(parent.fileCount);
            parent.dirCountWrapper.set(parent.dirCount);

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        parent.sizeWrapper.set(parent.size);
        parent.fileCountWrapper.set(parent.fileCount);
        parent.dirCountWrapper.set(parent.dirCount);
    }
}
