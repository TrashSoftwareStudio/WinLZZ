package trashsoftware.winBwz.utility;

import trashsoftware.winBwz.packer.UnPacker;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.ResourceBundle;

/**
 * An input stream that reads multiple files as one whole file.
 *
 * @author zbh
 * @see trashsoftware.winBwz.utility.MultipleInputStream
 * @since 0.8
 */
public class SeparateInputStream extends MultipleInputStream {

    private UnPacker unPacker;

    private ResourceBundle bundle;

    private int signature;

    private boolean hasSignature;

    private boolean sectionSet;

    private String expectedName;

    /**
     * Constructor of a new <code>SeparateInputStream</code> instance.
     *
     * @param files     queue of files
     * @param parent    the parent <code>UnPacker</code> instance, which launches this
     *                  <code>SeparateInputStream</code> instance
     * @param buffered  whether to use a buffered reader or not
     * @param signature the signature at the beginning of every part file
     * @throws IOException if any IO error occurs
     */
    private SeparateInputStream(Deque<File> files, UnPacker parent, boolean buffered, int signature)
            throws IOException {
        super(files, null, buffered);
        this.unPacker = parent;
        this.signature = signature;
        hasSignature = true;
    }

    /**
     * Constructor of a new <code>SeparateInputStream</code> instance without signatures at beginning of
     * every part file, only used for testing.
     *
     * @param files    queue of files
     * @param parent   the parent <code>UnPacker</code> instance, which launches this
     *                 <code>SeparateInputStream</code> instance
     * @param buffered whether to use a buffered reader or not
     * @throws IOException if any IO error occurs
     */
    @Deprecated
    public SeparateInputStream(Deque<File> files, UnPacker parent, boolean buffered) throws IOException {
        super(files, null, buffered);
        this.unPacker = parent;
    }

    /**
     * Sets up the language loader
     *
     * @param lanLoader the <code>ResourceBundle</code> instance used for displaying text
     */
    public void setLanLoader(ResourceBundle lanLoader) {
        this.bundle = lanLoader;
    }

    /**
     * Returns a new <code>SeparateInputStream</code> instance.
     *
     * @param prefixName the original (common) name of the input files
     * @param suffixName the suffix extension name of the input files
     * @param partCount  the number of files to be read
     * @param parent     the parent <code>UnPacker</code> instance, which launches this
     *                   <code>SeparateInputStream</code> instance
     * @param signature  the signature at the beginning of every part file
     * @return a newly created <code>SeparateInputStream</code> instance
     * @throws IOException if any IO error occurs
     */
    public static SeparateInputStream createNew(String prefixName, String suffixName, int partCount, UnPacker parent,
                                                int signature)
            throws IOException {
        LinkedList<File> files = new LinkedList<>();
        for (int i = 1; i <= partCount; i++) {
            files.addLast(new File(String.format("%s.%d%s", prefixName, i, suffixName)));
        }
        return new SeparateInputStream(files, parent, true, signature);
    }

    /**
     * Reads bytes into <code>array</code> and returns the total number of bytes read.
     *
     * @param array the byte array to read data in
     * @return the number of bytes read
     * @throws IOException if the input stream is not readable
     */
    @Override
    public int read(byte[] array) throws IOException {
        int remain = array.length;
        while (remain > 0) {
            if (currentLength >= remain) {
//                System.out.format("%d %d %d\n", array.length, remain, currentLength);
                int read = currentInputStream.read(array, array.length - remain, remain);
//                System.out.println(length);
                currentLength -= read;
                remain -= read;
            } else {
                int read = currentInputStream.read(array, array.length - remain, (int) currentLength);
                remain -= read;
                if (files.isEmpty()) break;
                File f = files.getFirst();
//                System.out.println(f);
                if (!f.exists()) {
                    sectionSet = false;
                    expectedName = f.getAbsolutePath();
                    Platform.runLater(this::showPauseInfo
                    );
                    while (!sectionSet) System.out.print("");
                    if (unPacker != null && unPacker.isInterrupted) throw new IOInterruptedException();
                }
                f = files.removeFirst();

                if (unPacker != null) unPacker.currentFile.setValue(String.format("%s", f.getName()));

                currentLength = f.length();
                currentInputStream.close();
                if (buffered) currentInputStream = new BufferedInputStream(new FileInputStream(f));
                else currentInputStream = new FileInputStream(f);

                if (hasSignature) {
                    byte[] signatureBuffer = new byte[4];
                    if (currentInputStream.read(signatureBuffer) != 4) throw new IOException("Cannot read signature");
                    if (Bytes.bytesToInt32(signatureBuffer) != signature) {
                        throw new IOException("Signature verification failed");
                    }
                    currentLength -= 4;
                }
            }
        }
        return array.length - remain;
    }

    private void showPauseInfo() {
        Stage pauseStage = new Stage();
        VBox root = new VBox();
        root.setPrefSize(360.0, 120.0);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setSpacing(10.0);
        root.setPadding(new Insets(10.0));
        Scene scene = new Scene(root);
        pauseStage.setScene(scene);
        pauseStage.setTitle(bundle.getString("needNextSection"));

        HBox upperBox = new HBox();
        upperBox.setSpacing(10.0);
        upperBox.setAlignment(Pos.CENTER);
        TextField nameField = new TextField();
        nameField.setPrefWidth(290.0);
        nameField.setText(expectedName);
        Button browseButton = new Button(bundle.getString("browse"));
        browseButton.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                    bundle.getString("winLzzArchive"), "*.pz"));
            fc.setInitialDirectory(new File(expectedName).getParentFile());
            File newFile = fc.showOpenDialog(null);
            nameField.setText(newFile.getAbsolutePath());
        });
        upperBox.getChildren().addAll(nameField, browseButton);

        HBox lowerBox = new HBox();
        lowerBox.setSpacing(10.0);
        lowerBox.setAlignment(Pos.CENTER_RIGHT);
        Button confirmButton = new Button(bundle.getString("confirm"));
        confirmButton.setOnAction(e -> {
            File file = new File(nameField.getText());
            if (!file.exists()) {
                return;
            }
            files.removeFirst();
            files.addFirst(file);
            pauseStage.close();
        });

        Button cancelButton = new Button(bundle.getString("cancel"));
        cancelButton.setOnAction(e -> {
            pauseStage.close();
            if (unPacker != null) unPacker.interrupt();
        });

        lowerBox.getChildren().addAll(confirmButton, cancelButton);

        root.getChildren().addAll(new Label(bundle.getString("askNextSection")), upperBox, lowerBox);

        pauseStage.showAndWait();

        sectionSet = true;
    }


    /**
     * Skips <code>n</code> bytes while reading.
     *
     * @param n the count of bytes to be skipped
     * @return the counts of bytes that are successfully skipped
     * @throws IOException if the input stream is unavailable to be skipped
     */
    @Override
    public long skip(long n) throws IOException {
        long s = currentInputStream.skip(n);
        currentLength -= s;
        return s;
    }

    /**
     * Closes this <code>SeparateInputStream</code> instance.
     *
     * @throws IOException if this <code>SeparateInputStream</code> instance cannot be closed
     */
    @Override
    public void close() throws IOException {
        currentInputStream.close();
    }
}


/**
 * The GUI window that requires user to plugin the next part file, shows when WinLZZ cannot find the next file
 * automatically.
 *
 * @author zbh
 * @see javafx.application.Application
 * @since 0.8
 */
class InsertWindow extends Application {

    private ResourceBundle bundle;

    /**
     * Sets up the language loader
     *
     * @param lanLoader the <code>ResourceBundle</code> instance used for displaying text
     */
    public void setLanLoader(ResourceBundle lanLoader) {
        this.bundle = lanLoader;
    }

    /**
     * Shows the window.
     */
    public void show() {
        start(new Stage());
    }


    /**
     * Starts showing the window.
     *
     * @param primaryStage the <code>javafx.Stage</code> instance
     */
    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox();
        Scene scene = new Scene(root);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.setTitle(bundle.getString("needNextSection"));

        stage.showAndWait();
    }
}
