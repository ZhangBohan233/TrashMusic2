package trashsoftware.trashMusic.core;

import trashsoftware.trashMusic.core.wav.WavFile;

import java.util.Arrays;

public class Drumbeat {

    public static final Drumbeat BASS_DRUM = new Drumbeat(100.0,
            new double[]{31, 41, 53, 83, 109},
            new double[]{50, 50, 50, 50, 50});
    public static final Drumbeat SIDE_DRUM = new Drumbeat(80.0,
            new double[]{109, 173, 233, 293, 367, 433},
            new double[]{50, 60, 70, 60, 40, 20});
    public static final Drumbeat CYMBAL = new Drumbeat(100.0,
            new double[]{173, 199, 269, 331, 6831, 7897, 9353, 11893},
            new double[]{50, 50, 50, 50, 10, 10, 10, 10});

    private final double durationMs;
    private final double actualDurationMs;
    private final int[] wave;

    public Drumbeat(double durationMs, double[] freqArray, double[] volumeArray) {
        this.durationMs = durationMs;
        wave = WavFile.makeSimpleWaveData(freqArray[0], durationMs, 16, WavFile.DEFAULT_SAMPLE_RATE, volumeArray[0] / volumeArray.length);
        actualDurationMs = (double) wave.length / WavFile.DEFAULT_SAMPLE_RATE;
        for (int i = 1; i < freqArray.length; ++i) {
            int[] overlapWave = WavFile.makeSimpleWaveData(freqArray[i], durationMs, 16, WavFile.DEFAULT_SAMPLE_RATE, volumeArray[i] / volumeArray.length);
            for (int f = 0; f < wave.length; ++f) {
                wave[f] += overlapWave[f];
            }
        }
        System.out.println(Arrays.toString(wave));
    }

    public int[] getWave() {
        return wave;
    }

    public double getActualDurationMs() {
        return actualDurationMs;
    }
}
