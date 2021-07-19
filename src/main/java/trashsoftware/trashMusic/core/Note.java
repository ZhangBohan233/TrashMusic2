package trashsoftware.trashMusic.core;

import java.util.Map;
import java.util.zip.CRC32;

public class Note implements MusicElement {

    public static final Map<Integer, String> NUM_LETTER = Map.of(
            1, "C",
            2, "D",
            3, "E",
            4, "F",
            5, "G",
            6, "A",
            7, "B"
    );

    private final double beats;
    private final int num;
    private final int shift;
    private final int lowHigh;

    public Note(int num, int lowHigh, int shift, double beats) {
        this.num = num;
        this.lowHigh = lowHigh;
        this.shift = shift;
        this.beats = beats;
    }

    public Pitch toPitch(Pitch basePitch) {
        Pitch baseC4Pitch = toPitchBasedOnC4();
        return Pitch.fromOrder(basePitch.getOrder() - Pitch.C4.getOrder() + baseC4Pitch.getOrder());
    }

    public boolean isPause() {
        return num == 0;
    }

    public double getBeats() {
        return beats;
    }

    public double getDurationMs(int speed) {
        return (60000.0 / speed * beats);
    }

    public int getLowHigh() {
        return lowHigh;
    }

    /**
     * @param beatLength 以几分音符为一拍，如：四分音符为0.25
     * @return 时值，以四分音符为1
     */
    public double getTimeValue(double beatLength) {
        return beats * beatLength / 0.25;
    }

    private Pitch toPitchBasedOnC4() {
        String letter = NUM_LETTER.get(num);
        String shiftStr = getShiftStr();
        return Pitch.fromStdRep(letter + shiftStr + (lowHigh + 4));
    }

    private String getShiftStr() {
        return switch (shift) {
            case 1 -> "#";
            case -1 -> "b";
            default -> "";
        };
    }

    private String getLowHighStr() {
        return "'".repeat(Math.max(0, lowHigh)) + ".".repeat(Math.max(0, -lowHigh));
    }

    @Override
    public String toString() {
        return num + getShiftStr() + getLowHighStr() + "*" + beats;
    }

    public String toStringMusical() {
        String shiftStr = "♭".repeat(Math.max(0, -shift)) + "#".repeat(Math.max(0, shift));
        return shiftStr + num;
    }

    public String toStringNotation(double beatLength) {
        double timeValue = getTimeValue(beatLength);
        String lengthStr = "";
        if (timeValue < 0.25) lengthStr = "___";
        else if (timeValue < 0.5) lengthStr = "__";
        else if (timeValue < 1) lengthStr = "_";
        else if (timeValue == 2) lengthStr = "-";
        else if (timeValue == 3) lengthStr = "--";
        else if (timeValue == 4) lengthStr = "---";

        if (timeValue < 2 && timeValue % 0.1875 == 0) lengthStr += "*";
        return num + getShiftStr() + getLowHighStr() + lengthStr;
    }

    @Override
    public void updateCrc32(CRC32 current) {
        current.update(num == 0 ? 0 : Pitch.LETTER_ORDER.get(NUM_LETTER.get(num).charAt(0)) + shift);
        current.update((int) (beats * 8));
        current.update(lowHigh);
    }
}
