package trashsoftware.trashMusic.core;

import java.util.ArrayList;

public class PlainSoundPart extends ArrayList<PlainMeasurePart> {
    private int index;
    private int volume;
    private final String name;

    public PlainSoundPart(String name, int volume, int index) {
        this.name = name;
        this.volume = volume;
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public PlainMeasurePart getLast() {
        return get(size() - 1);
    }

    public PlainMeasurePart removeLast() {
        return remove(size() - 1);
    }
}
