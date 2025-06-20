package trashsoftware.winBwz.rangeCodec;

public class RangeCodingConstants {
    public static final int MAX_FREQ = 1 << 23;
    public static final int RANGE_BITS = 40;
    public static final long MAX_RANGE = (1L << RANGE_BITS);
    public static final long MASK_RANGE = MAX_RANGE - 1;
}
