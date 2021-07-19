package trashsoftware.trashMusic.core;

import java.util.zip.CRC32;

public interface MusicElement {

    void updateCrc32(CRC32 current);
}
