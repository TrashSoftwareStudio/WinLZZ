package trashsoftware.winBwz.packer.pz;

import trashsoftware.winBwz.core.Constants;
import trashsoftware.winBwz.core.DeCompressor;
import trashsoftware.winBwz.core.bwz.BWZDeCompressor;
import trashsoftware.winBwz.encrypters.Decipher;
import trashsoftware.winBwz.encrypters.bzse.BZSEStreamDecoder;
import trashsoftware.winBwz.encrypters.zse.ZSEFileDecoder;
import trashsoftware.winBwz.packer.CatalogNode;
import trashsoftware.winBwz.utility.Bytes;
import trashsoftware.winBwz.utility.IOInterruptedException;
import trashsoftware.winBwz.utility.Security;
import trashsoftware.winBwz.utility.Util;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.zip.CRC32;

public class PzSolidUnPacker extends PzUnPacker {
    /**
     * List of {@code IndexNodeUnp}'s.
     * <p>
     * This list is order-sensitive. Each node represents an actual file except the root node.
     */
    private final List<IndexNodeUnp> indexNodes = new ArrayList<>();
    /**
     * The root {@code ContextNode} of the archive.
     * <p>
     * This node does not represent an actual directory.
     */
    protected PzSolidCatalogNode rootNode;

    /**
     * Creates a new UnPacker instance, with <code>packName</code> as the input archive name.
     *
     * @param packName       the name/path of the input archive file.
     * @param resourceBundle resources bundle
     */
    public PzSolidUnPacker(String packName, ResourceBundle resourceBundle) {
        super(packName, resourceBundle);
    }

    /**
     * Reads the context map and extra field from archive file to this {@code UnPacker} instance.
     * <p>
     * The context map records the path structure of the file stored in archive.
     * The context map is compressed by some algorithm.
     *
     * @throws Exception if any error occurs during reading.
     */
    @Override
    public void readFileStructure() throws Exception {
        unCompressMap();
        readStructure();
        deleteTemp();

        rootNode = new PzSolidCatalogNode(indexNodes.get(0).getName(), null);
        buildContextTree(rootNode, indexNodes.get(0));
        interpretExtraField();
    }

    @Override
    protected void readStructure() throws IOException {
        BufferedInputStream mapInputStream = new BufferedInputStream(new FileInputStream(mapName));
        CRC32 contextChecker = new CRC32();

        byte[] flag = new byte[2];
        byte[] buffer8 = new byte[8];

        while (mapInputStream.read(flag) == 2) {
            contextChecker.update(flag);
            boolean isDir = (flag[0] & 0xff) == 0;
            int nameLen = flag[1] & 0xff;
            byte[] nameBytes = new byte[nameLen];
            if (mapInputStream.read(nameBytes) != nameLen) throw new IOException("Error occurs while reading");
            contextChecker.update(nameBytes);
            String name = Bytes.stringDecode(nameBytes);
            IndexNodeUnp inu = new IndexNodeUnp(name);

            if (mapInputStream.read(buffer8) != 8) throw new IOException("Error occurs while reading");
            contextChecker.update(buffer8);
            long start = Bytes.bytesToLong(buffer8);
            if (mapInputStream.read(buffer8) != 8) throw new IOException("Error occurs while reading");
            contextChecker.update(buffer8);
            long end = Bytes.bytesToLong(buffer8);
            if (isDir) {
                inu.setChildrenRange((int) start, (int) end);
                dirCount += 1;
            } else {
                inu.setScale(start, end);
                origSize = end;
                fileCount += 1;
            }
            indexNodes.add(inu);
        }
        mapInputStream.close();
        totalProgress.set(origSize);

        if (contextChecker.getValue() != crc32Context) {
//            throw new ChecksumDoesNotMatchException("Context damaged");
            System.err.println("CRC32 Checksum does not match");
        }
    }

    private void buildContextTree(PzSolidCatalogNode node, IndexNodeUnp inu) {
        if (inu.isDir()) {
            List<IndexNodeUnp> children = indexNodes.subList(inu.getChildrenRange()[0], inu.getChildrenRange()[1]);
            String path = node.getPath();
            for (IndexNodeUnp n : children) {
                PzSolidCatalogNode cn = new PzSolidCatalogNode(path + File.separator + n.getName(), node);
                node.addChild(cn);
            }
            for (int i = 0; i < children.size(); i++)
                buildContextTree((PzSolidCatalogNode) node.getChildren().get(i), children.get(i));
        } else {
            node.setLocation(inu.getStart(), inu.getEnd());
        }
    }

    /**
     * Returns the root node.
     *
     * @return {@code rootNode} the root node.
     */
    @Override
    public PzSolidCatalogNode getRootNode() {
        return rootNode;
    }

    /**
     * Extracts and uncompress all files and directories which are under <code>cn</code> to the path
     * <code>targetDir</code>, recursively.
     * <p>
     * * Original path structure will be kept.
     *
     * @param targetDir path to extract contents.
     * @param cn        the ContextNode to start extraction.
     * @throws Exception if any failure happens during extraction and decompression.
     */
    @Override
    public void unCompressFrom(String targetDir, CatalogNode cn) throws Exception {
        PzSolidCatalogNode pcn = (PzSolidCatalogNode) cn;
        isTest = false;
        startTime = System.currentTimeMillis();
        progress.set(1);
        unCompressMain();

        if (isInterrupted) {
            failInfo = bundle.getString("operationInterrupted");
            throw new IOInterruptedException();
        }

        step.setValue(bundle.getString("verifying"));
        long currentCRC32 = Security.generateCRC32(tempName);
        if (currentCRC32 != crc32Checksum) {
            failInfo = bundle.getString("fileDamaged");
//            throw new ChecksumDoesNotMatchException("CRC32 Checksum does not match");
            System.err.println("CRC32 Checksum does not match");
        }

        RandomAccessFile raf = new RandomAccessFile(tempName, "r");
        String dirOffset;
        if (!pcn.getPath().contains(File.separator)) dirOffset = "";
        else dirOffset = pcn.getPath().substring(0, pcn.getPath().lastIndexOf(File.separator));
        step.setValue(bundle.getString("extracting"));
        traversalExtract(targetDir, pcn, raf, dirOffset);
        raf.close();
    }

    /**
     * Returns whether the compressed file is undamaged.
     *
     * @return true if the file can be uncompress successfully and the uncompressed file is the same as the
     * original file
     */
    @Override
    public boolean testPack() {
        isTest = true;
        startTime = System.currentTimeMillis();
        try {
            unCompressMain();
            if (isInterrupted) return false;
            step.setValue(bundle.getString("verifying"));
            long currentCRC32 = Security.generateCRC32(tempName);
            return currentCRC32 == crc32Checksum;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void traversalExtract(String targetDir, PzSolidCatalogNode cn, RandomAccessFile raf, String dirOffset)
            throws IOException {
        String path = targetDir + File.separator + cn.getPath().substring(dirOffset.length());
        File f = new File(path);
        if (cn.isDir()) {
            if (!f.exists()) {
                if (!f.mkdirs()) System.out.println("Failed to create directory");
            }
            for (CatalogNode scn : cn.getChildren())
                traversalExtract(targetDir, (PzSolidCatalogNode) scn, raf, dirOffset);
        } else {
            currentFile.setValue(cn.getPath().substring(1));
            long[] location = cn.getLocation();
            raf.seek(location[0]);
            Util.fileTruncate(raf, path, 8192, location[1] - location[0]);
        }
    }

    private void unCompressMainPureBWZ(UnpTimerTask utt) throws Exception {
        String name;
        long startPos;
        if (isSeparated) {
            step.setValue(bundle.getString("merging"));
            combineName = packName + ".comb";
            name = combineName;
            try {
                combineSplitBwz();
            } catch (IOInterruptedException e) {
                Util.deleteFile(cmpTempName);
                Util.deleteFile(combineName);
                return;
            }
            startPos = 0;
        } else {
            name = packName;
            startPos = archiveLength - cmpMainLength;
        }
        if (isTest) step.setValue(bundle.getString("testing"));
        else step.setValue(bundle.getString("uncIng"));

        BWZDeCompressor mainDec = new BWZDeCompressor(name, windowSize, startPos);
        mainDec.setUnPacker(this);
        mainDec.setThreads(threadNumber);
        utt.setDeCompressor(mainDec);
        FileOutputStream mainFos = new FileOutputStream(tempName);
        try {
            mainDec.uncompress(mainFos);
            mainFos.close();
        } catch (Exception e) {
            mainDec.deleteCache();
            throw e;
        } finally {
            bis.close();
            Util.deleteFile(combineName);
            Util.deleteFile(cmpTempName);
            deleteTemp();
        }

//        if (isInterrupted) return;
//        long currentCRC32 = Security.generateCRC32(tempName);
//        if (currentCRC32 != crc32Checksum) throw new ChecksumDoesNotMatchException("CRC32 Checksum does not match");
    }

    protected boolean isUnCompressed() {
        File f = new File(tempName);
        return f.exists() && f.length() == origSize;
    }

    private void combineSplitBwz() throws IOException {
        Util.fileTruncate(bis, combineName, 8192, cmpMainLength);
    }

    private void unCompressMain() throws Exception {
        UnpTimerTask utt = new DeCompTimerTask();
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(utt, 0, 1000 / Constants.GUI_UPDATES_PER_S);

        try {
            if (origSize == 0) {
                File f = new File(tempName);
                if (!f.createNewFile()) System.out.println("Creation failed");
            } else if (!isUnCompressed()) {
                if (encryptLevel != 0) {
                    step.setValue(bundle.getString("decrypting"));
                    Decipher decipher;
                    FileOutputStream fos = new FileOutputStream(cmpTempName);
                    switch (encryption) {
                        case "zse":
                            decipher = new ZSEFileDecoder(bis, password, cmpMainLength);
                            break;
                        case "bzse":
                            decipher = new BZSEStreamDecoder(bis, password, cmpMainLength);
                            break;
                        default:
                            throw new NoSuchAlgorithmException("No such algorithm");
                    }
                    decipher.setParent(this);
                    decipher.decrypt(fos);
                    fos.flush();
                    fos.close();
                } else if (windowSize != 0 && alg.equals("bwz")) {
                    unCompressMainPureBWZ(utt);
                    return;
                } else {
                    try {
                        Util.fileTruncate(bis, cmpTempName, 8192, cmpMainLength);
                    } catch (IOInterruptedException e) {
                        Util.deleteFile(cmpTempName);
                        return;
                    }
                }

                if (windowSize == 0) {
                    tempName = cmpTempName;
                } else {
                    if (isTest) step.setValue(bundle.getString("testing"));
                    else step.setValue(bundle.getString("uncIng"));

                    DeCompressor mainDec;
                    mainDec = getDeCompressor(cmpTempName, windowSize);
                    mainDec.setUnPacker(this);
                    mainDec.setThreads(threadNumber);
                    utt.setDeCompressor(mainDec);
                    FileOutputStream mainFos = new FileOutputStream(tempName);
                    try {
                        mainDec.uncompress(mainFos);
                        mainFos.close();
                    } catch (Exception e) {
                        mainDec.deleteCache();
                        throw e;
                    } finally {
                        Util.deleteFile(cmpTempName);
                        bis.close();
                    }
                }
            }
        } finally {
            timer.cancel();
        }
    }

    private class DeCompTimerTask extends UnpTimerTask {
        @Override
        public void run() {
            update();
        }

        private synchronized void update() {
            accumulator++;
            if (deCompressor == null) return;
            long position = deCompressor.getUncompressedLength();
            progress.set(position);
            if (accumulator % Constants.GUI_UPDATES_PER_S == 0) {
                updateTimer(position);


            }
        }
    }
}
