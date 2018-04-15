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
        ArrayList<byte[]> result = new ArrayList<>();
        int i = 0;
        while (i < text.length) {
            int current = password[result.size() % password.length];
            int index = 0;
            for (int j = 0; j < current; j++) {
                if (i < text.length) {
                    index += 1;
                    i += 1;
                } else {
                    break;
                }
            }
            byte[] segment = new byte[index];
            result.add(segment);
        }

        int maxLen = Util.arrayMax(password);
        int p = 0;
        for (int j = 0; j < maxLen; j++) {
            for (byte[] aResult : result) {
                if (j < aResult.length) {
                    aResult[j] = text[p];
                    p += 1;
                }
            }
        }

        byte[] toReturn = new byte[text.length];
        int index = 0;
        for (byte[] block : result) {
            System.arraycopy(block, 0, toReturn, index, block.length);
            index += block.length;
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
