package trashsoftware.trashMusic.core;

import trashsoftware.trashMusic.core.eq.Equalizer;
import trashsoftware.trashMusic.core.eq.Overtone;
import trashsoftware.trashMusic.core.volTransform.ExpVolumeTransform;
import trashsoftware.trashMusic.core.volTransform.LinearVolumeTransform;
import trashsoftware.trashMusic.core.volTransform.VolumeTransform;
import trashsoftware.trashMusic.core.wav.WavFile;
import trashsoftware.trashMusic.util.Util;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

public class TrashMusicNotation {

    private static final double baseVolMultiplier = 0.5;
    private final Lyrics lyrics = new Lyrics();
    private final File tmnFile;
    private PlainSoundPart[] soundParts;
    private int speed;
    private double beatLength;  // 以几分音符为一拍，四分音符为0.25
    private int beatsCount;  // 每小节拍数
    private Pitch basePitch;

    private TrashMusicNotation(File tmnFile) {
        this.tmnFile = tmnFile;
    }

    public static TrashMusicNotation fromTmnFile(File tmnFile) {
        TrashMusicNotation notation = new TrashMusicNotation(tmnFile);
        PlainSoundPart activeSoundPart = null;
        boolean mainStarted = false;
        Note lastNote = null;
        try (BufferedReader br = new BufferedReader(new FileReader(tmnFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.strip();
                if (!line.isBlank() && !line.startsWith("#")) {
                    if (mainStarted) {
                        if (activeSoundPart == null) {
                            throw new TrashMusicException();
                        }
                        String[] parts = line.split(" ");
                        for (String part : parts) {
                            part = part.strip();
                            if (!part.isBlank()) {
                                if (part.startsWith("(") && part.endsWith(")")) {
                                    PlainSoundPart soundPart = notation
                                            .getSoundPartByName(part.substring(1, part.length() - 1));
                                    if (soundPart != null) activeSoundPart = soundPart;
                                } else if (part.equals("|")) {
                                    activeSoundPart.add(new PlainMeasurePart());
                                } else if (part.equals("||")) {
                                    activeSoundPart.getLast().setPartType(PlainMeasurePart.PartType.REPEAT_FROM_START);
                                    activeSoundPart.add(new PlainMeasurePart());
                                } else if (part.equals(":||")) {
                                    activeSoundPart.getLast().setPartType(PlainMeasurePart.PartType.END_OF_REPEAT);
                                    activeSoundPart.add(new PlainMeasurePart());
                                } else if (part.equals("||:")) {
                                    if (activeSoundPart.getLast().isEmpty()) {
                                        activeSoundPart.removeLast();
                                    }
                                    activeSoundPart.add(new PlainMeasurePart(PlainMeasurePart.PartType.BEGIN_OF_REPEAT));
                                } else if (isMusicDigit(part.charAt(0)) &&
                                        (part.length() == 1 || part.charAt(1) != ':')) {
                                    Note note = analyzeNote(part, notation.beatLength);
                                    lastNote = note;
                                    PlainMeasurePart last = activeSoundPart.getLast();
                                    last.add(note);
//                                    appendToNested(last, note, last.getBeatsInPart());
//                                    last.setBeatsInPart(last.getBeatsInPart() + note.getBeats());
                                } else {
                                    int par;
                                    String text;
                                    if (part.indexOf(":") == 1 && (part.charAt(0) > '0' && part.charAt(0) <= '9')) {
                                        // 别想了，不可能有9段以上的歌
                                        par = part.charAt(0) - '0';
                                        text = part.substring(2);
                                    } else {
                                        par = 1;
                                        text = part;
                                    }
                                    if (lastNote == null)
                                        throw new TrashMusicException("Syntax error: leading lyrics");
                                    notation.lyrics.putLyric(par, lastNote, text);
                                }
                            }
                        }
                    } else {
                        if (line.equals("main")) {
                            if (notation.speed == 0 ||
                                    notation.beatLength == 0 ||
                                    notation.beatsCount == 0 ||
                                    notation.basePitch == null) {
                                throw new TrashMusicException("File header not completed");
                            }
                            mainStarted = true;
                            if (notation.soundParts == null) {
                                notation.soundParts = new PlainSoundPart[1];
                                notation.soundParts[0] = new PlainSoundPart("Solo", 100, 0);
                                notation.soundParts[0].add(new PlainMeasurePart());
                                activeSoundPart = notation.soundParts[0];
                            }
                        } else {
                            String[] parts = line.split("=");
                            for (int i = 0; i < parts.length; i++) {
                                parts[i] = parts[i].strip();
                            }
                            if (parts.length != 2) throw new TrashMusicException("Syntax error");
                            switch (parts[0]) {
                                case "1":
                                    notation.basePitch = Pitch.fromStdRep(parts[1]);
                                    break;
                                case "speed":
                                    notation.speed = Integer.parseInt(parts[1]);
                                    break;
                                case "beat":
                                    String[] beatStr = parts[1].split("/");
                                    if (beatStr.length != 2)
                                        throw new TrashMusicException("Syntax error");
                                    notation.beatsCount = Integer.parseInt(beatStr[0].strip());
                                    notation.beatLength = 1.0 / Integer.parseInt(beatStr[1].strip());
                                    break;
                                case "soundParts":
                                    String[] soundPartNames = parts[1].split(",");
                                    notation.soundParts = new PlainSoundPart[soundPartNames.length];

                                    for (int i = 0; i < soundPartNames.length; i++) {
                                        notation.soundParts[i] = new PlainSoundPart(
                                                soundPartNames[i].strip(),
                                                80,
                                                i);
                                        notation.soundParts[i].add(new PlainMeasurePart());
                                    }

                                    activeSoundPart = notation.soundParts[0];
                                case "volumes":
                                    break;
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(notation.basePitch);

        return notation;
    }

    private static boolean appendToNested(MusicList tarList, Note toAdd, double beatsBefore) {
        double noteBeats = toAdd.getBeats();
        if (tarList instanceof BeatGroup) {
            if (beatsBefore % 1 == 0) {  //  一整拍结束
                return false;
            } else if (beatsBefore % 1 + noteBeats > 1) {  // 原本不到一拍，加了之后又多了
                return false;
            } else {
                MusicElement last = tarList.getLast();
                if (last instanceof BeatGroup) {
                    if (!appendToNested((MusicList) last, toAdd, beatsBefore)) {
                        tarList.add(toAdd);
                    }
                    return true;
                } else {
                    double groupBaseBeats = ((BeatGroup) tarList).getBaseLength();
                    if (noteBeats == groupBaseBeats) {
                        tarList.add(toAdd);
                        return true;
                    } else if (noteBeats == groupBaseBeats * 0.5 || noteBeats == groupBaseBeats * 0.75) {
                        tarList.add(new BeatGroup(groupBaseBeats * 0.5, toAdd));
                        return true;
                    } else if (noteBeats == groupBaseBeats * 0.25 || noteBeats == groupBaseBeats * 0.375) {
                        tarList.add(new BeatGroup(groupBaseBeats * 0.25,
                                new BeatGroup(groupBaseBeats * 0.25, toAdd)));
                        return true;
                    } else {
                        // 在“以二分音符为一拍”的情况下可能运行至此处
                        return false;
                    }
                }
            }
        } else if (tarList instanceof MeasurePart) {
            if (tarList.size() > 0) {
                MusicElement last = tarList.getLast();
                if (last instanceof BeatGroup) {
                    if (appendToNested((MusicList) last, toAdd, beatsBefore))
                        return true;
                }
            }

            if (beatsBefore % 1 == 0) {
                if (noteBeats >= 1) {
                    tarList.add(toAdd);
                } else if (noteBeats >= 0.5) {
                    tarList.add(new BeatGroup(0.5, toAdd));
                } else if (noteBeats >= 0.25) {
                    tarList.add(new BeatGroup(0.5, new BeatGroup(0.25, toAdd)));
                } else {
                    tarList.add(new BeatGroup(0.5, new BeatGroup(0.25,
                            new BeatGroup(0.125, toAdd))));
                }
            } else {
                tarList.add(toAdd);
            }
            return true;
        } else {
            throw new TrashMusicException("Unexpected list: " + tarList);
        }
    }

    private static Note analyzeNote(String notation, double beatLength) {
        int num = notation.charAt(0) - '0';
        int high = Util.count(notation, '\'');
        int low = Util.count(notation, '.');
        if (high != 0 && low != 0) throw new TrashMusicException("Cannot both high and low");
        int longer = Util.count(notation, '-');
        int shorter = Util.count(notation, '_');
        if (longer != 0 && shorter != 0)
            throw new TrashMusicException("Cannot both long and short");

        // length 指拍数。简谱标准为四分音符，其 beat_length = 0.25。
        // 如以二分音符为一拍，则四分音符拍数为0.5
        double length = 0.25 / beatLength;
        if (longer != 0) length *= (longer + 1);
        else if (shorter != 0) length *= Math.pow(0.5, shorter);

        int extendCount = Util.count(notation, '*');
        if (extendCount == 1) length *= 1.5;
        else if (extendCount != 0) throw new TrashMusicException("Cannot extend multiple times");

        int shift = Util.count(notation, '#') - Util.count(notation, 'b');
        return new Note(num, high - low, shift, length);
    }

    private static boolean isMusicDigit(char c) {
        return c >= '0' && c <= '7';
    }

    public SoundPart[] makeGroupedParts() {
        SoundPart[] soundParts = new SoundPart[this.soundParts.length];
        for (int sp = 0; sp < soundParts.length; ++sp) {
            PlainSoundPart psp = this.soundParts[sp];
            SoundPart soundPart = new SoundPart(psp);
            for (int p = 0; p < psp.size(); ++p) {
                PlainMeasurePart pmp = psp.get(p);
                double beatsInPart = pmp.getBeatsInPart();
                if (beatsInPart != beatsCount && beatsInPart != 0) {
                    System.err.printf("第%d声部第%d小节拍数不符。实际拍数为%f拍。",
                            sp + 1, p + 1, pmp.getBeatsInPart());
                    System.err.println(pmp);
                }
                MeasurePart mp = new MeasurePart(pmp);
                double beats = 0.0;
                for (Note note : pmp) {
                    appendToNested(mp, note, beats);
                    beats += note.getBeats();
                }
                soundPart.add(mp);
            }
            soundParts[sp] = soundPart;
        }
        return soundParts;
    }

    public File waveFile() {
        return new File(tmnFile.getAbsolutePath() + ".wav");
    }

    public File waveFile(int soundPartNum) {
        return new File(tmnFile.getAbsolutePath() + ".part" + soundPartNum + ".wav");
    }

    public File drumFile() {
        return new File(tmnFile.getAbsolutePath() + ".drum.wav");
    }

    public long notationChecksum() {
        byte[] header = {(byte) speed, (byte) (beatLength * 8), (byte) beatsCount, (byte) basePitch.getOrder()};
        CRC32 crc32 = new CRC32();
        crc32.update(header);
        for (PlainSoundPart sp : soundParts) {
            crc32.update(sp.getVolume());
        }
        for (PlainSoundPart sp : soundParts) {
            for (PlainMeasurePart mp : sp) {
                mp.updateCrc32(crc32);
            }
        }
        return crc32.getValue();
    }

    public void saveAsTmn() throws IOException {
        saveAsTmn(tmnFile);
    }

    public void saveAsTmn(File file) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write(String.format("1=%s\n", basePitch));
            bw.write(String.format("speed=%d\n", speed));
            bw.write(String.format("beat=%d/%d\n", beatsCount, (int) (1 / beatLength)));

            if (soundParts.length != 1) {
                bw.write(String.format("soundParts=%s\n",
                        List.of(soundParts).stream().map(PlainSoundPart::getName).collect(Collectors.joining(","))));
                bw.write(String.format("volumes=%s\n",
                        List.of(soundParts).stream().map(
                                        sp -> String.valueOf(sp.getVolume()))
                                .collect(Collectors.joining(","))));
            }

            bw.write("\nmain\n");

            int partsPerLine = (int) Math.round(16.0 / beatsCount);
            List<String>[] outLists = new StringList[soundParts.length];
            for (int i = 0; i < outLists.length; ++i) outLists[i] = new StringList();
            for (int sp = 0; sp < soundParts.length; ++sp) {
                List<String> lineList = new ArrayList<>();
                for (int p = 0; p < soundParts[sp].size(); ++p) {
                    PlainMeasurePart mp = soundParts[sp].get(p);
                    PlainMeasurePart nextPart = p == soundParts[sp].size() - 1 ? null : soundParts[sp].get(p + 1);
                    if (mp.getPartType() == PlainMeasurePart.PartType.BEGIN_OF_REPEAT) {
                        lineList.add("||:");
                    }
                    processListToList(mp, lineList);
                    if (mp.getPartType() == PlainMeasurePart.PartType.END_OF_REPEAT) {
                        lineList.add(":||");
                    } else if (mp.getPartType() == PlainMeasurePart.PartType.REPEAT_FROM_START) {
                        lineList.add("||");
                    } else if (nextPart != null && nextPart.getPartType() != PlainMeasurePart.PartType.BEGIN_OF_REPEAT) {
                        lineList.add("|");
                    }
                    if (p % partsPerLine == partsPerLine - 1) {
                        String line = String.join("", lineList);
                        outLists[sp].add(line);
                    }
                }
            }
            if (soundParts.length == 1) {
                for (String line : outLists[0]) {
                    bw.write(line);
                    bw.write("\n");
                }
            } else {
                for (int lineNum = 0; lineNum < outLists[0].size(); ++lineNum) {
                    for (int sp = 0; sp < soundParts.length; ++sp) {
                        bw.write(String.format("(%s) ", soundParts[sp].getName()));
                        bw.write(outLists[sp].get(lineNum));
                        bw.write("\n");
                    }
                }
            }
        }
    }

    private void processListToList(PlainMeasurePart list, List<String> outList) {
        for (Note note : list) {
            outList.add(note.toStringNotation(beatLength) + " ");
            for (Map.Entry<Integer, Lyrics.LyricParagraph> entry : lyrics.entrySet()) {
                Text text = entry.getValue().get(note);
                if (text != null) {
                    if (lyrics.size() == 1) {
                        outList.add(text.getText() + " ");
                    } else {
                        outList.add(String.format("%d:%s ", entry.getKey(), text.getText()));
                    }
                }
            }
        }
    }

    WavFile makeDrumbeats() {
        WavFile wavFile = WavFile.createNew(drumFile(), WavFile.DEFAULT_SAMPLE_RATE, 1);
        Drumbeat[] drums;
        if (beatsCount == 2) {
            drums = new Drumbeat[]{Drumbeat.BASS_DRUM, Drumbeat.SIDE_DRUM};
        } else if (beatsCount == 3) {
            drums = new Drumbeat[]{Drumbeat.BASS_DRUM, Drumbeat.SIDE_DRUM, Drumbeat.SIDE_DRUM};
        } else if (beatsCount == 4) {
            drums = new Drumbeat[]{Drumbeat.BASS_DRUM, Drumbeat.CYMBAL, Drumbeat.SIDE_DRUM, Drumbeat.CYMBAL};
        } else {
            throw new TrashMusicException();
        }
        double beatDurationMs = 60000.0 / speed;
        double passDuration = 0.0;
        double writtenDuration = 0.0;
        for (PlainMeasurePart ignored : soundParts[0]) {
            for (int i = 0; i < beatsCount; ++i) {
                Drumbeat drum = drums[i];
                passDuration += beatDurationMs;
                wavFile.putData(drum.getWave(), 0);
                writtenDuration += drum.getActualDurationMs();
                double rem = passDuration - writtenDuration;
                writtenDuration += wavFile.putFlatFreq(0, rem, 0);
            }
        }
        wavFile.flushBuffer();
        return wavFile;
    }
    
    private OvertoneEqualizer[] defaultEqualizers(int nSoundParts) {
        if (nSoundParts == 1) {
            return new OvertoneEqualizer[]{OvertoneEqualizer.PLAIN};
        } else if (nSoundParts == 2) {
            return new OvertoneEqualizer[]{
                    new OvertoneEqualizer(
                            Overtone.LOW,
                            Equalizer.PLAIN
                    ),
                    new OvertoneEqualizer(
                            Overtone.HIGH,
                            Equalizer.PLAIN
                    )
            };
        } else {
            OvertoneEqualizer[] oes = new OvertoneEqualizer[nSoundParts];
            for (int i = 0; i < soundParts.length; ++i) {
                oes[i] = OvertoneEqualizer.PLAIN;
            }
            return oes;
        }
    }

    public void writeWav(boolean withDrums) {
        int sampleRate = WavFile.DEFAULT_SAMPLE_RATE;
        OvertoneEqualizer[] oes = defaultEqualizers(soundParts.length);
        VolumeTransform[] transforms = new VolumeTransform[soundParts.length];
        for (int i = 0; i < soundParts.length; ++i) {
            transforms[i] = new ExpVolumeTransform(sampleRate / 2);
        }
        writeWav(sampleRate, oes, transforms, withDrums);
    }

    void writeWav(int sampleRate,
                  OvertoneEqualizer[] overtoneEqualizers,
                  VolumeTransform[] transforms,
                  boolean withDrums) {
        WavFile wavFile;
        if (soundParts.length == 1) {
            wavFile = new WaveMaker().saveOneSoundPart(
                    waveFile(), soundParts[0], sampleRate, overtoneEqualizers[0], transforms[0]);
        } else {
            if (soundParts.length != overtoneEqualizers.length) {
                throw new TrashMusicException("Number of sound parts and number of eqs do not match");
            }
            WavFile[] wavFiles = new WavFile[soundParts.length];
            for (int i = 0; i < wavFiles.length; ++i) {
                wavFiles[i] = new WaveMaker().saveOneSoundPart(
                        waveFile(i), soundParts[i], sampleRate, overtoneEqualizers[i], transforms[i]);
            }
            wavFile = overlayMultipleWaves(wavFiles);
        }
        if (withDrums) {
            WavFile drumFile = makeDrumbeats();
            int[] data = wavFile.getChannel(0);
            int[] drumData = drumFile.getChannel(0);
            for (int i = 0; i < Math.min(data.length, drumData.length); ++i) {
                data[i] += drumData[i];
            }
        }
        wavFile.writeWav();
        System.out.println("Write finish");
    }

    private WavFile overlayMultipleWaves(WavFile[] wavFiles) {
        WavFile first = wavFiles[0];
        WavFile resultFile = WavFile.createNew(waveFile(), first.getSampleRate(), first.getNumChannels());
        for (WavFile wavFile : wavFiles) {
            resultFile.overlay(wavFile);
        }
        return resultFile;
    }

    @Override
    public String toString() {
        return "TrashMusicNotation{" +
                "speed=" + speed +
                ", beatLength=" + beatLength +
                ", beatsCount=" + beatsCount +
                ", basePitch=" + basePitch +
                ", tmnFile=" + tmnFile +
                ", soundParts=" + Arrays.toString(soundParts) +
                '}';
    }

    public PlainSoundPart getSoundPartByName(String name) {
        for (PlainSoundPart soundPart : soundParts) {
            if (soundPart.getName().equals(name)) return soundPart;
        }
        return null;
    }

    public PlainSoundPart getSoundPart(int index) {
        return soundParts[index];
    }

    public int getNumParagraph() {
        return lyrics.size();
    }

    public double getBeatLength() {
        return beatLength;
    }

    public int getBeatsCount() {
        return beatsCount;
    }

    public File getTmnFile() {
        return tmnFile;
    }

    public int getSpeed() {
        return speed;
    }

    public void setBasePitch(Pitch basePitch) {
        this.basePitch = basePitch;
    }

    public Pitch getBasePitch() {
        return basePitch;
    }

    public Pitch getBasePlayPitch() {
        return basePitch;
    }

    public Lyrics getLyrics() {
        return lyrics;
    }

    public void deleteNote(PlainMeasurePart part, Note note) {
        for (PlainSoundPart psp : soundParts) {
            for (PlainMeasurePart pmp : psp) {
                if (pmp == part) {
                    if (pmp.remove(note)) break;
                }
            }
        }
    }

    public void deleteText() {

    }

    public void addNote() {

    }

    public void addMeasurePart(PlainMeasurePart.PartType type, int soundPartIndex) {

    }

    public void appendMeasurePart(PlainMeasurePart.PartType type, int soundPartIndex) {
        if (type == PlainMeasurePart.PartType.BEGIN_OF_REPEAT) {
            PlainMeasurePart last = soundParts[soundPartIndex].getLast();
            last.setPartType(PlainMeasurePart.PartType.BEGIN_OF_REPEAT);
        } else {
            PlainMeasurePart newPart = new PlainMeasurePart(type);
            soundParts[soundPartIndex].add(newPart);
        }
    }

    public static class OvertoneEqualizer {
        public static final OvertoneEqualizer PLAIN = new OvertoneEqualizer(
                Overtone.PLAIN, Equalizer.PLAIN
        );

        private final Overtone overtone;
        private final Equalizer equalizer;

        public OvertoneEqualizer(Overtone overtone, Equalizer equalizer) {
            this.overtone = overtone;
            this.equalizer = equalizer;
        }
    }

    private static class StringList extends ArrayList<String> {

    }

    private class WaveMaker {

        // 以下两项用于处理 double 转 long 时的累积误差
        private double totalNotationDurationMs;  // 已经处理的时长（乐谱上）
        private long totalWrittenDurationMs;  // 已经写入文件的时长

        private WavFile saveOneSoundPart(File file,
                                         PlainSoundPart soundPart,
                                         int frameRate,
                                         OvertoneEqualizer overtoneEqualizer,
                                         VolumeTransform transform) {
            WavFile wavFile = WavFile.createNew(file, frameRate, 1);

            int repeatIndex = -1;
            Set<Integer> usedRepeats = new TreeSet<>();

            int partIndex = 0;  // index of measurePart
            while (partIndex < soundPart.size()) {
                PlainMeasurePart measurePart = soundPart.get(partIndex++);
                double beatsInPart = measurePart.getBeatsInPart();
                if (beatsInPart != 0 && beatsInPart != beatsCount) {
                    System.err.printf("第%d声部第%d小节拍数不符。实际拍数为%f拍%n",
                            soundPart.getIndex() + 1, partIndex, measurePart.getBeatsInPart());
                }
                if (measurePart.getPartType() == PlainMeasurePart.PartType.BEGIN_OF_REPEAT) {
                    repeatIndex = partIndex;
                } else if (measurePart.getPartType() == PlainMeasurePart.PartType.END_OF_REPEAT) {
                    if (!usedRepeats.contains(partIndex)) {
                        usedRepeats.add(partIndex);
                        if (repeatIndex == -1) throw new TrashMusicException("No begin of repeat");
                        partIndex = repeatIndex;
                    }
                } else if (measurePart.getPartType() == PlainMeasurePart.PartType.REPEAT_FROM_START) {
                    if (!usedRepeats.contains(partIndex)) {
                        usedRepeats.add(partIndex);
                        partIndex = 0;
                    }
                }
                processList(measurePart, soundPart.getVolume(), wavFile, overtoneEqualizer, transform);
            }

            wavFile.flushBuffer();
            return wavFile;
        }

        private void processList(PlainMeasurePart lst,
                                 int soundPartVol,
                                 WavFile wavFile,
                                 OvertoneEqualizer overtoneEqualizer,
                                 VolumeTransform transform) {
            for (Note note : lst) {
                double duration = note.getDurationMs(speed);
                totalNotationDurationMs += duration;
                double writeDuration = totalNotationDurationMs - totalWrittenDurationMs;
                double freq = note.isPause() ? 0.0 : note.toPitch(basePitch).getFreq();
                totalWrittenDurationMs += wavFile.putFlatFreq(
                        freq,
                        writeDuration,
                        soundPartVol * baseVolMultiplier,
                        transform,
                        0,
                        overtoneEqualizer.overtone,
                        overtoneEqualizer.equalizer);
            }
        }
    }
}
