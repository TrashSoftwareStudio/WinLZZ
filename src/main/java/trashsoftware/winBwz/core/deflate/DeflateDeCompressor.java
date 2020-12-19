package trashsoftware.winBwz.core.deflate;

import trashsoftware.winBwz.core.Constants;
import trashsoftware.winBwz.core.RegularDeCompressor;
import trashsoftware.winBwz.packer.pz.PzUnPacker;
import trashsoftware.winBwz.utility.LengthOutputStream;

import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.zip.InflaterInputStream;

public class DeflateDeCompressor extends RegularDeCompressor {

    private final String inFile;
    private long outPosition;

    public DeflateDeCompressor(String inFile) {
        this.inFile = inFile;
    }

    @Override
    public long getPosition() {
        return outPosition;
    }

    @Override
    public void uncompress(OutputStream out) throws Exception {
        Timer timer = null;
        if (unPacker != null) {
            timeOffset = System.currentTimeMillis() - unPacker.startTime;
            timer = new Timer();
            timer.scheduleAtFixedRate(new DeCompTimerTask(), 0, 1000 / Constants.GUI_UPDATES_PER_S);
        }
        InflaterInputStream dis = new InflaterInputStream(new FileInputStream(inFile));
        LengthOutputStream los = new LengthOutputStream(out);
        byte[] buffer = new byte[DeflateCompressor.bufferSize];
        int read;
        while ((read = dis.read(buffer)) > 0) {
            los.write(buffer, 0, read);
            outPosition = los.getWrittenLength();
        }
        if (timer != null) timer.cancel();
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
