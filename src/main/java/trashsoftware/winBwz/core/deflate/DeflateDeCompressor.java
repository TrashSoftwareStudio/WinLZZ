package trashsoftware.winBwz.core.deflate;

import trashsoftware.winBwz.core.Constants;
import trashsoftware.winBwz.core.DeCompressor;
import trashsoftware.winBwz.packer.PzUnPacker;
import trashsoftware.winBwz.utility.LengthOutputStream;
import trashsoftware.winBwz.utility.Util;

import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.InflaterInputStream;

public class DeflateDeCompressor implements DeCompressor {

    private final String inFile;
    private PzUnPacker unPacker;
    private long timeOffset;
    private long outPosition;

    public DeflateDeCompressor(String inFile) {
        this.inFile = inFile;
    }

    @Override
    public void uncompress(OutputStream out) throws Exception {
        Timer timer = null;
        if (unPacker != null) {
            timeOffset = System.currentTimeMillis() - unPacker.startTime;
            timer = new Timer();
            timer.scheduleAtFixedRate(new DdcTimerTask(), 0, 1000 / Constants.GUI_UPDATES_PER_S);
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

    class DdcTimerTask extends TimerTask {
        private int accumulator;
        private long lastUpdateProgress;

        @Override
        public void run() {
            unPacker.progress.set(outPosition);
            accumulator++;
            if (accumulator % Constants.GUI_UPDATES_PER_S == 0) {  // whole second
                double finished = ((double) outPosition) / unPacker.getTotalOrigSize();
                double rounded = (double) Math.round(finished * 1000) / 10;
                unPacker.percentage.set(String.valueOf(rounded));
                int newUpdated = (int) (outPosition - lastUpdateProgress);
                lastUpdateProgress = unPacker.progress.get();
                int ratio = newUpdated / 1024;
                unPacker.ratio.set(String.valueOf(ratio));

                long timeUsed = accumulator * 1000L / Constants.GUI_UPDATES_PER_S;
                unPacker.timeUsed.set(Util.secondToString((timeUsed + timeOffset) / 1000));
                long expectTime = (unPacker.getTotalOrigSize() - outPosition) / ratio / 1024;
                unPacker.timeExpected.set(Util.secondToString(expectTime));

                unPacker.passedLength.set(Util.sizeToReadable(outPosition));
            }
        }
    }
}
