package trashsoftware.win_bwz.gui.graphicUtil;

import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.ReadOnlyLongWrapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * A node {@code Object} represents the starting directory(s), or file(s).
 * <p>
 * This class is used to show the properties of files.
 *
 * @author zbh
 * @since 0.7.0
 */
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
     * Creates a new {@code InfoNode} instance.
     * <p>
     * Constructor for single file.
     *
     * @param file the file.
     */
    public InfoNode(File file) {
        this.file = file;
    }

    /**
     * Creates a new {@code InfoNode} instance.
     * <p>
     * Constructor for multiple files;
     *
     * @param files file array
     */
    public InfoNode(File[] files) {
        this.files = files;
    }

    /**
     * Reads attributes from the file.
     *
     * @throws IOException if the file is not readable
     */
    public void readInfo() throws IOException {
        if (isSingle()) {
            BasicFileAttributes bfa = Files.readAttributes(Paths.get(this.file.getAbsolutePath()), BasicFileAttributes.class);
            creationTime = bfa.creationTime().toMillis();
            modifiedTime = bfa.lastModifiedTime().toMillis();
            accessTime = bfa.lastAccessTime().toMillis();
        }
    }

    /**
     * Reads all the files and directories under the {@code file}, if the {@code file} is a directory.
     */
    public void readDirs() {
        Thread timer = new Thread(new Timer(this));
        timer.start();
        if (isSingle()) traverse(file);
        else for (File f : files) traverse(f);
        isRunning = false;
    }

    public void interrupt() {
        isRunning = false;
    }

    private void traverse(File f) {
        if (!isRunning) return;
        if (f.isDirectory()) {
            dirCount += 1;
            File[] children = f.listFiles();
            if (children != null) for (File child : children) traverse(child);
        } else {
            fileCount += 1;
            size += f.length();
        }
    }

    /**
     * Returns whether this {@code InfoNode} represents a single file.
     *
     * @return {@code true} if this {@code InfoNode} represents a single file,
     * {@code false} if this {@code InfoNode} represents multiple files
     */
    public boolean isSingle() {
        return file != null;
    }

    /**
     * Returns the type of the {@code file} if this {@code InfoNode} represents a single file.
     *
     * @return the type of the {@code file} if this {@code InfoNode} represents a single file
     */
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

    /**
     * Returns the file.
     *
     * @return the file
     */
    public File getFile() {
        if (isSingle()) return file;
        else return files[0];
    }

    /**
     * Returns the size of all files under this {@code file} if this {@code InfoNode} represents directory(s) or
     * multiple files, or the size of this {@code file} if this {@code InfoNode} represents a file.
     *
     * @return the size of all files under this {@code file} if this {@code InfoNode} represents directory(s) or
     * multiple files, or the size of this {@code file} if this {@code InfoNode} represents a file.
     */
    public long getSize() {
        return size;
    }

    /**
     * Returns the time of creation this {@code file}.
     *
     * @return the time of creation this {@code file}
     */
    public long getCreationTime() {
        return creationTime;
    }

    /**
     * Returns the last modified time of this {@code file}.
     *
     * @return the last modified time of this {@code file}
     */
    public long getModifiedTime() {
        return modifiedTime;
    }

    /**
     * Returns the last accessed time of this {@code file}.
     *
     * @return the last accessed time of this {@code file}
     */
    public long getAccessTime() {
        return accessTime;
    }

    /**
     * Initializes all properties used for updating GUI.
     */
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


/**
 * An implementation of {@code Runnable}, used to update status of a {@code InfoNode} instance to a
 * {@code FilePropertyUI} instance every 1 second.
 *
 * @author zbh
 * @see java.lang.Runnable
 * @since 0.7.0
 */
class Timer implements Runnable {

    private InfoNode parent;

    /**
     * Creates a new {@code Timer} instance.
     *
     * @param parent the {@code Timer} which created this {@code Timer}.
     */
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
