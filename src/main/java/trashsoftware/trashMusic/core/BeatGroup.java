package trashsoftware.trashMusic.core;

public class BeatGroup extends MusicList {
    private final double baseLength;

    BeatGroup(double baseLength, MusicElement firstElement) {
        this.baseLength = baseLength;
        add(firstElement);
    }

    public double getBaseLength() {
        return baseLength;
    }

    /**
     * @param beatLength 以几分音符为一拍，如：四分音符为0.25
     * @return 时值，以四分音符为1
     */
    public double getTimeValue(double beatLength) {
        return baseLength * beatLength / 0.25;
    }
}
