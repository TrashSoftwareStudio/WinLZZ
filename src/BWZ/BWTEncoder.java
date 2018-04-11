package BWZ;

import Utility.Bytes;
class BWTEncoder {

    private short[] text;

    private int origRowIndex;

    BWTEncoder(byte[] text) {
        this.text = new short[text.length + 1];
        for (int i = 0; i < text.length; i++) {
            this.text[i] = (short) ((text[i] & 0xff) + 1);
        }
        this.text[this.text.length - 1] = 0;
    }

    short[] Transform2() {
        short[] result = new short[text.length + 3];
        short[] trans = transform2();
        byte[] indexRep = Bytes.intToBytes24(origRowIndex);
        for (int i = 0; i < 3; i++) {
            result[i] = (short) (indexRep[i] & 0xff);
        }
        System.arraycopy(trans, 0, result, 3, trans.length);
        return result;
    }

    private short[] transform2() {
        SuffixArray sa = new SuffixArray(text);
        sa.build(257);
//        System.out.println(Arrays.toString(sa2.getSa()));
        int[] array = sa.getSa();

        int len = array.length;
        assert len == text.length;
        short[] result = new short[len];
        for (int i = 0; i < len; i++) {
            int pos = (array[i] + len - 1) % len;
            result[i] = text[pos];

            if (array[i] == 0) {
                origRowIndex = i;
            }
        }
        return result;
    }

}
