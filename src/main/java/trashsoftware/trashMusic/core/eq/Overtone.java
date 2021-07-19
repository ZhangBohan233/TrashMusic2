package trashsoftware.trashMusic.core.eq;

import java.util.Map;

public class Overtone {

    public static final Overtone PLAIN = new Overtone();
    public static final Overtone MID = new Overtone(Map.of(
            2.0, 0.1,
            1.0, 1.0,
            0.5, 0.8,
            0.25, 0.4));
    public static final Overtone HIGH = new Overtone(Map.of(
            1.0, 1.0,
            2.0, 0.4,
            4.0, 0.1,
            8.0, 0.001
    ));
    public static final Overtone LOW = new Overtone(Map.of(
            1.0, 1.0,
            0.5, 1.0,
            0.25, 1.0
    ));

    private final Map<Double, Double> multiplierVolMap;
    private double totalVolume;

    public Overtone(Map<Double, Double> multiplierVolMap) {
        this.multiplierVolMap = multiplierVolMap;
        for (Double vol : multiplierVolMap.values()) totalVolume += vol;
    }

    public Overtone() {
        this(Map.of(1.0, 1.0));
    }

    public int getTonesCount() {
        return multiplierVolMap.size();
    }

    public double getTotalVolume() {
        return totalVolume;
    }

    public Map<Double, Double> getMultiplierVolMap() {
        return multiplierVolMap;
    }
}
