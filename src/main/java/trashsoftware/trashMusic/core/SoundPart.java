package trashsoftware.trashMusic.core;

import java.util.ArrayList;

/**
 * 声部
 */
public class SoundPart extends ArrayList<MeasurePart> {

    private final PlainSoundPart soundPart;

    public SoundPart(PlainSoundPart soundPart) {
        this.soundPart = soundPart;
    }
    
    public String getName() {
        return soundPart.getName();
    }
}
