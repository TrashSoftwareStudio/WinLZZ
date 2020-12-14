package trashsoftware.winBwz.gui.graphicUtil;

/**
 * A node that records information of an entry of annotation.
 *
 * @author zbh
 * @since 0.7.1
 */
public class AnnotationNode {

    /**
     * The byte text of annotation.
     */
    private final byte[] annotation;

    /**
     * Whether the annotation text is compressed.
     */
    private final boolean compressed;

    /**
     * Creates a new AnnotationNode instance.
     *
     * @param annotation the annotation content, in byte array form.
     * @param compress   whether the annotation text is compressed.
     */
    public AnnotationNode(byte[] annotation, boolean compress) {
        this.annotation = annotation;
        this.compressed = compress;
    }

    /**
     * Returns the byte array representation of the annotation text.
     *
     * @return the annotation byte text
     */
    public byte[] getAnnotation() {
        return annotation;
    }

    /**
     * Returns whether the annotation is compressed.
     *
     * @return {@code true} if the annotation is compressed, {@code false} otherwise
     */
    public boolean isCompressed() {
        return compressed;
    }
}
