package trashsoftware.trashMusic.core;

import trashsoftware.trashMusic.core.eq.Equalizer;
import trashsoftware.trashMusic.core.eq.Overtone;
import trashsoftware.trashMusic.core.volTransform.VolumeTransform;
import trashsoftware.trashMusic.core.wav.WavFile;

import java.io.File;
import java.io.IOException;

public class TmnTest {

    public static void main(String[] args) throws IOException {
        TrashMusicNotation tmn = TrashMusicNotation.fromTmnFile(new File("files/嘉宾.tmn"));
        System.out.println(tmn);
        tmn.writeWav(22050,
                new TrashMusicNotation.OvertoneEqualizer[]
                        {new TrashMusicNotation.OvertoneEqualizer(Overtone.PLAIN, Equalizer.PLAIN)},
                new VolumeTransform[]{null, null},
                false);
        WavFile beatsFile = tmn.makeDrumbeats();
        beatsFile.writeWav();
//        WavFile wavFile = WavFile.fromFile(new File("files/doReMi.tmn.wav"));
//        System.out.println(wavFile);
//        WavFile newFile = WavFile.createNew(new File("files/test.wav"), 11025, 1);
//        newFile.putFlatFreq(440.0, 500, 30.0);
//        newFile.putFlatFreq(493.9, 1000, 30.0, Overtone.MID, Equalizer.PLAIN);
//        newFile.flushBuffer();
//        newFile.writeWav();
    }
}
