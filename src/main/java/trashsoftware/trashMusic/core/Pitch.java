package trashsoftware.trashMusic.core;

import trashsoftware.trashMusic.util.Util;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Pitch {

    public static final Map<Character, Integer> LETTER_ORDER = Map.of(
            'C', 0,
            'D', 2,
            'E', 4,
            'F', 5,
            'G', 7,
            'A', 9,
            'B', 11
    );

    public static final Map<Integer, String> ORDER_PITCH = Util.mergeMaps(
            Map.of(
                    0, "C",
                    1, "C#",
                    2, "D",
                    3, "Eb",
                    4, "E",
                    5, "F",
                    6, "F#",
                    7, "G",
                    8, "G#",
                    9, "A"
            ),
            Map.of(
                    10, "Bb",
                    11, "B"
            )
    );

    public static final Pitch A4 = fromStdRep("A4");
    public static final Pitch C4 = fromStdRep("C4");

    private final int order;

    private Pitch(int order) {
        this.order = order;
    }

    public static Pitch fromOrder(int order) {
        return new Pitch(order);
    }

    public static Pitch fromStdRep(String rep) {
        char first = rep.charAt(0);
        char second = rep.length() > 1 ? rep.charAt(1) : '\0';
        Integer order = LETTER_ORDER.get(first);
        int shiftIndex;
        if (order != null) {
            shiftIndex = 1;
        } else {
            order = LETTER_ORDER.get(second);
            shiftIndex = 0;
        }
        int shift = 0;
        if (rep.length() == 3) {
            char shiftChar = rep.charAt(shiftIndex);
            if (shiftChar == '#') shift = 1;
            else if (shiftChar == 'b' || shiftChar == '♭') shift = -1;
            else throw new TrashMusicException("Unexpected pitch");
        }
        int number = Integer.parseInt(rep.substring(rep.length() - 1));
        return new Pitch(number * 12 + order + shift);
    }

    public double getFreq() {
        if (this.equals(A4)) return 440.0;
        else {
            double base = Math.pow(2.0, 1.0 / 12.0);
            return 440.0 * Math.pow(base, order - A4.order);
        }
    }

    public int getOrder() {
        return order;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pitch pitch = (Pitch) o;
        return order == pitch.order;
    }

    @Override
    public int hashCode() {
        return Objects.hash(order);
    }

    @Override
    public String toString() {
        int major = order / 12;
        int minor = order % 12;
        return ORDER_PITCH.get(minor) + major;
    }

    public String toStringMusical() {
        String normal = toString();
        if (normal.length() == 2) return normal.substring(0, 1);
        else return (normal.charAt(1) == 'b' ? '♭' : normal.charAt(1)) + normal.substring(0, 1);
    }
}
