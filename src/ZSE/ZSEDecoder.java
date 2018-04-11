package ZSE;

import Utility.Util;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class ZSEDecoder {

    private byte[] encodedText;

    private int[] password;

    ZSEDecoder(byte[] encodedText, String password) throws UnsupportedEncodingException {
        this.encodedText = encodedText;
        this.password = ZSEEncoder.generatePassword(password, encodedText.length);
    }

    @Deprecated
    public ZSEDecoder(byte[] encodedText, int[] directPassword) {
        this.encodedText = encodedText;
        this.password = directPassword;
    }

    private void rollBytes(byte[] text) {
        for (int i = 0; i < text.length; i++) {
            int shift = password[i % password.length];
            text[i] = (byte) (text[i] - shift);
        }
    }

    private byte[] swapTail(byte[] text) {
        int range = Util.arraySum(password);
        if (range > text.length) {
            return text;
        } else {
            byte[] result = new byte[text.length];
            byte[] moved = new byte[range];
            System.arraycopy(text, 0, moved, 0, range);

            System.arraycopy(text, range, result, 0, text.length - range);
            System.arraycopy(moved, 0, result, text.length - range, range);
            return result;
        }
    }

    private byte[] switchRCBack(byte[] text) {

//        if (text.length < password[0]) {
//            return text;
//        }


//        System.out.println(Arrays.toString(password));

        ArrayList<ArrayList<Byte>> result = new ArrayList<>();
        int i = 0;
        while (i < text.length) {
            int current = password[result.size() % password.length];
            ArrayList<Byte> segment = new ArrayList<>();
            for (int j = 0; j < current; j++) {
                if (i < text.length) {
                    segment.add((byte) 0);
                    i += 1;
                } else {
                    break;
                }
            }
            result.add(segment);
        }

        int maxLen = Util.arrayMax(password);
        int p = 0;
        for (int j = 0; j < maxLen; j++) {
            for (ArrayList<Byte> aResult : result) {
                try {
                    aResult.set(j, text[p]);
                    p += 1;
                } catch (IndexOutOfBoundsException aie) {
                    //
                }
            }
        }

        byte[] toReturn = new byte[text.length];

        ArrayList<Byte> result2 = new ArrayList<>();

        for (ArrayList<Byte> list : result) {
            result2.addAll(list);
        }

        for (int l = 0; l < text.length; l ++) {
            toReturn[l] = result2.get(l);
        }

        return toReturn;
    }

    byte[] Decode() {

        byte[] swappedTail = swapTail(encodedText);
        byte[] swappedBack = ZSEEncoder.swap(swappedTail, password);

        byte[] result = switchRCBack(swappedBack);
        rollBytes(result);
        return result;
    }
}
