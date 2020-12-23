package trashsoftware.winBwz.core.deflate;

import trashsoftware.winBwz.core.RegularDeCompressor;
import trashsoftware.winBwz.packer.pz.PzUnPacker;
import trashsoftware.winBwz.utility.LengthOutputStream;

import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.zip.InflaterInputStream;

public class DeflateDeCompressor extends RegularDeCompressor {

    private final String inFile;

    public DeflateDeCompressor(String inFile) {
        this.inFile = inFile;
    }

    @Override
    public void uncompress(OutputStream out) throws Exception {
        InflaterInputStream dis = new InflaterInputStream(new FileInputStream(inFile));
        LengthOutputStream los = new LengthOutputStream(out);
        byte[] buffer = new byte[DeflateCompressor.bufferSize];
        int read;
        while ((read = dis.read(buffer)) > 0) {
            los.write(buffer, 0, read);
            outPosition = los.getWrittenLength();
        }
        los.flush();
        los.close();
        dis.close();
    }

    @Override
    public void setUnPacker(PzUnPacker unPacker) {
        this.unPacker = unPacker;
    }

    @Override
    public void deleteCache() {

    }

    @Override
    public void setThreads(int threads) {

    }
}
