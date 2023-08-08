package trashsoftware.trashMusic.core.volTransform;

public class ExpVolumeTransform extends VolumeTransform {
    
    private final int stdFrames;
    
    public ExpVolumeTransform(int stdFrames) {
        this.stdFrames = stdFrames;
    }
    
    @Override
    public double waveHeightAt(int frameIndex, double original) {
        double factor = (double) frameIndex / stdFrames;
        return 1.0 / Math.exp(factor) * original;
    }
}
