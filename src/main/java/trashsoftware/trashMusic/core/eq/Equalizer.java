package trashsoftware.trashMusic.core.eq;

import java.util.Arrays;

public class Equalizer {

    public static final Equalizer PLAIN = new Equalizer();

    /**
     * 31.25, 62.5, 125, 250, 500, 1K, 2K, 4K, 8K, 16K
     */
    private final double[] eq;

    public Equalizer(double[] eq) {
        this.eq = eq;
    }

    public Equalizer() {
        this(new double[]{1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0});
    }

    public double volumeMultiplier(double freq) {
        return 1.0;
    }

    @Override
    public String toString() {
        return "FreqEqualizer{" +
                "eq=" + Arrays.toString(eq) +
                '}';
    }
}
