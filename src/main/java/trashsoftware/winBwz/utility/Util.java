package trashsoftware.winBwz.utility;

import trashsoftware.winBwz.gui.GUIClient;

import java.io.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * A class that consists of common utility methods.
 *
 * @since 0.4
 */
public abstract class Util {

    /**
     * A commonly used io buffer size.
     */
    public static final int DEFAULT_BUFFER = 8192;

    /**
     * Deletes the {@code File} which has path/name <code>fileName</code> if such a file exists.
     *
     * @param fileName the path or name of the file to be deleted.
     */
    public static void deleteFile(String fileName) {
        if (fileName != null) {
            File file = new File(fileName);
            deleteFile(file);
        }
    }

    /**
     * Deletes the {@code File} if such a file exists.
     *
     * @param file the file to be deleted.
     */
    public static void deleteFile(File file) {
        if (file != null)
            if (file.exists())
                if (!file.delete()) System.out.println("Deletion Failed: " + file.getAbsolutePath());
    }

    /**
     * Recursively delete a directory <code>file</code> and its sub-files and sub-directories.
     *
     * @param file the directory to be deleted
     */
    public static void deleteDir(File file) {
        if (file.exists()) {
            if (file.isDirectory()) {
                File[] children = file.listFiles();
                for (File f : Objects.requireNonNull(children)) deleteDir(f);
            }
            if (!file.delete()) System.out.println("Deletion Failed: " + file.getAbsolutePath());
        }
    }

    /**
     * Recursively delete a directory <code>file</code> and its sub-files and sub-directories.
     *
     * @param file the directory to be deleted
     * @return {@code true} if all deletion succeed, {@code false} otherwise
     */
    @SuppressWarnings("all")
    public static boolean recursiveDelete(File file) {
        boolean suc = true;
        if (file.exists()) {
            if (file.isDirectory()) {
                File[] children = file.listFiles();
                for (File f : Objects.requireNonNull(children)) if (!recursiveDelete(f)) suc = false;
            }
            if (!file.delete()) suc = false;
        } else {
            suc = false;
        }
        return suc;
    }

    /**
     * Returns the name with the extension added.
     *
     * @param origName the original name
     * @param ext      the extension
     * @return the name with extension
     */
    public static String getCompressFileName(String origName, String ext) {
        return origName + "." + ext;
    }

    /**
     * Returns the name with a number added after main part to avoid duplication.
     * <p>
     * This method will find a available name anyway by adding one if a name already exist.
     *
     * @param fileName the file's name
     * @return the duplication-avoided name of the <code>fileName</code>
     */
    public static String getCopyName(String fileName) {
        int pos = fileName.lastIndexOf(".");
        String ext;
        String name;
        if (pos == -1) {
            ext = "";
            name = fileName;
        } else {
            ext = fileName.substring(pos);
            name = fileName.substring(0, pos);
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
     * Returns the name with the last extension removed, and a number added after main part to avoid duplication.
     * <p>
     * This method will find a available name anyway by adding one if a name already exist.
     *
     * @param compressedFileName the name with extension which will be removed
     * @return the duplication-avoided name of the <code>fileName</code> with extension removed
     */
    public static String getOriginalCopyName(String compressedFileName) {
        String origName = compressedFileName.substring(0, compressedFileName.lastIndexOf("."));
        return getCopyName(origName);
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

    /**
     * Returns the integer ceiling of a {@code double}, which is the minimum integer that greater than or equal to
     * <code>a</code>
     *
     * @param a the number to be calculated
     * @return the minimum integer that is greater than or equal to <code>a</code>
     */
    private static int ceiling(double a) {
        return (int) Math.ceil(a);
    }

    /**
     * Returns the base-2 logarithm of <code>a</code>
     *
     * @param a the number to be calculated
     * @return the base-2 logarithm of <code>a</code>
     */
    private static double log2(double a) {
        return (Math.log(a)) / Math.log(2);
    }

    /**
     * Concatenates all files which have paths stored in <code>files</code> into the stream <code>fos</code>, with the
     * order kept.
     *
     * @param fos        the target output stream, which will be written
     * @param files      path of files which will be written to <code>fos</code>
     * @param bufferSize the size of the IO buffer,
     *                   which is the number of bytes store in random access memory during process
     * @return total length of files that are concatenated
     * @throws IOException if any IO error occurs
     */
    public static long fileConcatenate(OutputStream fos, String[] files, int bufferSize) throws IOException {
        long len = 0;
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

    /**
     * Writes the input stream <code>bis</code> into file which has path <code>file</code>, with <code>length</code>
     * number of bytes copied.
     *
     * @param bis        the input stream
     * @param file       the name or path of file to be written
     * @param bufferSize the size of the IO buffer,
     *                   which is the number of bytes store in random access memory during process
     * @param length     the total length to be written
     * @throws IOException if any IO error occurs
     */
    public static void fileTruncate(InputStream bis, String file, int bufferSize, long length) throws IOException {
        long lengthRem = length;
        FileOutputStream bos = new FileOutputStream(file);
        byte[] buffer = new byte[bufferSize];
        int read;
        try {
            while ((read = bis.read(buffer, 0, (int) Math.min(bufferSize, lengthRem))) != -1 && lengthRem != 0) {
                bos.write(buffer, 0, read);
                lengthRem -= read;
            }
        } catch (IOInterruptedException e) {
            bos.flush();
            bos.close();
            throw e;
        }
        bos.flush();
        bos.close();
    }

    /**
     * Writes the input stream <code>bis</code> into the output stream <code>bos</code>, with <code>length</code>
     * number of bytes copied.
     *
     * @param bis    the input stream
     * @param bos    the {@code OutputStream} to be written
     * @param length the total length to be written
     * @return the number of bytes has been processed
     * @throws IOException if any IO error occurs
     */
    public static long fileTruncate(InputStream bis, OutputStream bos, long length) throws IOException {
        return fileTruncate(bis, bos, DEFAULT_BUFFER, length);
    }

    /**
     * Writes the input stream <code>bis</code> into the output stream <code>bos</code>.
     *
     * @param bis the input stream
     * @param bos the {@code OutputStream} to be written
     * @return the number of bytes has been processed
     * @throws IOException if any IO error occurs
     */
    public static long fileTruncate(InputStream bis, OutputStream bos) throws IOException {
        return fileTruncate(bis, bos, Long.MAX_VALUE);
    }

    /**
     * Writes the input stream <code>bis</code> into the output stream <code>bos</code>, with <code>length</code>
     * number of bytes copied.
     *
     * @param bis        the input stream
     * @param bos        the {@code OutputStream} to be written
     * @param bufferSize the size of the IO buffer,
     *                   which is the number of bytes store in random access memory during process
     * @param length     the total length to be written
     * @return the number of bytes has been processed
     * @throws IOException if any IO error occurs
     */
    public static long fileTruncate(InputStream bis, OutputStream bos, int bufferSize, long length) throws IOException {
        long lengthRem = length;
        byte[] buffer = new byte[bufferSize];
        int read;
        long res = 0;
        while ((read = bis.read(buffer, 0, (int) Math.min(bufferSize, lengthRem))) != -1 && lengthRem != 0) {
            bos.write(buffer, 0, read);
            lengthRem -= read;
            res += read;
        }
        return res;
    }

    /**
     * Writes the input stream <code>bis</code> into the output stream <code>bos</code>, with <code>length</code>
     * number of bytes copied.
     *
     * @param bis        the input stream
     * @param bos        the {@code OutputStream} to be written
     * @param bufferSize the size of the IO buffer,
     *                   which is the number of bytes store in random access memory during process
     * @param length     the total length to be written
     * @throws IOException if any IO error occurs
     */
    public static void fileTruncate(MultipleInputStream bis, OutputStream bos, int bufferSize, long length) throws IOException {
        long lengthRem = length;
        byte[] buffer = new byte[bufferSize];
        int read;

        while (lengthRem > 0) {
            if (lengthRem >= bufferSize) {
                read = bis.read(buffer);
                bos.write(buffer);
            } else {
                byte[] shortBuffer = new byte[(int) lengthRem];
                read = bis.read(shortBuffer);
                bos.write(shortBuffer);
            }
            lengthRem -= read;
        }
    }

    /**
     * Writes the input stream <code>bis</code> into file which has path <code>file</code>, with <code>length</code>
     * number of bytes copied.
     * <p>
     * This method takes a {@code RandomAccessFile} as the input stream/
     *
     * @param bis        the input stream
     * @param file       the name or path of file to be written
     * @param bufferSize the size of the IO buffer,
     *                   which is the number of bytes store in random access memory during process
     * @param length     the total length to be written
     * @throws IOException if any IO error occurs
     */
    public static void fileTruncate(RandomAccessFile bis, String file, int bufferSize, long length) throws IOException {
        long lengthRem = length;
        FileOutputStream bos = new FileOutputStream(file);
        byte[] buffer = new byte[bufferSize];
        int read;
        while ((read = bis.read(buffer, 0, (int) Math.min(bufferSize, lengthRem))) != -1 && lengthRem != 0) {
            bos.write(buffer, 0, read);
            lengthRem -= read;
        }
        bos.flush();
        bos.close();
    }

    /**
     * Converts a {@code Collection<Byte>} into a byte array, with the same element values.
     *
     * @param c the byte collection
     * @return the byte array which has same element values with <code>c</code>
     */
    public static byte[] collectionToArray(Collection<Byte> c) {
        byte[] result = new byte[c.size()];
        int i = 0;
        for (byte b : c) {
            result[i] = b;
            i += 1;
        }
        return result;
    }

    /**
     * Converts a {@code Collection<Short>} into a short array, with the same element values.
     *
     * @param c the short collection
     * @return the byte array which has same element values with <code>c</code>
     */
    public static short[] collectionToShortArray(Collection<Short> c) {
        short[] result = new short[c.size()];
        int i = 0;
        for (short b : c) {
            result[i] = b;
            i += 1;
        }
        return result;
    }

    /**
     * Converts a {@code Collection<Integer>} into a int array, with the same element values.
     *
     * @param c the short collection
     * @return the byte array which has same element values with <code>c</code>
     */
    public static int[] collectionToIntArray(Collection<Integer> c) {
        int[] result = new int[c.size()];
        int i = 0;
        for (int b : c) {
            result[i] = b;
            i += 1;
        }
        return result;
    }

    /**
     * Converts the number of seconds into the form "Minutes:Seconds".
     *
     * @param seconds the number of seconds
     * @return the readable form of <code>seconds</code> in "mm:ss"
     */
    public static String secondToString(long seconds) {
        if (seconds >= 3600) return "59:59";
        String minutes = String.valueOf(seconds / 60);
        minutes = Bytes.charMultiply('0', 2 - minutes.length()) + minutes;
        String secondsStr = String.valueOf(seconds % 60);
        secondsStr = Bytes.charMultiply('0', 2 - secondsStr.length()) + secondsStr;
        return minutes + ":" + secondsStr;
    }

    /**
     * Returns the readable {@code String} of <code>size</code>, representing the size of a file.
     * <p>
     * This method shows a number that at most 1,024 and a corresponding suffix
     *
     * @param size the size to be converted
     * @return the readable {@code String}
     */
    public static String sizeToReadable(long size) {
        return sizeToReadable(size, GUIClient.getBundle());
    }

    /**
     * Returns the readable {@code String} of <code>size</code>, representing the size of a file.
     * <p>
     * This method shows a number that at most 1,024 and a corresponding suffix
     *
     * @param size   the size to be converted
     * @param bundle the resource bundle used to display text
     * @return the readable {@code String}
     */
    public static String sizeToReadable(long size, ResourceBundle bundle) {
        if (size < Math.pow(2, 10)) return numToReadable2Decimal((int) size) + " " + bundle.getString("byte");
        else if (size < Math.pow(2, 20)) return numToReadable2Decimal((double) size / 1024) + " KB";
        else if (size < Math.pow(2, 30)) return numToReadable2Decimal((double) size / 1048576) + " MB";
        else return numToReadable2Decimal((double) size / 1073741824) + "GB";
    }

    /**
     * Splits a {@code String} representation of a number every 3 digit with the colon symbol, from the back side.
     *
     * @param src the {@code String} representation of a number to be split.
     * @return the colon-split {@code String} of <code>src</code>
     */
    public static String splitNumber(String src) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = src.length() - 1; i >= 0; i--) {
            if (count % 3 == 0 && count != 0) sb.append(",");
            sb.append(src.charAt(i));
            count += 1;
        }
        return sb.reverse().toString();
    }

    /**
     * Splits every <code>lineLength</code> character of <code>src</code> to lines.
     *
     * @param src        the {@code String} to be split
     * @param lineLength the interval of split
     * @return the {@code String} after split
     */
    public static String splitStringToLine(String src, int lineLength) {
        StringBuilder builder = new StringBuilder();
        int i = 0;
        while (i < src.length() - lineLength) {
            builder.append(src, i, i + lineLength).append('\n');
            i += lineLength;
        }
        builder.append(src.substring(i));
        return builder.toString();
    }

    public static String numToReadable2Decimal(double num) {
        return num == (int) num ? String.format("%,d", (int) num) : String.format("%,.2f", num);
    }

    /**
     * Returns the total number of bytes in a {@code Collection<byte[]>}.
     *
     * @param c the collection of byte arrays
     * @return the total length of arrays in <code>c</code>
     */
    public static int collectionOfArrayLength(Collection<byte[]> c) {
        int sum = 0;
        for (byte[] b : c) {
            sum += b.length;
        }
        return sum;
    }

    /**
     * Sorts the <code>data</code> using counting-sort algorithm.
     *
     * @param data    the short array to be sorted
     * @param buckets the bucket number used to sort,
     *                which is typically the alphabet size
     */
    public static void countingSort(int[] data, int buckets) {
        int[] counts = new int[buckets];
        for (int s : data) counts[s] += 1;
        int index = 0;
        for (int i = 0; i < buckets; i++) for (int j = 0; j < counts[i]; j++) data[index++] = i;
    }

    /**
     * Return {@code 1} if s1 is lexicographically greater than s2, {@code 0} if equal, {@code -1} otherwise.
     *
     * @param s1 {@code String} 1
     * @param s2 {@code String} 2
     * @return comparison result of two string.
     */
    public static int stringCompare(String s1, String s2) {
        int s1Length = s1.length();
        int s2Length = s2.length();
        int minLength = Math.min(s1Length, s2Length);
        for (int i = 0; i < minLength; i++) {
            int charCmp = Character.compare(s1.charAt(i), s2.charAt(i));
            if (charCmp != 0) return charCmp;
        }
        return Integer.compare(s1Length, s2Length);
    }

    /**
     * Reads a text file into a {@code String}.
     *
     * @param file the {@code File} to be read
     * @return the content of <code>file</code>
     * @throws IOException if the <code>file</code> is not readable
     */
    public static String readTextFile(File file) throws IOException {
        FileInputStream bin = new FileInputStream(file);
        int p = (bin.read() << 8) + bin.read();
        bin.close();
        String code;

        switch (p) {
            case 0xefbb:
                code = "UTF-8";
                break;
            case 0xfffe:
                code = "Unicode";
                break;
            case 0xfeff:
                code = "UTF-16BE";
                break;
            default:
                code = "GBK";
                break;
        }

        InputStream fis = new FileInputStream(file);
        BufferedReader br = new BufferedReader(new InputStreamReader(fis, code));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) builder.append(line).append('\n');
        br.close();

        return builder.toString();
    }

    /**
     * Records the size block into byte array, which consists of several {@code long}'s.
     *
     * @param sizes the size block array
     * @return the byte array records the block array
     */
    public static byte[] generateSizeBlock(long[] sizes) {
        StringBuilder bits = new StringBuilder();
        for (long i : sizes) {
            long bit8 = 256;
            long bit16 = 65_536 + bit8;
            long bit24 = 16_777_216 + bit16;
            long bit32 = 4_294_967_296L + bit24;
            long bit40 = 1_099_511_627_776L + bit32;
            long bit48 = 281_474_976_710_656L + bit40;
            long bit56 = 72_057_594_037_927_936L + bit48;
            if (i < bit8) {
                bits.append("00");
                bits.append(Bytes.numberToBitString(i, 8));
            } else if (i < bit16) {
                bits.append("01");
                bits.append(Bytes.numberToBitString(i - bit8, 16));
            } else if (i < bit24) {
                bits.append("10");
                bits.append(Bytes.numberToBitString(i - bit16, 24));
            } else if (i < bit32) {
                bits.append("110");
                bits.append(Bytes.numberToBitString(i - bit24, 32));
            } else if (i < bit40) {
                bits.append("1110");
                bits.append(Bytes.numberToBitString(i - bit32, 40));
            } else if (i < bit48) {
                bits.append("11110");
                bits.append(Bytes.numberToBitString(i - bit40, 48));
            } else if (i < bit56) {
                bits.append("111110");
                bits.append(Bytes.numberToBitString(i - bit48, 56));
            } else {
                bits.append("111111");
                bits.append(Bytes.numberToBitString(i - bit56, 64));
            }
        }
        byte[] block = Bytes.stringBuilderToBytesFull(bits);
        byte[] result = new byte[block.length + 1];
        System.arraycopy(block, 0, result, 0, block.length);
        result[result.length - 1] = (byte) block.length;
        return result;
    }

    /**
     * Recovers the size block, which consists of several {@code long}'s.
     *
     * @param block  the byte array recorded size block
     * @param number the number of numbers stored inside the size block
     * @return the array of sizes
     */
    public static long[] recoverSizeBlock(byte[] block, int number) {
        String bits = Bytes.bytesToString(block);
        long[] result = new long[number];
        int i = 0;
        int j = 0;
        long bit8 = 256;
        long bit16 = 65_536 + bit8;
        long bit24 = 16_777_216 + bit16;
        long bit32 = 4_294_967_296L + bit24;
        long bit40 = 1_099_511_627_776L + bit32;
        long bit48 = 281_474_976_710_656L + bit40;
        long bit56 = 72_057_594_037_927_936L + bit48;
        while (j < number) {
            String flag = bits.substring(i, i + 2);
            i += 2;
            long base;
            int read;
            switch (flag) {
                case "00":
                    base = 0;
                    read = 8;
                    break;
                case "01":
                    base = bit8;
                    read = 16;
                    break;
                case "10":
                    base = bit16;
                    read = 24;
                    break;
                case "11":
                    char next = bits.charAt(i);
                    i += 1;
                    if (next == '0') {  // 110
                        base = bit24;
                        read = 32;
                    } else {
                        next = bits.charAt(i);
                        i += 1;
                        if (next == '0') {  // 1110
                            base = bit32;
                            read = 40;
                        } else {
                            next = bits.charAt(i);
                            i += 1;
                            if (next == '0') {  // 11110
                                base = bit40;
                                read = 48;
                            } else {
                                next = bits.charAt(i);
                                i += 1;
                                if (next == '0') {  // 111110
                                    base = bit48;
                                    read = 56;
                                } else {  // 111111
                                    base = bit56;
                                    read = 64;
                                }
                            }
                        }
                    }
                    break;
                default:
                    throw new IndexOutOfBoundsException("Unknown Error");
            }
            long num = base + Long.parseLong(bits.substring(i, i + read), 2);
            i += read;
            result[j] = num;
            j += 1;
        }
        return result;
    }

    /**
     * Returns the byte representation of window size, used for recording the size of sliding window of lzz2, or
     * the block size of bwz.
     * <p>
     * The <code>windowSize</code> must be a power of 2.
     *
     * @param windowSize the window size to be converted
     * @return the byte representation
     */
    public static byte windowSizeToByte(int windowSize) {
        return (byte) Util.ceiling(Util.log2(windowSize));
    }

    /**
     * Returns whether the <code>s</code> contains any character in <code>symbols</code>.
     *
     * @param symbols the dictionary chars
     * @param s       the string to be searched
     * @return {@code true} if <code>s</code> contains any character in <code>symbols</code>,
     * {@code false} otherwise
     */
    public static boolean charArrayContains(char[] symbols, String s) {
        int len = s.length();
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            for (char x : symbols) if (c == x) return true;
        }
        return false;
    }

    /**
     * Returns the array of bit-strings, converted from the input byte array <code>array</code>.
     *
     * @param array the byte array to be converted
     * @return the bit string array
     */
    @Deprecated
    public static String[] byteArrayToBitStringArray(byte[] array) {
        String[] result = new String[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = Bytes.byteToBitString(array[i]);
        }
        return result;
    }

    public static boolean checkFileIdentical(String fileName1, String fileName2) {
        try {
            BufferedInputStream checkBis = new BufferedInputStream(new FileInputStream(fileName1));
            BufferedInputStream checkBis2 = new BufferedInputStream(new FileInputStream(fileName2));
            int read;
            byte[] buffer = new byte[8192];
            byte[] buffer2 = new byte[8192];
            int begin = 0;
            boolean suc = true;
            while ((read = checkBis.read(buffer)) > 0) {
                int read2 = checkBis2.read(buffer2);
                if (read != read2) {
                    System.out.println("Lengths not match");
                    suc = false;
                    break;
                }
                if (!Arrays.equals(buffer, buffer2)) {
                    System.out.println("Content not match between " + begin + " and " + (begin + read));
                    suc = false;
                    break;
                }

                begin += read;
            }
            checkBis.close();
            checkBis2.close();
            return suc;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Returns whether there is a String in the array that equals to the pattern.
     *
     * @param array         the array
     * @param pattern       the string to be searched
     * @param caseSensitive whether the case affects the result
     * @return {@code true} iff there is at least one {@code String} in {@code array} that {@code equals()} the
     * string {@code pattern}
     */
    public static boolean arrayContains(String[] array, String pattern, boolean caseSensitive) {
        for (String s : array) {
            if (caseSensitive) {
                if (s.equals(pattern)) return true;
            } else {
                if (s.toLowerCase().equals(pattern.toLowerCase())) return true;
            }
        }
        return false;
    }
}
