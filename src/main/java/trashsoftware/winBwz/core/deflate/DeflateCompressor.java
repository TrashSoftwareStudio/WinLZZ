package trashsoftware.winBwz.core.deflate;

import trashsoftware.winBwz.core.RegularCompressor;
import trashsoftware.winBwz.packer.pz.PzPacker;
import trashsoftware.winBwz.utility.LengthOutputStream;

import java.io.*;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public class DeflateCompressor extends RegularCompressor {
    static final int bufferSize = 8192;
    //    private String inFile;
    private final InputStream is;
    private int level;
    private long compressedSize;
    private long position;

    public DeflateCompressor(String inFile, int presetLevel) throws IOException {
        this(new FileInputStream(inFile), presetLevel, new File(inFile).length());
    }

    public DeflateCompressor(InputStream inputStream, int presetLevel, long totalLength) {
        super(totalLength);
        this.level = presetLevel;
        this.is = inputStream;
    }

    @Override
    public long getInputSize() {
        return position;
    }

    @Override
    public void compress(OutputStream out) throws Exception {
        LengthOutputStream los = new LengthOutputStream(out);
        los.notCloseOut();  // avoid 'out' being closed by dos.close
        Deflater deflater = new Deflater(level);
        DeflaterOutputStream dos = new DeflaterOutputStream(los, deflater);
        byte[] buffer = new byte[bufferSize];
        int read;
        while ((read = is.read(buffer)) > 0) {
            dos.write(buffer, 0, read);
            position += read;
            compressedSize = los.getWrittenLength();
        }
        is.close();
        dos.flush();
        dos.close();
        compressedSize = los.getWrittenLength();
    }

    @Override
    public void setPacker(PzPacker packer) {
        this.packer = packer;
    }

    @Override
    public void setThreads(int threads) {

    }

    @Override
    public void setCompressionLevel(int level) {
        this.level = level;
    }

    @Override
    public long getOutputSize() {
        return compressedSize;
    }
}
