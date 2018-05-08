package WinLzz.ZSE;

import WinLzz.Utility.Util;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.zip.CRC32;

public class ZSEEncoder {

    private byte[] text;

    private int[] password;

    ZSEEncoder(byte[] text, String password) throws UnsupportedEncodingException {
        this.text = text;
        this.password = generatePassword(password, text.length);
    }

    @Deprecated
    public ZSEEncoder(byte[] text, int[] DirectPassword) {
        this.text = text;
        this.password = DirectPassword;
    }

    static int[] generatePassword(String pwd, int textLen) throws UnsupportedEncodingException {
        byte[] bytePwd = pwd.getBytes("UTF-8");
        int[] tempPwd = new int[bytePwd.length];
        int i = 0;
        CRC32 crc = new CRC32();
        while (i < bytePwd.length) {
            crc.update(bytePwd[i]);
            if (textLen > 64) tempPwd[i++] = (int) (crc.getValue() & 0x2f);
            else tempPwd[i++] = (int) (crc.getValue() & 0x0f);
            crc.reset();
        }
        return tempPwd;
    }

    private void rollBytes(byte[] text) {
        for (int i = 0; i < text.length; i++) {
            int shift = password[i % password.length];
            text[i] = (byte) (text[i] + shift);
        }
    }

    private byte[] switchRC(byte[] text) {
        ArrayList<ArrayList<Byte>> result = new ArrayList<>();
        int i = 0;
        while (i < text.length) {
            int current = password[result.size() % password.length];
            ArrayList<Byte> segment = new ArrayList<>();
            for (int j = 0; j < current; j++) {
                try {
                    segment.add(text[i]);
                    i += 1;
                } catch (ArrayIndexOutOfBoundsException aio) {
                    break;
                }
            }
            result.add(segment);
        }
        byte[] toReturn = new byte[text.length];
        int max = Util.arrayMax(password);
        int index = 0;

        for (int p = 0; p < max; p++) {
            for (ArrayList<Byte> aResult : result) if (p < aResult.size()) toReturn[index++] = aResult.get(p);
        }
        return toReturn;
    }


    static byte[] swap(byte[] text, int[] password) {
        int range = Util.arrayAverageInt(password);
        if (range > text.length) {
            return text;
        } else {
            int i = 0;
            byte[] result = new byte[text.length];
            while (i <= text.length - range * 2) {
                byte[] front = new byte[range];
                byte[] back = new byte[range];
                System.arraycopy(text, i, front, 0, range);
                System.arraycopy(text, i + range, back, 0, range);

                System.arraycopy(back, 0, result, i, range);
                System.arraycopy(front, 0, result, i + range, range);
                i += range * 2;
            }
            System.arraycopy(text, i, result, i, text.length - i);
            return result;
        }
    }


    private byte[] swapHead(byte[] text) {
        int range = Util.arraySum(password);
        if (range > text.length) {
            return text;
        } else {
            byte[] result = new byte[text.length];
            byte[] moved = new byte[range];
            System.arraycopy(text, text.length - range, moved, 0, range);
            System.arraycopy(moved, 0, result, 0, range);
            System.arraycopy(text, 0, result, range, text.length - range);
            return result;
        }
    }


    byte[] Encode() {

        rollBytes(text);
        byte[] switched = switchRC(text);
        byte[] swapped = swap(switched, password);

        return swapHead(swapped);
    }
}