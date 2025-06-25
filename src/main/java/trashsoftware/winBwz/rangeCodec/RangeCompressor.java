package trashsoftware.winBwz.rangeCodec;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class RangeCompressor extends AbstractRangeCompressor {
    
    protected String inFile;
    
    public RangeCompressor(String inFile, int eofSig) {
        super(eofSig);
        
        this.inFile = inFile;
    }
    
    public void compress(OutputStream outFile) throws IOException {
        out = outFile;
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(inFile))) {
            int b;
            while ((b = bis.read()) != -1) {
                encodeSymbol(b);
                freq.increment(b);
            }
            finish();
        } 
    }
}
