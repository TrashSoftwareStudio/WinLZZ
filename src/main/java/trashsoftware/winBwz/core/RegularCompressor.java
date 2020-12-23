package trashsoftware.winBwz.core;

import trashsoftware.winBwz.packer.pz.PzPacker;

public abstract class RegularCompressor implements Compressor {

    protected PzPacker packer;
    protected long totalLength;

    /**
     * Time used by any launching process before the actual compress process starts.
     */
    protected long timeOffset;

    public RegularCompressor(long totalLength) {
        this.totalLength = totalLength;
    }

    @Override
    public void setPacker(PzPacker packer) {
        this.packer = packer;
    }
}
