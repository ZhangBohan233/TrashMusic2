package trashsoftware.trashMusic.core;

public class MeasurePart extends MusicList {

    private final PlainMeasurePart part;

    MeasurePart(PlainMeasurePart part) {
        this.part = part;
    }

    public PlainMeasurePart getPlainPart() {
        return part;
    }
}
