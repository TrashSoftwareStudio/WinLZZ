package BWZ;

class BWTEncoderByte {

    /* Original text for encoding. */
    private byte[] text;

    private int origRowIndex;

    /**
     * Creates a new instance of BWTEncoder.
     *
     * @param text The text being transformed.
     */
    BWTEncoderByte(byte[] text) {
        this.text = new byte[text.length + 1];
        for (int i = 0; i < text.length; i++) this.text[i] = (byte) ((text[i] & 0xff) + 1);  // Transform every byte
        // to unsigned and plus one to make sure nothing is smaller than or equal to the EOF character.
        this.text[this.text.length - 1] = 0;  // Add the EOF character (0) at the end of the original text.
        // This is necessary for transforming suffix array into Burrows-Wheeler matrix.
    }

    byte[] Transform() {
        return transform();
//        byte[] result = new byte[text.length];
//        byte[] trans = transform();
//        byte[] indexRep = Bytes.intToBytes24(origRowIndex);
////        for (int i = 0; i < 3; i++) result[i] = (byte) (indexRep[i] & 0xff);
////        System.arraycopy(trans, 0, result, 3, trans.length);
//        return result;
    }

    int getOrigRowIndex() {
        return origRowIndex;
    }

    private byte[] transform() {
        int[] suffixArray;
        SuffixArrayDoublingByte sa = new SuffixArrayDoublingByte(text);
        sa.build(17);
        suffixArray = sa.getSa();

        int len = suffixArray.length;
        assert len == text.length;
        byte[] result = new byte[len];
        for (int i = 0; i < len; i++) {
            int pos = (suffixArray[i] + len - 1) % len;
            result[i] = text[pos];
            if (suffixArray[i] == 0) origRowIndex = i;
        }
        return result;
    }
}
