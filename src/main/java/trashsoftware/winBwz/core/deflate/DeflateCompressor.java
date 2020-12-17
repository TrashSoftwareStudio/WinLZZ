package trashsoftware.winBwz.core.deflate;

import trashsoftware.winBwz.core.Compressor;
import trashsoftware.winBwz.core.Constants;
import trashsoftware.winBwz.packer.PzPacker;
import trashsoftware.winBwz.utility.LengthOutputStream;
import trashsoftware.winBwz.utility.Util;

import java.io.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public class DeflateCompressor implements Compressor {
    static final int bufferSize = 8192;
    //    private String inFile;
    private final InputStream is;
    private int level;
    private long compressedSize;
    private PzPacker packer;
    private long timeOffset;
    private final long totalLength;
    private long position;

    public DeflateCompressor(String inFile, int presetLevel) throws IOException {
        this(new FileInputStream(inFile), presetLevel, new File(inFile).length());
    }

    public DeflateCompressor(InputStream inputStream, int presetLevel, long totalLength) {
        this.level = presetLevel;
        this.is = inputStream;
        this.totalLength = totalLength;
    }

    @Override
    public void compress(OutputStream out) throws Exception {
        Timer timer = null;
        if (packer != null) {
            timeOffset = System.currentTimeMillis() - packer.startTime;
            timer = new Timer();
            timer.scheduleAtFixedRate(new DefTimerTask(), 0, 1000 / Constants.GUI_UPDATES_PER_S);
        }
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
        if (timer != null) timer.cancel();
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
    public long getCompressedSize() {
        return compressedSize;
    }

    class DefTimerTask extends TimerTask {
        private int accumulator;

        private long lastUpdateProgress;

        @Override
        public void run() {
            packer.progress.set(position);
            accumulator++;
            if (accumulator % Constants.GUI_UPDATES_PER_S == 0) {  // whole second
                double finished = ((double) position) / totalLength;
                double rounded = (double) Math.round(finished * 1000) / 10;
                packer.percentage.set(String.valueOf(rounded));
                int newUpdated = (int) (position - lastUpdateProgress);
                lastUpdateProgress = position;
                int ratio = newUpdated / 1024;
                packer.ratio.set(String.valueOf(ratio));

                long timeUsed = accumulator * 1000L / Constants.GUI_UPDATES_PER_S;
                packer.timeUsed.set(Util.secondToString((timeUsed + timeOffset) / 1000));
                long expectTime = (totalLength - position) / ratio / 1024;
                packer.timeExpected.set(Util.secondToString(expectTime));

                packer.passedLength.set(Util.sizeToReadable(position));
            }
        }
    }
}
