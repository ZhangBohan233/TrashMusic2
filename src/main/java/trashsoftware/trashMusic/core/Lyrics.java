package trashsoftware.trashMusic.core;

import java.util.HashMap;
import java.util.TreeMap;

public class Lyrics extends TreeMap<Integer, Lyrics.LyricParagraph> {

    public void putLyric(int paragraphNum, Note note, String lyric) {
        LyricParagraph paragraph = get(paragraphNum);
        if (paragraph == null) {
            paragraph = new LyricParagraph(paragraphNum);
            put(paragraphNum, paragraph);
        }
        paragraph.put(note, new Text(lyric, paragraph, note));
    }

    public static class LyricParagraph extends HashMap<Note, Text> {
        private final int paragraphNum;

        LyricParagraph(int paragraphNum) {
            this.paragraphNum = paragraphNum;
        }

        public int getParagraphNum() {
            return paragraphNum;
        }
    }
}
