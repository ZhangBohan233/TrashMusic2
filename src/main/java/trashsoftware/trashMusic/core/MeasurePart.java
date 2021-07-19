package trashsoftware.trashMusic.core;

public class MeasurePart extends MusicList {

    private PartType partType;
    private double beatsInPart = 0.0;

    public MeasurePart() {
        this(PartType.NORMAL);
    }

    public MeasurePart(PartType partType) {
        this.partType = partType;
    }

    public void setPartType(PartType partType) {
        this.partType = partType;
    }

    public double getBeatsInPart() {
        return beatsInPart;
    }

    public void setBeatsInPart(double beatsInPart) {
        this.beatsInPart = beatsInPart;
    }

    public PartType getPartType() {
        return partType;
    }

    public enum PartType {
        NORMAL,
        BEGIN_OF_REPEAT,
        END_OF_REPEAT,
        REPEAT_FROM_START
    }
}
