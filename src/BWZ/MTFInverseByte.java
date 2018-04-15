package BWZ;

import java.util.LinkedList;

public class MTFInverseByte {

    private byte[] text;

    public MTFInverseByte(byte[] text) {
        this.text = text;
    }

    public byte[] Inverse() {
        LinkedList<Byte> ll = new LinkedList<>();
        for (byte i = 0; i < 18; i++) ll.addLast(i);
        byte[] result = new byte[text.length];
        int index = 0;
        for (short b : text) {
            int i = b & 0xff;
            Byte s;
            if (i == 0) s = ll.getFirst();
            else {
                s = ll.remove(i);
                ll.addFirst(s);
            }
            result[index] = s;
            index += 1;
        }
        return result;
    }
}
