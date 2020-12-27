package trashsoftware.winBwz.core;

import trashsoftware.winBwz.packer.pz.PzUnPacker;
import trashsoftware.winBwz.utility.Util;

import java.util.TimerTask;

public abstract class RegularDeCompressor implements DeCompressor {

    protected PzUnPacker unPacker;
    protected long outPosition;

    @Override
    public long getOutputSize() {
        return outPosition;
    }

    @Override
    public void setUnPacker(PzUnPacker unPacker) {
        this.unPacker = unPacker;
    }
}
