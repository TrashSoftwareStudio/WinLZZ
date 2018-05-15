package WinLzz.GraphicUtil;

public class AnnotationNode {

    /**
     * The byte text of annotation.
     */
    private byte[] annotation;

    /**
     * Whether the annotation text is compressed.
     */
    private boolean compressed;

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

    public byte[] getAnnotation() {
        return annotation;
    }

    public boolean isCompressed() {
        return compressed;
    }
}
