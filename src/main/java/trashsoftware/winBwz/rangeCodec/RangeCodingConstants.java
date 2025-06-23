package trashsoftware.winBwz.rangeCodec;

public class RangeCodingConstants {
    public static final int MAX_FREQ = 1 << 15;
    public static final int RANGE_BITS = 48;
    public static final long MAX_RANGE = (1L << RANGE_BITS);
    public static final long MASK_RANGE = MAX_RANGE - 1;
//    public static final long EMERGENCY_FLUSH_THRESHOLD = 1L << 16;
}
