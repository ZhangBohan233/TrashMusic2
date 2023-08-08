package trashsoftware.trashMusic.fxml;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import trashsoftware.trashMusic.core.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class NotationWrapper {

    private static final double STD_WIDTH = 56.0;
    private static final double TOP_Y = 32.0;
    private static final double LEFT_X = 64.0;
    private static final double LYRIC_HEIGHT = 40.0;

    private static final double TEXT_HEIGHT = 12.0;
    private static final double TEXT_WIDTH = 8.0;
    private static final double UNDERLINE_SPACING = 3.0;
    private static final double CURSOR_RIGHT = 6.0;
    private static final double CURSOR_HEIGHT = 16.0;

    private static final int CLICK_DETECT_HEIGHT = 18;
    private static final int CLICK_DETECT_WIDTH = (int) STD_WIDTH;

    private static final Color BACKGROUND = Color.WHITESMOKE;

    private final double rowHeight;
    private final int rowParts;

    private final TrashMusicNotation notation;
    private final Canvas canvas;
    private final GraphicsContext graphics;
    private final EditorView parent;

    private double x, y, lastNoteX;
    private double line1LeftX = -1, line2LeftX = -1, line3LeftX = -1;

    private final Map<Position, ClickableWrapper> positionNoteMap = new HashMap<>();
    private ClickableWrapper currentSelected;
    private Position selectedPos;

    public NotationWrapper(TrashMusicNotation notation, EditorView parent, Canvas canvas) {
        this.notation = notation;
        this.parent = parent;
        this.canvas = canvas;
        this.graphics = canvas.getGraphicsContext2D();

        rowHeight = (notation.getNumParagraph() + 1) * LYRIC_HEIGHT;
        rowParts = Math.round(16.0f / notation.getBeatsCount());
    }

    public TrashMusicNotation getNotation() {
        return notation;
    }

    void addMeasurePart(String addedType) {
        PlainMeasurePart.PartType partType = switch (addedType) {
            case "||" -> PlainMeasurePart.PartType.REPEAT_FROM_START;
            case "||:" -> PlainMeasurePart.PartType.BEGIN_OF_REPEAT;
            case ":||" -> PlainMeasurePart.PartType.END_OF_REPEAT;
            default -> PlainMeasurePart.PartType.NORMAL;
        };
        if (selectedPos == null) {
            notation.appendMeasurePart(partType, 0);
            refresh();
            return;
        }
    }

    void bindListeners(ScrollPane pane) {
        canvas.setOnMouseClicked(this::onMouseClicked);
        pane.setOnKeyTyped(this::onKeyTyped);
        pane.setOnKeyReleased(this::onKeyReleased);
    }

    void unbindListeners() {
    }

    private void onMouseClicked(MouseEvent event) {
        wipeCursor();
        double mouseX = event.getX();
        double mouseY = event.getY();
        int row = (int) ((mouseY + CLICK_DETECT_HEIGHT / 2 + TEXT_HEIGHT / 2 - TOP_Y) / LYRIC_HEIGHT);
        int rowY = (int) (TOP_Y + row * LYRIC_HEIGHT);
        if (mouseY - rowY > CLICK_DETECT_HEIGHT) return;
        for (int i = 0; i < CLICK_DETECT_WIDTH; ++i) {
            Position pos = new Position((int) (mouseX - i + TEXT_WIDTH / 2), rowY);
            currentSelected = positionNoteMap.get(pos);
            if (currentSelected != null) {
                selectedPos = pos;
                break;
            }
        }
        if (selectedPos != null)
            drawCursor();
    }

    private void wipeCursor() {
        refresh();
    }

    private void drawCursor() {
        graphics.strokeLine(
                selectedPos.x + CURSOR_RIGHT,
                selectedPos.y - CURSOR_HEIGHT / 2,
                selectedPos.x + CURSOR_RIGHT,
                selectedPos.y + CURSOR_HEIGHT / 2);
    }

    private void onKeyReleased(KeyEvent keyEvent) {
        switch (keyEvent.getCode()) {
            case BACK_SPACE:
                if (currentSelected instanceof NoteWrapper) {
                    NoteWrapper toDel = (NoteWrapper) currentSelected;
                    notation.deleteNote(toDel.parentWrapper.measurePart.getPlainPart(), toDel.note);
                    refresh();
                }
                break;
        }
    }

    private void onKeyTyped(KeyEvent keyEvent) {
        String typed = keyEvent.getCharacter();
        if (typed == null || typed.isBlank()) return;
        if (currentSelected instanceof NoteWrapper) {
            int num = typed.charAt(0) - '0';
            if (num < 0 || num > 7) return;
            int shift = 0;
            if (toggleAtIndex(parent.sharpFlatGroup, 0)) {
                shift = 1;
            } else if (toggleAtIndex(parent.sharpFlatGroup, 2)) {
                shift = -1;
            }
            double timeValue = 0.25;
            ToggleButton timeValueButton = (ToggleButton) parent.timeValueGroup.getSelectedToggle();
            if (timeValueButton != null) {
                String[] fraction = timeValueButton.getText().split("/");
                int denominator = Integer.parseInt(fraction[0]);
                int numerator = fraction.length == 2 ? Integer.parseInt(fraction[1]) : 1;
                timeValue = (double) denominator / numerator;
            }
            if (parent.extendCheckBox.isSelected()) {
                timeValue *= 1.5;
            }

            double beats = timeValue / notation.getBeatLength();
            Note note = new Note(num, Integer.parseInt(parent.lowHighLabel.getText()), shift, beats);
            System.out.println(note);
        }
    }

    private static boolean toggleAtIndex(ToggleGroup group, int index) {
        return group.getSelectedToggle() == group.getToggles().get(index);
    }

    void refresh() {
        long st = System.currentTimeMillis();
        graphics.setFill(BACKGROUND);
        graphics.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        positionNoteMap.clear();
        currentSelected = null;
        selectedPos = null;

        graphics.setTextAlign(TextAlignment.CENTER);
        graphics.setFill(Color.BLACK);
        graphics.setStroke(Color.BLACK);

        x = LEFT_X;
        lastNoteX = x;
        y = TOP_Y;

        SoundPart[] soundParts = notation.makeGroupedParts();
        
        int nMeasureParts = soundParts[0].size();
        int nRows = (int) Math.ceil((double) nMeasureParts / rowParts);
        
        for (int row = 0; row < nRows; row++) {
            double rowTopY = TOP_Y + rowHeight * row * soundParts.length;
            double rowBotY = TOP_Y + rowHeight * (row + 1) * soundParts.length;
            if (rowBotY > canvas.getHeight()) {
                canvas.setHeight(rowBotY);
            }
            
            for (int sp = 0; sp < soundParts.length; sp++) {
                y = rowTopY + sp * rowHeight;
                x = LEFT_X;
                
                SoundPart activePart = soundParts[sp];

                graphics.fillText("(" + activePart.getName() + ")", x / 2, y + TEXT_HEIGHT / 3);
                
                int beginMPIndex = Math.min(row * rowParts, activePart.size());
                int endMPIndex = Math.min(beginMPIndex + rowParts, activePart.size());

                double partsInterval = STD_WIDTH / 2;
                for (int p = beginMPIndex; p < endMPIndex; ++p) {
                    MeasurePart part = activePart.get(p);
                    MeasurePartWrapper partWrapper = new MeasurePartWrapper(part, x, y);
                    if (part.getPlainPart().getPartType() == PlainMeasurePart.PartType.BEGIN_OF_REPEAT) {
                        double xx = x - partsInterval;
                        PartBarWrapper barWrapper = new PartBarWrapper(part, true);
                        positionNoteMap.put(new Position((int) xx, (int) y), barWrapper);
                        graphics.setLineWidth(3);
                        graphics.strokeLine(xx, y - TEXT_HEIGHT + 1, xx, y + TEXT_HEIGHT - 1);
                        graphics.setLineWidth(1);
                        graphics.strokeLine(xx + 5, y - TEXT_HEIGHT, xx + 5, y + TEXT_HEIGHT);
                        graphics.fillOval(xx + 8.5, y - 6.5, 3, 3);
                        graphics.fillOval(xx + 8.5, y + 3.5, 3, 3);
                    }

                    drawMeasurePart(partWrapper);
                    partWrapper.endX = x;
                    MeasurePart nextPart = p == activePart.size() - 1 ? null : activePart.get(p + 1);
                    if (part.getPlainPart().getPartType() == PlainMeasurePart.PartType.END_OF_REPEAT) {
                        graphics.strokeLine(x, y - TEXT_HEIGHT, x, y + TEXT_HEIGHT);
                        graphics.setLineWidth(3);
                        graphics.strokeLine(x + 5, y - TEXT_HEIGHT + 1, x + 5, y + TEXT_HEIGHT - 1);
                        graphics.setLineWidth(1);
                        graphics.fillOval(x - 6.5, y - 6.5, 3, 3);
                        graphics.fillOval(x - 6.5, y + 3.5, 3, 3);

                        PartBarWrapper barWrapper = new PartBarWrapper(part);
                        positionNoteMap.put(new Position((int) x, (int) y), barWrapper);
                    } else if (part.getPlainPart().getPartType() == PlainMeasurePart.PartType.REPEAT_FROM_START) {
                        graphics.strokeLine(x, y - TEXT_HEIGHT, x, y + TEXT_HEIGHT);
                        graphics.setLineWidth(3);
                        graphics.strokeLine(x + 5, y - TEXT_HEIGHT + 1, x + 5, y + TEXT_HEIGHT - 1);
                        graphics.setLineWidth(1);

                        PartBarWrapper barWrapper = new PartBarWrapper(part);
                        positionNoteMap.put(new Position((int) x, (int) y), barWrapper);
                    } else if (nextPart != null && nextPart.getPlainPart().getPartType() != PlainMeasurePart.PartType.BEGIN_OF_REPEAT) {
                        // 最后一个小节不画
                        graphics.strokeLine(x, y - TEXT_HEIGHT, x, y + TEXT_HEIGHT);

                        PartBarWrapper barWrapper = new PartBarWrapper(part);
                        positionNoteMap.put(new Position((int) x, (int) y), barWrapper);
                    }
                    
                    x += partsInterval;
                }
            }
        }
        
        System.out.println("Draw time: " + (System.currentTimeMillis() - st));
    }

    private void drawMeasurePart(MeasurePartWrapper measurePartWrapper) {
        x = measurePartWrapper.x;
        y = measurePartWrapper.y;
        drawList(measurePartWrapper.measurePart, measurePartWrapper);
    }

    private void drawList(MusicList musicList, MeasurePartWrapper parentWrapper) {
        for (MusicElement element : musicList) {
            double underDotY = y + TEXT_HEIGHT / 2;
            if (element instanceof BeatGroup) {
                BeatGroup group = (BeatGroup) element;
                double timeValue = group.getTimeValue(notation.getBeatLength());
                if (timeValue == 0.5) line1LeftX = x - TEXT_WIDTH / 2;
                else if (timeValue == 0.25) line2LeftX = x - TEXT_WIDTH / 2;
                else if (timeValue == 0.125) line3LeftX = x - TEXT_WIDTH / 2;
                drawList(group, parentWrapper);
                double endX = lastNoteX + TEXT_WIDTH / 2;
                if (timeValue <= 0.5) {
                    graphics.strokeLine(line1LeftX, underDotY, endX, underDotY);
                }
                if (timeValue <= 0.25) {
                    underDotY += UNDERLINE_SPACING;
                    graphics.strokeLine(line2LeftX, underDotY, endX, underDotY);
                }
                if (timeValue <= 0.125) {
                    underDotY += UNDERLINE_SPACING;
                    graphics.strokeLine(line3LeftX, underDotY, endX, underDotY);
                }
            } else if (element instanceof Note) {
                Note note = (Note) element;
                NoteWrapper noteWrapper = new NoteWrapper(note, parentWrapper);
                lastNoteX = x;
                positionNoteMap.put(new Position((int) x, (int) y), noteWrapper);
                graphics.fillText(note.toStringMusical(), x, y + TEXT_HEIGHT / 3);

                // 歌词
                for (Map.Entry<Integer, Lyrics.LyricParagraph> entry : notation.getLyrics().entrySet()) {
                    Text lyric = entry.getValue().get(note);

                    if (lyric != null) {
                        double yy = y + LYRIC_HEIGHT * entry.getKey();
                        TextWrapper textWrapper = new TextWrapper(lyric, noteWrapper);
                        positionNoteMap.put(new Position((int) x, (int) yy), textWrapper);
                        graphics.fillText(lyric.getText(), x, yy);
                    }
                }

                double timeValue = note.getTimeValue(notation.getBeatLength());
                // 注：延长点仅用于四分音符以内
                if (timeValue <= 1.5 && timeValue % 0.1875 == 0) {
                    double dotX = x + TEXT_WIDTH * 0.75;
                    graphics.fillOval(dotX - 1.5, y - 1.5, 3, 3);
                }

                if (timeValue <= 0.75) {
                    graphics.strokeLine(x - TEXT_WIDTH / 2, underDotY, x + TEXT_WIDTH / 2, underDotY);
                    underDotY += UNDERLINE_SPACING;
                }
                if (timeValue <= 0.375) {
                    graphics.strokeLine(x - TEXT_WIDTH / 2, underDotY, x + TEXT_WIDTH / 2, underDotY);
                    underDotY += UNDERLINE_SPACING;
                }
                if (timeValue <= 0.1875) {
                    graphics.strokeLine(x - TEXT_WIDTH / 2, underDotY, x + TEXT_WIDTH / 2, underDotY);
                    underDotY += UNDERLINE_SPACING;
                }
                if (timeValue >= 2) {
                    double xx = x + STD_WIDTH;
                    for (int i = 1; i < (int) timeValue; ++i) {
                        graphics.strokeLine(xx, y, xx + TEXT_WIDTH, y);
                        xx += STD_WIDTH;
                    }
                }

                if (note.getLowHigh() > 0) {
                    double yy = y - TEXT_HEIGHT / 2 - UNDERLINE_SPACING;
                    for (int i = 0; i < note.getLowHigh(); ++i) {
                        graphics.fillOval(x - 1.25, yy - 1.25, 2.5, 2.5);
                        yy -= UNDERLINE_SPACING;
                    }
                } else if (note.getLowHigh() < 0) {
                    double yy = underDotY;
                    for (int i = 0; i < -note.getLowHigh(); ++i) {
                        graphics.fillOval(x - 1.25, yy - 1.25, 2.5, 2.5);
                        yy += UNDERLINE_SPACING;
                    }
                }

                x += STD_WIDTH * Math.max(timeValue, 0.25);
            }
        }
    }

    private static class MeasurePartWrapper {
        private final MeasurePart measurePart;
        private final double x, y;
        private double endX;

        MeasurePartWrapper(MeasurePart measurePart, double x, double y) {
            this.measurePart = measurePart;
            this.x = x;
            this.y = y;
        }
    }

    private interface ClickableWrapper {

    }

    private static class NoteWrapper implements ClickableWrapper {
        private final Note note;
        private final MeasurePartWrapper parentWrapper;

        NoteWrapper(Note note, MeasurePartWrapper parentWrapper) {
            this.note = note;
            this.parentWrapper = parentWrapper;
        }

        @Override
        public String toString() {
            return "NoteWrapper{" +
                    "note=" + note +
                    '}';
        }
    }

    private static class PartBarWrapper implements ClickableWrapper {
        private final MeasurePart measurePart;
        private final boolean isBeginRepeat;

        PartBarWrapper(MeasurePart measurePart, boolean isBeginRepeat) {
            this.measurePart = measurePart;
            this.isBeginRepeat = isBeginRepeat;
        }

        PartBarWrapper(MeasurePart measurePart) {
            this(measurePart, false);
        }

        @Override
        public String toString() {
            return "PartBarWrapper{" +
                    "measurePart=" + measurePart +
                    '}';
        }
    }

    private static class TextWrapper implements ClickableWrapper {
        private final Text text;
        private final NoteWrapper noteWrapper;

        TextWrapper(Text text, NoteWrapper noteWrapper) {
            this.text = text;
            this.noteWrapper = noteWrapper;
        }

        @Override
        public String toString() {
            return "TextWrapper{" +
                    "text=" + text +
                    ", noteWrapper=" + noteWrapper +
                    '}';
        }
    }

    private static class Position {
        private final int x;
        private final int y;

        Position(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Position position = (Position) o;
            return Double.compare(position.x, x) == 0 && Double.compare(position.y, y) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }

        @Override
        public String toString() {
            return String.format("(%d, %d)", x, y);
        }
    }
}
