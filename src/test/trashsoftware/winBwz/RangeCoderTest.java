package trashsoftware.winBwz;

import trashsoftware.winBwz.rangeCodec.freq.AdaptiveFrequencyTable;
import trashsoftware.winBwz.rangeCodec.freq.FrequencyTable;
import trashsoftware.winBwz.rangeCodec.RangeDecoder;
import trashsoftware.winBwz.rangeCodec.RangeEncoder;
import trashsoftware.winBwz.utility.Util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RangeCoderTest {

    public static void main(String[] args) throws IOException {
        String baseFileName = "cmpFiles.tar";
        baseFileName = "dsCtrl.txt";
        
        String origExt = baseFileName.substring(baseFileName.lastIndexOf("."));
        
        long t0 = System.currentTimeMillis();
//        FileOutputStream fosHuf = new FileOutputStream(baseFileName + ".huf");
//        
//        HuffmanCompressor hc = new HuffmanCompressor(baseFileName);
//        int[] map = hc.getMap(256);
//        hc.SepCompress(fosHuf);
//        fosHuf.flush();
//        fosHuf.close();
        
        long t1 = System.currentTimeMillis();
        System.out.println(t1 - t0);

        BufferedOutputStream rangeOs = new BufferedOutputStream(Files.newOutputStream(Paths.get(baseFileName + ".rng")));
        byte[] inputBytes = Util.readFileToArray(new File(baseFileName));
        
        FrequencyTable ftEnc = new AdaptiveFrequencyTable(257);
        System.out.println(ftEnc);
        RangeEncoder re = new RangeEncoder(rangeOs);
        for (byte b : inputBytes) {
            re.encodeSymbol(b & 0xff, ftEnc);
            ftEnc.increment(b & 0xff);
        }
        re.encodeSymbol(256, ftEnc);
        re.finish();
        rangeOs.flush();
        rangeOs.close();
        
        long t2 = System.currentTimeMillis();
        System.out.println(t2 - t1);
        
        FrequencyTable ftDec = new AdaptiveFrequencyTable(257);

        byte[] rngCmpBytes = Util.readFileToArray(new File(baseFileName + ".rng"));
        System.out.println("Compressed size: " + rngCmpBytes.length);
        ByteArrayInputStream bai = new ByteArrayInputStream(rngCmpBytes);
        BufferedOutputStream rngRec = new BufferedOutputStream(Files.newOutputStream(Paths.get(baseFileName + ".rng" + origExt)));
        RangeDecoder rd = new RangeDecoder(bai);
        rd.initialize();
        int count = 0;
        while (true) {
            try {
                int sym = rd.decodeSymbol(ftDec);
                if (sym == 256) {
                    System.out.println("EOF");
                    break;  // EOF marker
                }
                ftDec.increment(sym);
                rngRec.write(sym);
                count++;
            } catch (EOFException eof) {
                eof.printStackTrace();
                break;
            }
        }
        rngRec.flush();
        rngRec.close();

        System.out.println("Out size: " + count);

        long t3 = System.currentTimeMillis();
        System.out.println(t3 - t2);
    }
}
