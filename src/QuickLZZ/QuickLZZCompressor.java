package QuickLZZ;

import Huffman.HuffmanCompressor;
import Huffman.MapCompressor.MapCompressor;
import Huffman.RLCCoder.RLCCoder;
import Interface.Compressor;
import LZZ2.Util.LZZ2Util;
import Packer.Packer;
import Utility.Bytes;
import Utility.FileBitOutputStream;
import Utility.FileInputBufferArray;
import Utility.Util;

import java.io.*;

public class QuickLZZCompressor implements Compressor {

    private InputStream sis;

    private long totalLength;

    /**
     * Size of sliding window.
     */
    private int windowSize;

    private int dictSize;

    private String mainTempName, lenHeadTempName, disHeadTempName, flagTempName, dlBodyTempName;

    private int cmpSize = 0;

    private int itemCount = 0;

    private Packer parent;

    private int timeAccumulator = 0;

    private long lastUpdateProgress = 0;

    private long startTime;

    private long timeOffset;

    /**
     * Constructor of a new LZZ2Compressor Object.
     *
     * @param inFile     name of file to compress.
     * @param windowSize total sliding window size.
     * @throws IOException if error occurs during file reading or writing.
     */
    public QuickLZZCompressor(String inFile, int windowSize) throws IOException {
        this.windowSize = windowSize;
        this.dictSize = windowSize - 3;

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

        QuickHashSlider slider = new QuickHashSlider();

        int front = 3;  // The frontier of buffer.
        int position = 0;  // The processing index. i.e. the border of buffer and slider.
        int lastRead = 0;  // The number of bytes proceed in last round.
        int sliderSize = 0;

        BufferedOutputStream mainFos = new BufferedOutputStream(new FileOutputStream(mainTempName));
        BufferedOutputStream disFos = new BufferedOutputStream(new FileOutputStream(disHeadTempName));
        BufferedOutputStream lenFos = new BufferedOutputStream(new FileOutputStream(lenHeadTempName));
        FileBitOutputStream flagFos = new FileBitOutputStream(new BufferedOutputStream(new FileOutputStream(flagTempName)));
        FileBitOutputStream dlbFos = new FileBitOutputStream(new BufferedOutputStream(new FileOutputStream(dlBodyTempName)));

        long lastCheckTime = System.currentTimeMillis();
        startTime = lastCheckTime;
        if (parent != null) {
            timeOffset = lastCheckTime - parent.startTime;
        }
        long currentTime;

        while (true) {
            // Push buffer.
            int newLoad = lastRead;

            if (front + newLoad >= totalLength) {
                newLoad = (int) totalLength - front;
            }

            front += newLoad;
            sliderSize += lastRead;

            fillHashSlider(fba, position, lastRead, slider);

            position += lastRead;
            slider.clearOutRanged(dictSize);
            if (sliderSize > dictSize) sliderSize = dictSize;

            if (totalLength - position < 3) {
                for (int i = position; i < totalLength; i++) {
                    flagFos.write('0');
                    mainFos.write(fba.getByte(i));
                }
                break;
            }

            int[] search = search(fba, position, slider);

            if (search[1] == 0) {
                // Not a match
                flagFos.write('0');
                mainFos.write(fba.getByte(position));
                lastRead = 1;
            } else {
                // A match.
                lastRead = search[1] * 3;

                flagFos.write('1');
                int distanceInt = position - search[0];
                int lengthInt = search[1];
//                System.out.print(distanceInt + " " + lengthInt + ", ");

                LZZ2Util.addDistance(distanceInt, 0, disFos, dlbFos);  // Distance first.
                LZZ2Util.addLength(lengthInt, 0, lenFos, dlbFos);
                itemCount += 1;
            }
            if (parent != null && parent.isInterrupted) {
                break;
            }
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

    private void fillHashSlider(FileInputBufferArray fba, int front, int newLoad, QuickHashSlider slider) {
        for (int i = front; i < front + newLoad; i++) {
            if (i > fba.length() - 3) {
                break;
            }
            QuickHashNode hn = new QuickHashNode(fba.getByte(i), fba.getByte(i + 1), fba.getByte(i + 2));
            slider.put(hn, i);
        }
    }

    private int[] search(FileInputBufferArray fba, int pos, QuickHashSlider slider) {
        return longestMatch(fba, pos, slider);
    }

    private int[] longestMatch(FileInputBufferArray fba, int pos, QuickHashSlider slider) {

        QuickHashNode hn = new QuickHashNode(fba.getByte(pos), fba.getByte(pos + 1), fba.getByte(pos + 2));
        Integer index = slider.get(hn);

        if (index == null) {
            return new int[]{0, 0};
        }

        int length = 1;
        int i = 3;

        while (pos + i + 3 < totalLength && length < 286) {
            QuickHashNode qhn = new QuickHashNode(fba.getByte(pos + i), fba.getByte(pos + i + 1), fba.getByte(pos + i + 2));
            Integer follow = slider.get(qhn);
            if (follow == null) {
                break;
            } else if (follow == index + i) {
                i += 3;
                length += 1;
            } else {
                break;
            }
        }

        return new int[]{index, length};
    }

    private void deleteTemp() {
        Util.deleteFile(mainTempName);
        Util.deleteFile(disHeadTempName);
        Util.deleteFile(lenHeadTempName);
        Util.deleteFile(flagTempName);
        Util.deleteFile(dlBodyTempName);
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

        RLCCoder rlc = new RLCCoder(totalMap);
        rlc.Encode();
        byte[] rlcMain = rlc.getMainResult();
        String rlcBits = rlc.getRlcBits();

        byte[] rlcBytes = Bytes.stringToBytesFull(rlcBits);

        MapCompressor mc = new MapCompressor(rlcMain);
        byte[] csq = mc.Compress(true);

        outFile.write(csq);
        outFile.write(rlcBytes);

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
        int rlcByteLen = rlcBytes.length;
        int[] sizes = new int[]{csqLen, rlcByteLen, disHeadLen, lenHeadLen, flagLen, dlbLen};
        byte[] sizeBlock = LZZ2Util.generateSizeBlock(sizes);
        outFile.write(sizeBlock);
        cmpSize = disHeadLen + lenHeadLen + flagLen + dlbLen + mainLen + csqLen + rlcByteLen + sizeBlock.length;
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
    public void setCompressionLevel(int compressionLevel) {
    }

    @Override
    public void setThreads(int threads) {
    }
}
