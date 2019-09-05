package trashsoftware.win_bwz.lzz2_plus;

import trashsoftware.win_bwz.bwz.bwt.BWTEncoderByte;
import trashsoftware.win_bwz.bwz.MTFTransformByte;
import trashsoftware.win_bwz.huffman.HuffmanCompressor;
import trashsoftware.win_bwz.huffman.MapCompressor.MapCompressor;
import trashsoftware.win_bwz.lzz2.LZZ2Compressor;
import trashsoftware.win_bwz.longHuffman.LongHuffmanCompressorRam;
import trashsoftware.win_bwz.utility.MultipleInputStream;
import trashsoftware.win_bwz.utility.Util;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Lzz2PlusCompressor extends LZZ2Compressor {

    private static final int PART_LENGTH = 65536;

    /**
     * Constructor of a new {@code LZZ2Compressor} instance.
     *
     * @param inFile     name of file to compress.
     * @param windowSize total sliding window size.
     * @param bufferSize size of look ahead buffer.
     * @throws IOException if error occurs during file reading or writing.
     */
    public Lzz2PlusCompressor(String inFile, int windowSize, int bufferSize) throws IOException {
        super(inFile, windowSize, bufferSize);
    }

    /**
     * Constructor of a new {@code LZZ2Compressor} instance.
     *
     * @param mis         the input stream
     * @param windowSize  total sliding window size.
     * @param bufferSize  size of look ahead buffer.
     * @param totalLength the total length of the files to be compressed
     */
    public Lzz2PlusCompressor(MultipleInputStream mis, int windowSize, int bufferSize, long totalLength) {
        super(mis, windowSize, bufferSize, totalLength);
    }

    @Override
    public void compress(OutputStream outFile) throws IOException {
        compressText();

        if (isNotCompressible(outFile)) return;

        HuffmanCompressor dhc = new HuffmanCompressor(disHeadTempName);
        byte[] dhcMap = dhc.getMap(64);

        HuffmanCompressor lhc = new HuffmanCompressor(lenHeadTempName);
        byte[] lhcMap = lhc.getMap(32);

        HuffmanCompressor fc = new HuffmanCompressor(flagTempName);
        byte[] fcMap = fc.getMap(256);

        byte[] totalMap = new byte[352];
        System.arraycopy(dhcMap, 0, totalMap, 0, 64);
        System.arraycopy(lhcMap, 0, totalMap, 64, 32);
        System.arraycopy(fcMap, 0, totalMap, 96, 256);

        byte[] rlcMain = new MTFTransformByte(totalMap).Transform(18);

        MapCompressor mc = new MapCompressor(rlcMain);
        byte[] csq = mc.Compress(false);

        outFile.write(csq);

        dhc.SepCompress(outFile);
        int disHeadLen = dhc.getCompressedLength();
        lhc.SepCompress(outFile);
        int lenHeadLen = lhc.getCompressedLength();
        fc.SepCompress(outFile);
        int flagLen = fc.getCompressedLength();

        long dlbLen = Util.fileConcatenate(outFile, new String[]{dlBodyTempName}, 8192);

        BufferedInputStream mis = new BufferedInputStream(new FileInputStream(mainTempName));
        long mainLen = separateHuffmanCompress(mis, outFile);
        mis.close();

        deleteTemp();

        long[] sizes = new long[]{disHeadLen, lenHeadLen, flagLen, dlbLen};
        byte[] sizeBlock = Util.generateSizeBlock(sizes);
        outFile.write(sizeBlock);
        cmpSize = disHeadLen + lenHeadLen + flagLen + dlbLen + mainLen + sizeBlock.length;
    }

    private long separateHuffmanCompress(InputStream inFile, OutputStream outFile) throws IOException {
        byte[] buffer = new byte[PART_LENGTH];
        long length = 0;
        int read;
        List<byte[]> heads = new ArrayList<>();
        while ((read = inFile.read(buffer)) != -1) {
            int[] intBuffer = new int[read];
            for (int i = 0; i < read; i++) {
                intBuffer[i] = buffer[i] & 0xff;
            }
            LongHuffmanCompressorRam hc = new LongHuffmanCompressorRam(intBuffer, 257, 256);
            byte[] map = hc.getMap(257);
//            System.out.println(Arrays.toString(map));
            heads.add(map);

//            System.out.println(Arrays.toString(cmpMap));
            byte[] result = hc.compress();
//            int mapLen = cmpMap.length;

            length += result.length;

            outFile.write(result);
        }
        byte[] totalMap = new byte[heads.size() * 257];
        for (int i = 0; i < heads.size(); i++) {
            byte[] block = heads.get(i);
            System.arraycopy(block, 0, totalMap, i * 257, 257);
        }
        System.out.println(Arrays.toString(totalMap));
        BWTEncoderByte beb = new BWTEncoderByte(totalMap);
        byte[] bebMap = beb.Transform();
        byte[] mapMtf = new MTFTransformByte(bebMap).Transform(18);
        byte[] cmpMap = new MapCompressor(mapMtf).Compress(false);
        outFile.write(cmpMap);
        length += cmpMap.length;

        return length;
    }
}
