package trashsoftware.trashMusic.core.volTransform;

public class LinearVolumeTransform extends VolumeTransform {
    
    protected int periodFrames;
    
    public LinearVolumeTransform(int periodFrames) {
        this.periodFrames = periodFrames;
    }

    @Override
    public double waveHeightAt(int frameIndex, double original) {
        double ratio = 1.0 - (double) frameIndex / periodFrames;
        return original * ratio;
    }
}
