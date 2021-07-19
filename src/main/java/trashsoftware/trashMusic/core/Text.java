package trashsoftware.trashMusic.core;

public class Text {

    private final String text;
    private final Lyrics.LyricParagraph paragraph;
    private final Note note;

    Text(String text, Lyrics.LyricParagraph paragraph, Note note) {
        this.text = text;
        this.paragraph = paragraph;
        this.note = note;
    }

    public String getText() {
        return text;
    }

    public Lyrics.LyricParagraph getParagraph() {
        return paragraph;
    }

    public Note getNote() {
        return note;
    }

    @Override
    public String toString() {
        return text;
    }
}
