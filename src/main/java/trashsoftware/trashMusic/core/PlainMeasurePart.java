package trashsoftware.trashMusic.core;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

public class PlainMeasurePart extends ArrayList<Note> {
    private PartType partType;

    public PlainMeasurePart() {
        this(PartType.NORMAL);
    }

    public PlainMeasurePart(PartType partType) {
        this.partType = partType;
    }

    public void setPartType(PartType partType) {
        this.partType = partType;
    }

    public double getBeatsInPart() {
        return stream().map(Note::getBeats).collect(Collectors.toList()).stream().reduce(0.0, Double::sum);
    }

    public PartType getPartType() {
        return partType;
    }

    @Override
    public boolean add(Note note) {
        return super.add(note);
    }

    public void updateCrc32(CRC32 current) {
        for (MusicElement element : this) {
            element.updateCrc32(current);
        }
    }

    public enum PartType {
        NORMAL,
        BEGIN_OF_REPEAT,
        END_OF_REPEAT,
        REPEAT_FROM_START
    }
}
