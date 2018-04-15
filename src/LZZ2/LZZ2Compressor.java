/*
 * LZZ2 (Lempel-Ziv-ZBH 2) compressor.
 *
 * An improved version of LZ77 algorithm, implemented by Bohan Zhang.
 */

package LZZ2;

import BWZ.MTFTransformByte;
import Huffman.HuffmanCompressor;
import Interface.Compressor;
import LZZ2.Util.*;
import Huffman.MapCompressor.MapCompressor;
import Packer.Packer;
import Utility.FileBitOutputStream;
import Utility.FileInputBufferArray;
import Utility.Util;

import java.io.*;
import java.util.*;

public class LZZ2Compressor implements Compressor {

    private InputStream sis;

    private long totalLength;

    private int windowSize;  // Size of sliding window.

    private int bufferMaxSize;  // Size of LAB (Look ahead buffer).

    private int dictSize;

    final static int minimumMatchLen = 3;

    private String mainTempName, lenHeadTempName, disHeadTempName, flagTempName, dlBodyTempName;

    private int cmpSize = 0;

    private int itemCount = 0;

    private Packer parent;

    private int timeAccumulator = 0;

    private long lastUpdateProgress = 0;

    private long startTime;

    private long timeOffset;

    private int compressionLevel;

    /**
     * Constructor of a new LZZ2Compressor Object.
     *
     * @param inFile     name of file to compress.
     * @param windowSize total sliding window size.
     * @param bufferSize size of look ahead buffer.
     * @throws IOException if error occurs during file reading or writing.
     */
    public LZZ2Compressor(String inFile, int windowSize, int bufferSize) throws IOException {
        this.windowSize = windowSize;
        this.bufferMaxSize = bufferSize + minimumMatchLen + 1;
        this.dictSize = windowSize - bufferSize;

        this.totalLength = new File(inFile).length();
        this.sis = new FileInputStream(inFile);
        setTempNames(inFile);
    }

    private void setTempNames(String inFile) {
        this.mainTempName = inFile + ".main.temp";
        this.lenHeadTempName = inFile + ".len.temp";
        this.disHeadTempName = inFile + ".dis.temp";
        this.flagTempName = inFile + ".flag.temp";
        this.dlBodyTempName = inFile + ".dlb.temp";
    }

    private void compressText() throws IOException {
        FileInputBufferArray fba = new FileInputBufferArray(sis, totalLength, windowSize);

        if (totalLength <= bufferMaxSize) {
            dictSize = (int) totalLength - 1;
            bufferMaxSize = (int) totalLength - 1;
        }
        SimpleHashSlider slider = new SimpleHashSlider();
        LinkedHashSet<Integer> omitSet = new LinkedHashSet<>();

        int front = bufferMaxSize;  // The frontier of buffer.
        int position = 0;  // The processing index. i.e. the border of buffer and slider.
        int lastRead = 0;  // The number of bytes proceed in last round.
        int sliderSize = 0;
        int bufferSize = bufferMaxSize;

        fillHashSlider(fba, 0, front, slider, omitSet);

        LinkedList<Integer> lastMatches = new LinkedList<>();
        int lastLength = -1;

        BufferedOutputStream mainFos = new BufferedOutputStream(new FileOutputStream(mainTempName));
        BufferedOutputStream disFos = new BufferedOutputStream(new FileOutputStream(disHeadTempName));
        BufferedOutputStream lenFos = new BufferedOutputStream(new FileOutputStream(lenHeadTempName));
        FileBitOutputStream flagFos = new FileBitOutputStream(new BufferedOutputStream(new FileOutputStream(flagTempName)));
        FileBitOutputStream dlbFos = new FileBitOutputStream(new BufferedOutputStream(new FileOutputStream(dlBodyTempName)));

        long lastCheckTime = System.currentTimeMillis();
        startTime = lastCheckTime;
        if (parent != null) timeOffset = lastCheckTime - parent.startTime;
        long currentTime;

        while (true) {
            // Push buffer.
            int newLoad = bufferMaxSize - bufferSize;

            if (front + newLoad >= totalLength) newLoad = (int) totalLength - front;

            fillHashSlider(fba, front, newLoad, slider, omitSet);

            bufferSize += newLoad;
            front += newLoad;
            sliderSize += lastRead;

            position += lastRead;
            slider.clearOutRanged(dictSize);
            optimizeLinkedHashSet(omitSet, position);
            if (sliderSize > dictSize) sliderSize = dictSize;

            if (bufferSize < 3) {
                for (int i = position; i < totalLength; i++) {
                    flagFos.write('0');
                    mainFos.write(fba.getByte(i));
                }
                break;
            }

            int[] search;  // Looking for longest match.
            if (getCompressionParam()[0] == 1) search = search1(fba, position, slider, sliderSize, bufferSize);
            else if (getCompressionParam()[0] == 2) search = search2(fba, position, slider, sliderSize, bufferSize);
            else search = search0(fba, position, slider, sliderSize, bufferSize);

            if (search[1] < minimumMatchLen) {
                // Not a match
                flagFos.write('0');
                mainFos.write(fba.getByte(position));
                lastRead = 1;
                bufferSize -= 1;
            } else {
                // A match.
                lastRead = search[1];
                bufferSize -= search[1];
                if (search[2] > 0) {
                    for (int i = 0; i < search[2]; i++) {
                        flagFos.write('0');
                        mainFos.write(fba.getByte(position));
                        position += 1;
                    }
                    bufferSize -= search[2];
                }

                flagFos.write('1');
                int distanceInt = sliderSize - search[0] + search[2];
                int lengthInt = search[1];

                int findInLast = reverseIndexInQueue(lastMatches, distanceInt);
                if (findInLast == -1) {
                    LZZ2Util.addDistance(distanceInt, 0, disFos, dlbFos);  // Distance first.
                    LZZ2Util.addLength(lengthInt, minimumMatchLen, lenFos, dlbFos);
                } else {
                    if (findInLast == 0 && lastLength == lengthInt) {
                        disFos.write((byte) 4);
                    } else {
                        disFos.write((byte) findInLast);
                        LZZ2Util.addLength(lengthInt, minimumMatchLen, lenFos, dlbFos);
                    }
                }
                itemCount += 1;
                lastLength = lengthInt;
                lastMatches.addFirst(distanceInt);
                if (lastMatches.size() > 4) lastMatches.removeLast();
            }
            if (parent != null && parent.isInterrupted) break;
            if (parent != null && (currentTime = System.currentTimeMillis()) - lastCheckTime >= 50) {
                updateInfo(position, currentTime);
                lastCheckTime = currentTime;
            }
        }

        mainFos.flush();
        mainFos.close();
        disFos.flush();
        disFos.close();
        lenFos.flush();
        lenFos.close();
        flagFos.flush();
        flagFos.close();
        dlbFos.flush();
        dlbFos.close();
        fba.close();

    }

    private void fillHashSlider(FileInputBufferArray fba, int front, int newLoad, SimpleHashSlider slider,
                                LinkedHashSet<Integer> omitSet) {
        for (int i = front; i < front + newLoad; i++) {
            if (i > fba.length() - 3) break;
            HashNode hn = new HashNode(fba.getByte(i), fba.getByte(i + 1), fba.getByte(i + 2));
            ArrayDeque<Integer> indices = slider.get(hn);
            if (indices != null) {
                if (getCompressionParam()[1] == 0) {
                    if (indices.getLast() != i - 1 && !omitSet.contains(i)) {
                        slider.addIndex(hn, i);
                    } else {
                        int j1 = 0;
                        while (j1 + i < fba.length() - 3 && j1 < bufferMaxSize - 8 && hn.getByte(2) == fba.getByte(j1 + i + 3)) {
                            slider.addVoid();
                            omitSet.add(i + j1);
                            j1 += 1;
                        }
                        i += j1;
                    }
                } else {
                    slider.addIndex(hn, i);
                }
            } else {
                indices = new ArrayDeque<>();
                indices.addLast(i);
                slider.put(hn, indices);
            }
        }
    }

    private int[] search0(FileInputBufferArray fba, int pos, SimpleHashSlider slider, int sliderSize, int bufferSize) {
        int[] search = longestMatch(fba, pos, slider, sliderSize, bufferSize);
        return new int[]{search[0], search[1], 0};
    }

    private int[] search1(FileInputBufferArray fba, int pos, SimpleHashSlider slider, int sliderSize, int bufferSize) {
        int[] first = longestMatch(fba, pos, slider, sliderSize, bufferSize);
        if (first[1] == 0 || first[1] == bufferSize - 1) {
            return new int[]{first[0], first[1], 0};
        } else {
            int[] second = longestMatch(fba, pos + 1, slider, sliderSize + 1, bufferSize - 1);
            if (first[1] + 1 < second[1]) return new int[]{second[0], second[1], 1};
            else return new int[]{first[0], first[1], 0};
        }
    }

    private int[] search2(FileInputBufferArray fba, int pos, SimpleHashSlider slider, int sliderSize, int bufferSize) {
        int[] first = longestMatch(fba, pos, slider, sliderSize, bufferSize);
        if (first[1] == 0 || first[1] == bufferSize - 1) return new int[]{first[0], first[1], 0};
        else {
            int[] firstContinue = longestMatch(fba, pos + first[1], slider, sliderSize + first[1], bufferSize - first[1]);
            int[] second = longestMatch(fba, pos + 1, slider, sliderSize + 1, bufferSize - 1);
            int[] secondContinue = longestMatch(fba, pos + second[1] + 1, slider,
                    sliderSize + second[1] + 1, bufferSize - second[1] - 1);
            if (first[1] + firstContinue[1] + 1 < second[1] + secondContinue[1])
                return new int[]{second[0], second[1], 1};
            else return new int[]{first[0], first[1], 0};
        }

    }

    private int[] longestMatch(FileInputBufferArray fba, int pos, SimpleHashSlider slider, int sliderSize, int bufferSize) {
        if (bufferSize < 3) return new int[]{0, 0};
        int sliderStart = pos - sliderSize;

        HashNode hn = new HashNode(fba.getByte(pos), fba.getByte(pos + 1), fba.getByte(pos + 2));

        ArrayDeque<Integer> indices = slider.get(hn);
        if (indices != null) {
            ArrayList<Integer> list = new ArrayList<>();
            for (int i : indices) if (i >= sliderStart && i < pos) list.add(i - sliderStart);
            if (list.isEmpty()) return new int[]{0, 0};

            int indexInSlider = list.get(list.size() - 1);
            int finalLen = 3;
            for (int i = list.size() - 1; i >= 0; i--) {
                int index = list.get(i);
                int len = 3;

                // Could not be a longer match.
                if (finalLen != 3 && fba.getByte(pos + finalLen) != fba.getByte(sliderStart + index + finalLen))
                    continue;

                while (len < bufferSize - 1 &&
                        fba.getByte(pos + len) == fba.getByte(sliderStart + index + len)) len += 1;

                if (len == bufferSize - 1) {
                    finalLen = len;
                    indexInSlider = index;
                    break;
                } else {
                    if (finalLen < len) {
                        finalLen = len;
                        indexInSlider = index;
                    }
                }
            }
            return new int[]{indexInSlider, finalLen};
        } else return new int[]{0, 0};
    }

    private int reverseIndexInQueue(Queue<Integer> queue, int target) {
        int index = 0;
        for (int i : queue) {
            if (i == target) return index;
            index += 1;
        }
        return -1;
    }

    private void deleteTemp() {
        Util.deleteFile(mainTempName);
        Util.deleteFile(disHeadTempName);
        Util.deleteFile(lenHeadTempName);
        Util.deleteFile(flagTempName);
        Util.deleteFile(dlBodyTempName);
    }

    private void optimizeLinkedHashSet(LinkedHashSet<Integer> set, int currentIndex) {
        Iterator<Integer> iterator = set.iterator();
        while (set.size() > 0 && iterator.hasNext() && iterator.next() < currentIndex - dictSize) iterator.remove();
    }

    private void updateInfo(int current, long updateTime) {
        parent.progress.set(current);
        if (timeAccumulator == 19) {
            timeAccumulator = 0;
            double finished = ((double) current) / totalLength;
            double rounded = (double) Math.round(finished * 1000) / 10;
            parent.percentage.set(String.valueOf(rounded));
            int newUpdated = (int) (current - lastUpdateProgress);
            lastUpdateProgress = parent.progress.get();
            int ratio = newUpdated / 1024;
            parent.ratio.set(String.valueOf(ratio));

            long timeUsed = updateTime - startTime;
            parent.timeUsed.set(Util.secondToString((timeUsed + timeOffset) / 1000));
            long expectTime = (totalLength - current) / ratio / 1024;
            parent.timeExpected.set(Util.secondToString(expectTime));

            parent.passedLength.set(Util.sizeToReadable(current));
        } else {
            timeAccumulator += 1;
        }
    }

    /**
     * Compress file into output stream.
     *
     * @param outFile the target output stream.
     * @throws IOException if io error occurs during compression.
     */
    @Override
    public void Compress(OutputStream outFile) throws IOException {
        compressText();

        if (itemCount == 0) {
            Util.fileConcatenate(outFile, new String[]{mainTempName}, 8192);
            outFile.write((byte) 0);
            cmpSize = (int) totalLength + 1;
            deleteTemp();
            return;
        }

        if (parent != null && parent.isInterrupted) {
            deleteTemp();
            return;
        }

        HuffmanCompressor dhc = new HuffmanCompressor(disHeadTempName);
        byte[] dhcMap = dhc.getMap(64);

        HuffmanCompressor lhc = new HuffmanCompressor(lenHeadTempName);
        byte[] lhcMap = lhc.getMap(32);

        HuffmanCompressor fc = new HuffmanCompressor(flagTempName);
        byte[] fcMap = fc.getMap(256);

        HuffmanCompressor mtc = new HuffmanCompressor(mainTempName);
        byte[] mtcMap = mtc.getMap(256);

        byte[] totalMap = new byte[608];
        System.arraycopy(dhcMap, 0, totalMap, 0, 64);
        System.arraycopy(lhcMap, 0, totalMap, 64, 32);
        System.arraycopy(fcMap, 0, totalMap, 96, 256);
        System.arraycopy(mtcMap, 0, totalMap, 352, 256);

        byte[] rlcMain = new MTFTransformByte(totalMap).Transform();

        MapCompressor mc = new MapCompressor(rlcMain);
        byte[] csq = mc.Compress(false);

        outFile.write(csq);

        dhc.SepCompress(outFile);
        int disHeadLen = dhc.getCompressedLength();
        lhc.SepCompress(outFile);
        int lenHeadLen = lhc.getCompressedLength();
        fc.SepCompress(outFile);
        int flagLen = fc.getCompressedLength();

        int dlbLen = Util.fileConcatenate(outFile, new String[]{dlBodyTempName}, 8192);

        mtc.SepCompress(outFile);
        int mainLen = mtc.getCompressedLength();

        deleteTemp();

        int csqLen = csq.length;
        int[] sizes = new int[]{csqLen, disHeadLen, lenHeadLen, flagLen, dlbLen};
        byte[] sizeBlock = LZZ2Util.generateSizeBlock(sizes);
        outFile.write(sizeBlock);
        cmpSize = disHeadLen + lenHeadLen + flagLen + dlbLen + mainLen + csqLen + sizeBlock.length;
    }


    /**
     * Returns the total output size after compressing.
     *
     * @return size after compressed.
     */
    @Override
    public long getCompressedSize() {
        return cmpSize;
    }

    @Override
    public void setParent(Packer parent) {
        this.parent = parent;
    }

    @Override
    public void setThreads(int threads) {
    }

    @Override
    public void setCompressionLevel(int compressionLevel) {
        this.compressionLevel = compressionLevel;
    }

    /**
     * @return [lazy evaluation delay, skip repeat]
     */
    private int[] getCompressionParam() {
        if (compressionLevel == 0) return new int[]{0, 0};
        else if (compressionLevel == 1) return new int[]{1, 0};
        else if (compressionLevel == 2) return new int[]{2, 0};
        else if (compressionLevel == 3) return new int[]{1, 1};
        else if (compressionLevel == 4) return new int[]{2, 1};
        else throw new IndexOutOfBoundsException("Unknown level");
    }
}
