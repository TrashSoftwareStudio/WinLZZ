package Utility;

import java.io.*;
import java.util.Collection;

public abstract class Util {

    public static void deleteFile(String fileName) {
        File file = new File(fileName);
        if (file.exists()) {
            if (!file.delete()) System.out.println("Deletion Failed: " + fileName);
        }
    }

    public static void deleteFile(File file) {
        if (file.exists()) {
            if (!file.delete()) System.out.println("Deletion Failed");
        }
    }

    public static String getCompressFileName(String origName, String ext) {
        return origName + "." + ext;
    }


    public static String getOriginalName(String fileName) {
        return fileName.substring(0, fileName.lastIndexOf("."));
    }


    public static String getOriginalCopyName(String fileName) {
        String origName = fileName.substring(0, fileName.lastIndexOf("."));
        int pos = origName.lastIndexOf(".");
        String ext;
        String name;
        if (pos == -1) {
            ext = "";
            name = origName;
        } else {
            ext = origName.substring(pos);
            name = origName.substring(0, pos);
        }
        int copy = 1;
        String copyName = name + "(" + copy + ")" + ext;
        while (new File(copyName).exists()) {
            copy += 1;
            copyName = name + "(" + copy + ")" + ext;
        }
        return copyName;
    }


    /**
     * Returns the sum of an integer array.
     *
     * @param source an integer array.
     * @return the sum of the array.
     */
    public static int arraySum(int[] source) {
        int sum = 0;
        for (int i : source) sum += i;
        return sum;
    }


    /**
     * Returns the maximum value of an integer array.
     *
     * @param source an integer array.
     * @return the maximum value of the array.
     */
    public static int arrayMax(int[] source) {
        int max = 0;
        for (int i : source)
            if (i > max) max = i;
        return max;
    }


    /**
     * Returns the average value of an integer array.
     *
     * @param source an integer array.
     * @return the average value of the array.
     */
    public static int arrayAverageInt(int[] source) {
        return arraySum(source) / source.length;
    }

    public static int ceiling(double a) {
        return (int) Math.ceil(a);
    }

    public static double log2(double a) {
        return (Math.log(a)) / Math.log(2);
    }

    public static void fileConcatenate(String[] files, int bufferSize) throws IOException {
        FileOutputStream fos = new FileOutputStream(files[0], true);
        for (int i = 1; i < files.length; i++) {
            String name = files[i];
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(name));
            byte[] buffer = new byte[bufferSize];
            int read;
            while ((read = bis.read(buffer)) != -1) fos.write(buffer, 0, read);
            bis.close();
        }
        fos.flush();
        fos.close();
    }

    public static int fileConcatenate(OutputStream fos, String[] files, int bufferSize) throws IOException {
        int len = 0;
        for (String name : files) {
            FileInputStream bis = new FileInputStream(name);
            byte[] buffer = new byte[bufferSize];
            int read;
            while ((read = bis.read(buffer)) != -1) {
                len += read;
                fos.write(buffer, 0, read);
            }
            bis.close();
        }
        return len;
    }

    public static void fileTruncate(InputStream bis, String file, int bufferSize, int length) throws IOException {
        int lengthRem = length;
        FileOutputStream bos = new FileOutputStream(file);
        byte[] buffer = new byte[bufferSize];
        int read;
        while ((read = bis.read(buffer, 0, Math.min(bufferSize, lengthRem))) != -1 && lengthRem != 0) {
            bos.write(buffer, 0, read);
            lengthRem -= read;
        }
        bos.flush();
        bos.close();
    }

    public static void fileTruncate(InputStream bis, OutputStream bos, int bufferSize, int length) throws IOException {
        int lengthRem = length;
        byte[] buffer = new byte[bufferSize];
        int read;
        while ((read = bis.read(buffer, 0, Math.min(bufferSize, lengthRem))) != -1 && lengthRem != 0) {
            bos.write(buffer, 0, read);
            lengthRem -= read;
        }
        bos.flush();
        bos.close();
    }

    public static void fileTruncate(RandomAccessFile bis, String file, int bufferSize, int length) throws IOException {
        int lengthRem = length;
        FileOutputStream bos = new FileOutputStream(file);
        byte[] buffer = new byte[bufferSize];
        int read;
        while ((read = bis.read(buffer, 0, Math.min(bufferSize, lengthRem))) != -1 && lengthRem != 0) {
            bos.write(buffer, 0, read);
            lengthRem -= read;
        }
        bos.flush();
        bos.close();
    }

    public static byte[] collectionToArray(Collection<Byte> c) {
        byte[] result = new byte[c.size()];
        int i = 0;
        for (byte b : c) {
            result[i] = b;
            i += 1;
        }
        return result;
    }

    public static short[] collectionToShortArray(Collection<Short> c) {
        short[] result = new short[c.size()];
        int i = 0;
        for (short b : c) {
            result[i] = b;
            i += 1;
        }
        return result;
    }

    public static byte[] collectionToArrayShort(Collection<Short> c) {
        byte[] result = new byte[c.size() * 2];
        int i = 0;
        for (short s : c) {
            byte[] bytes = Bytes.shortToBytes(s);
            result[i * 2] = bytes[0];
            result[i * 2 + 1] = bytes[1];
            i += 1;
        }
        return result;
    }

    public static String secondToString(long seconds) {
        if (seconds >= 3600) return "59:59";
        String minutes = String.valueOf(seconds / 60);
        minutes = Bytes.charMultiply('0', 2 - minutes.length()) + minutes;
        String secondsStr = String.valueOf(seconds % 60);
        secondsStr = Bytes.charMultiply('0', 2 - secondsStr.length()) + secondsStr;
        return minutes + ":" + secondsStr;
    }

    public static String sizeToReadable(long size) {
        if (size < Math.pow(2, 20)) return numToReadable((int) size) + " 字节";
        else if (size < Math.pow(2, 30)) return numToReadable((int) (size / 1024 + 1)) + " KB";
        else return numToReadable((int) (size / 1048576 + 1)) + "MB";
    }

    private static String numToReadable(int num) {
        if (num >= 1048576) throw new IndexOutOfBoundsException("Number Too large");
        String s = String.valueOf(num);
        if (s.length() <= 3) return s;
        int split = s.length() - 3;
        return s.substring(0, split) + "," + s.substring(split);
    }

    public static int collectionOfArrayLength(Collection<byte[]> c) {
        int sum = 0;
        for (byte[] b : c) {
            sum += b.length;
        }
        return sum;
    }

}
