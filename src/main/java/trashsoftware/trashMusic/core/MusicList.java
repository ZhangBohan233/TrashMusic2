package trashsoftware.trashMusic.core;

import java.util.ArrayList;
import java.util.zip.CRC32;

public class MusicList extends ArrayList<MusicElement> implements MusicElement {

    public MusicElement getLast() {
        return get(size() - 1);
    }

    @Override
    public void updateCrc32(CRC32 current) {
        for (MusicElement element : this) {
            element.updateCrc32(current);
        }
    }
}
