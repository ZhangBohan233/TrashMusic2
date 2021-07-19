package trashsoftware.trashMusic.core;

import java.util.ArrayList;

/**
 * 声部
 */
public class SoundPart extends ArrayList<MeasurePart> {

    private int volume;
    private final String name;

    public SoundPart(String name, int volume) {
        this.name = name;
        this.volume = volume;
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

    public MeasurePart getLast() {
        return get(size() - 1);
    }

    public MeasurePart removeLast() {
        return remove(size() - 1);
    }
}
