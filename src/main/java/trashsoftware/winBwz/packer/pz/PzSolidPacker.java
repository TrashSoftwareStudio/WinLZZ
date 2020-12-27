/*
 * PZ archive packer.
 *
 * Archive header info:
 * 4 bytes: PZ header
 * 2 bytes: version
 * 2 bytes: info bytes
 * 1 byte: window size
 * 4 bytes: time of creation
 * 4 bytes: CRC32 checksum for context
 * 4 bytes: CRC32 checksum for main text
 * 4 bytes: followed context length (n)
 * 2 bytes: extra field length (m)
 * m bytes: extra field
 * n bytes: context
 */

package trashsoftware.winBwz.packer.pz;

import trashsoftware.winBwz.core.Compressor;
import trashsoftware.winBwz.core.Constants;
import trashsoftware.winBwz.core.bwz.BWZCompressor;
import trashsoftware.winBwz.core.deflate.DeflateCompressor;
import trashsoftware.winBwz.core.fastLzz.FastLzzCompressor;
import trashsoftware.winBwz.core.lzz2.LZZ2Compressor;
import trashsoftware.winBwz.encrypters.Encipher;
import trashsoftware.winBwz.encrypters.bzse.BZSEStreamEncoder;
import trashsoftware.winBwz.encrypters.zse.ZSEFileEncoder;
import trashsoftware.winBwz.utility.Bytes;
import trashsoftware.winBwz.utility.MultipleInputStream;
import trashsoftware.winBwz.utility.Util;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Deque;
import java.util.Timer;

/**
 * The .pz archive packing program.
 * This program packs multiple files and directories into one .pz archive file.
 *
 * @author zbh
 * @since 0.4
 */
public class PzSolidPacker extends PzPacker {

    private PackTimerTask ptt;
    private Timer timer;

    /**
     * Creates a new {@code Packer} instance.
     * <p>
     * All input root files should be under a same path.
     *
     * @param inFiles the input root files.
     */
    public PzSolidPacker(File[] inFiles) {
        super(inFiles);
    }

    public static String getProgramFullVersion() {
        return String.format("%d.%d.%d.%d",
                PzSolidPacker.primaryVersion & 0xff,
                BWZCompressor.VERSION,
                LZZ2Compressor.VERSION,
                FastLzzCompressor.VERSION);
    }

    /**
     * Builds the file structure.
     */
    @Override
    public void build() {
        RootFile rf = new RootFile(inFiles);
        IndexNodeSolid rootNode = new IndexNodeSolid("", rf);
        fileCount = 1;
        indexNodes.add(rootNode);
        buildIndexTree(rf, rootNode);
        totalOrigLengthWrapper.set(totalLength);
    }

    private void buildIndexTree(File file, IndexNodeSolid currentNode) {
        if (isInterrupted) return;
        if (file.isDirectory()) {
            this.file.setValue(file.getAbsolutePath() + "\\");
            File[] sub = file.listFiles();

            int currentCount = fileCount;
            assert sub != null;
            IndexNodeSolid[] tempArr = new IndexNodeSolid[sub.length];
            int arrIndex = 0;
            for (File f : sub) {
                IndexNodeSolid in = new IndexNodeSolid(f.getName(), f);
                tempArr[arrIndex++] = in;
                indexNodes.add(in);
                fileCount += 1;
            }
            currentNode.setChildrenRange(currentCount, fileCount);
            for (int i = 0; i < sub.length; i++) if (!sub[i].isDirectory()) buildIndexTree(sub[i], tempArr[i]);
            for (int i = 0; i < sub.length; i++) if (sub[i].isDirectory()) buildIndexTree(sub[i], tempArr[i]);
        } else {
            long start = totalLength;
            totalLength += file.length();
            currentNode.setSize(start, totalLength);
        }
    }

    @Override
    protected int getSignature() {
        return SIGNATURE;
    }

    @Override
    protected void writeMapToStream(OutputStream headBos, Deque<File> mainList) throws IOException {
        for (IndexNode in : indexNodes) {
            byte[] array = in.toByteArray();
            contextCrc.update(array, 0, array.length);
            headBos.write(array);
            if (!in.isDir()) mainList.addLast(in.getFile());
        }
    }

    private void startTimer() {
        if (timer != null) timer.cancel();
        timer = new Timer();
        ptt = new CompTimerTask();
        timer.scheduleAtFixedRate(ptt, 0, 1000 / Constants.GUI_UPDATES_PER_S);
    }

    private void stopTimer() {
        if (timer != null) timer.cancel();
    }

    protected long writeBody(String outFile,
                             OutputStream bos,
                             Deque<File> inputStreams,
                             int windowSize,
                             int bufferSize) throws Exception {
        String encMainName = outFile + ".enc";

        startTimer();

        try {
            MultipleInputStream mis;
            if (windowSize == 0) {  // no compress
                if (encryptLevel == 0) {
                    mis = new MultipleInputStream(inputStreams, this, false);
                    Util.fileTruncate(mis, bos, totalLength);
                    compressedLength += totalLength;
                } else {
                    Encipher encipher;
                    switch (encryption) {
                        case "zse":
                            mis = new MultipleInputStream(inputStreams, this, false);
                            encipher = new ZSEFileEncoder(mis, password);
                            break;
                        case "bzse":
                            mis = new MultipleInputStream(inputStreams, this, true);
                            encipher = new BZSEStreamEncoder(mis, password);
                            break;
                        default:
                            throw new NoSuchAlgorithmException("No Such Encoding Algorithm");
                    }
                    if (bundle != null) step.setValue(bundle.getString("encrypting"));
//                step.setValue(lanLoader.get(271));
                    progress.set(0);
                    percentage.setValue("0.0");
                    encipher.setParent(this, totalLength);
                    ptt.setProcessor(encipher);
                    encipher.encrypt(bos);
                    compressedLength += encipher.getOutputSize();
                }
            } else if (totalLength != 0) {
                mis = new MultipleInputStream(inputStreams, this, false);
                Compressor mainCompressor;
                switch (alg) {
                    case "lzz2":
                        mainCompressor = new LZZ2Compressor(mis, windowSize, bufferSize, totalLength);
                        break;
                    case "fastLzz":
                        mainCompressor = new FastLzzCompressor(mis, windowSize, bufferSize, totalLength);
                        break;
                    case "bwz":
                        mainCompressor = new BWZCompressor(mis, windowSize);
                        break;
                    case "deflate":
                        mainCompressor = new DeflateCompressor(mis, cmpLevel, totalLength);
                        break;
                    default:
                        throw new NoSuchAlgorithmException("No such algorithm");
                }
                mainCompressor.setPacker(this);
                mainCompressor.setCompressionLevel(cmpLevel);
                mainCompressor.setThreads(threads);
                ptt.setProcessor(mainCompressor);
                if (encryptLevel == 0) {
                    mainCompressor.compress(bos);
                } else {
                    FileOutputStream encFos = new FileOutputStream(encMainName);
                    mainCompressor.compress(encFos);
                    encFos.flush();
                    encFos.close();

                    stopTimer();
                    startTimer();

                    InputStream encMainIs;
                    Encipher encipher;
                    switch (encryption) {
                        case "zse":
                            encMainIs = new FileInputStream(encMainName);
                            encipher = new ZSEFileEncoder(encMainIs, password);
                            break;
                        case "bzse":
                            encMainIs = new BufferedInputStream(new FileInputStream(encMainName));
                            encipher = new BZSEStreamEncoder(encMainIs, password);
                            break;
                        default:
                            throw new NoSuchAlgorithmException("No Such Encoding Algorithm");
                    }
                    if (bundle != null) step.setValue(bundle.getString("encrypting"));
                    file.setValue(outFile);
                    totalOrigLengthWrapper.set(mainCompressor.getOutputSize());
                    progress.set(0);
                    percentage.setValue("0.0");
                    encipher.setParent(this, mainCompressor.getOutputSize());
                    ptt.setProcessor(encipher);
                    encipher.encrypt(bos);
                    encMainIs.close();
                }
                compressedLength += mainCompressor.getOutputSize();
            } else {
                mis = new MultipleInputStream();
            }
//        long crc32 = mis.getCrc32Checksum();
//        byte[] fullBytes = Bytes.longToBytes(crc32);
//        byte[] crc32Checksum = new byte[4];
//        System.arraycopy(fullBytes, 4, crc32Checksum, 0, 4);

            Util.deleteFile(encMainName);
            bos.flush();
            bos.close();
            mis.close();

            if (isInterrupted) {
                Util.deleteFile(outFile);
                return 0;
            }
            return mis.getCrc32Checksum();
        } finally {
            stopTimer();
        }
    }

    /**
     * A class used for recording information of a file in an archive in the time of packing.
     *
     * @author zbh
     * @since 0.4
     */
    static class IndexNodeSolid extends IndexNode {

        /**
         * The start position of this file in the uncompressed main part of archive.
         */
        private long start;

        /**
         * The end position of this file in the uncompressed main part of archive.
         */
        private long end;

        /**
         * Creates a new {@code IndexNode} instance for a directory.
         *
         * @param name directory path.
         */
        IndexNodeSolid(String name, File file) {
            super(name, file);
        }

        /**
         * Returns the length of the file.
         *
         * @return the length of the file.
         */
        public long getSize() {
            return end - start;
        }

        /**
         * Sets up the start and end position of this file in the uncompressed main part of this archive.
         *
         * @param start the start position.
         * @param end   the end position.
         */
        public void setSize(long start, long end) {
            this.start = start;
            this.end = end;
            isDir = false;
        }

        /**
         * Returns the byte array representation of this {@code IndexNode}.
         *
         * @return the byte array representation of this {@code IndexNode}.
         * @throws UnsupportedEncodingException if the file name is too long (>255 bytes) or,
         *                                      the name cannot be encoded.
         */
        public byte[] toByteArray() throws UnsupportedEncodingException {
            byte[] nameBytes = Bytes.stringEncode(name);
            int len = nameBytes.length;
            if (len > 255) throw new UnsupportedEncodingException();
            byte[] result = new byte[len + 18];
            if (isDir) {
                result[0] = 0;
                result[1] = (byte) len;
                System.arraycopy(nameBytes, 0, result, 2, nameBytes.length);
                System.arraycopy(Bytes.longToBytes(childrenRange[0]), 0, result, len + 2, 8);
                System.arraycopy(Bytes.longToBytes(childrenRange[1]), 0, result, len + 10, 8);
            } else {
                result[0] = 1;
                result[1] = (byte) len;
                System.arraycopy(nameBytes, 0, result, 2, nameBytes.length);
                System.arraycopy(Bytes.longToBytes(start), 0, result, len + 2, 8);
                System.arraycopy(Bytes.longToBytes(end), 0, result, len + 10, 8);
            }
            return result;
        }

        @Override
        public String toString() {
            if (isDir) return "Dir(" + name + ", " + Arrays.toString(childrenRange) + ")";
            else return "File(" + name + ": " + start + ", " + end + ")";
        }
    }

    private class CompTimerTask extends PackTimerTask {

        @Override
        public void run() {
            update();
        }

        private synchronized void update() {
            accumulator++;
            if (processor == null) return;
            long position = processor.getInputSize();
            progress.set(position);
            if (accumulator % Constants.GUI_UPDATES_PER_S == 0) {
                updateTimer(position);

                long cmpSize = processor.getOutputSize() + compressedLength;
                cmpLength.set(Util.sizeToReadable(cmpSize));
                double cmpRatio = (double) cmpSize / position;
                double roundedRatio = (double) Math.round(cmpRatio * 1000) / 10;
                currentCmpRatio.set(roundedRatio + "%");
            }
        }
    }
}
