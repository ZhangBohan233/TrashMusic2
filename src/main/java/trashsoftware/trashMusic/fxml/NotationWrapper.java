package trashsoftware.trashMusic.fxml;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import trashsoftware.trashMusic.core.*;

import java.util.*;

public class NotationWrapper {

    private static final double STD_WIDTH = 56.0;
    private static final double TOP_Y = 32.0;
    private static final double LEFT_X = 64.0;
    private static final double LYRIC_HEIGHT = 40.0;

    private static final double TEXT_HEIGHT = 12.0;
    private static final double TEXT_WIDTH = 8.0;
    private static final double UNDERLINE_SPACING = 3.0;

    private static final double CLICK_DETECT_HEIGHT = 18.0;
    private static final double CLICK_DETECT_WIDTH = STD_WIDTH;

    private final double rowHeight;
    private final int rowParts;

    private final TrashMusicNotation notation;
    private final Canvas canvas;
    private final GraphicsContext graphics;

    private double x, y, lastNoteX;
    private double line1LeftX = -1, line2LeftX = -1, line3LeftX = -1;

    private final Map<Position, NoteWrapper> positionNoteMap = new HashMap<>();
    private ClickableWrapper currentSelected;

    public NotationWrapper(TrashMusicNotation notation, Canvas canvas) {
        this.notation = notation;
        this.canvas = canvas;
        this.graphics = canvas.getGraphicsContext2D();

        rowHeight = 40.0 + notation.getNumParagraph() * LYRIC_HEIGHT;
        rowParts = Math.round(16.0f / notation.getBeatsCount());
    }

    public TrashMusicNotation getNotation() {
        return notation;
    }

    void bindListeners() {
        canvas.setOnMouseClicked(this::onMouseClicked);
        canvas.setOnKeyTyped(this::onKeyTyped);
    }

    void unbindListeners() {
    }

    private void onMouseClicked(MouseEvent event) {
        double mouseX = event.getX();
        double mouseY = event.getY();
        int row = (int) ((mouseY + CLICK_DETECT_HEIGHT / 2 - TOP_Y) / rowHeight);
    }

    private void onKeyTyped(KeyEvent keyEvent) {

    }

    void draw() {
        graphics.setTextAlign(TextAlignment.CENTER);
        graphics.setFill(Color.BLACK);
        graphics.setStroke(Color.BLACK);

        x = LEFT_X;
        lastNoteX = x;
        y = TOP_Y;

        SoundPart activePart = notation.getSoundPart(0);

        double partsInterval = STD_WIDTH / 2;
        for (int p = 0; p < activePart.size(); ++p) {
            MeasurePart part = activePart.get(p);
            MeasurePartWrapper partWrapper = new MeasurePartWrapper(part, x, y);
            if (part.getPartType() == MeasurePart.PartType.BEGIN_OF_REPEAT) {
                double xx = x - partsInterval;
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
            if (part.getPartType() == MeasurePart.PartType.END_OF_REPEAT) {
                graphics.strokeLine(x, y - TEXT_HEIGHT, x, y + TEXT_HEIGHT);
                graphics.setLineWidth(3);
                graphics.strokeLine(x + 5, y - TEXT_HEIGHT + 1, x + 5, y + TEXT_HEIGHT - 1);
                graphics.setLineWidth(1);
                graphics.fillOval(x - 6.5, y - 6.5, 3, 3);
                graphics.fillOval(x - 6.5, y + 3.5, 3, 3);
            } else if (part.getPartType() == MeasurePart.PartType.REPEAT_FROM_START) {
                graphics.strokeLine(x, y - TEXT_HEIGHT, x, y + TEXT_HEIGHT);
                graphics.setLineWidth(3);
                graphics.strokeLine(x + 5, y - TEXT_HEIGHT + 1, x + 5, y + TEXT_HEIGHT - 1);
                graphics.setLineWidth(1);
            } else if (nextPart != null && nextPart.getPartType() != MeasurePart.PartType.BEGIN_OF_REPEAT) {
                graphics.strokeLine(x, y - TEXT_HEIGHT, x, y + TEXT_HEIGHT);
            }

            if (p % rowParts == rowParts - 1) {
                y += rowHeight;
                x = LEFT_X;
                if (y + rowHeight > canvas.getHeight()) {
                    canvas.setHeight(y + rowHeight);
                }
            } else {
                x += partsInterval;
            }
        }
    }

    private void drawMeasurePart(MeasurePartWrapper measurePartWrapper) {
        drawMeasurePart(measurePartWrapper, true);
    }

    private void drawMeasurePart(MeasurePartWrapper measurePartWrapper, boolean firstDraw) {
        if (!firstDraw) {  // 抹掉原本的小节
            graphics.setFill(Color.WHITE);
            graphics.fillRect(x, y - UNDERLINE_SPACING * 3, measurePartWrapper.endX, y + rowHeight);
            graphics.setFill(Color.BLACK);
        }
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
                positionNoteMap.put(new Position(x, y), noteWrapper);
                graphics.fillText(note.toStringMusical(), x, y + TEXT_HEIGHT / 3);

                // 歌词
                for (Map.Entry<Integer, Lyrics.LyricParagraph> entry : notation.getLyrics().entrySet()) {
                    Text lyric = entry.getValue().get(note);

                    if (lyric != null) {
                        double yy = y + LYRIC_HEIGHT * entry.getKey();
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
    }

    private static class TextWrapper implements ClickableWrapper {
        private Text text;
        private NoteWrapper noteWrapper;

        TextWrapper(Text text, NoteWrapper noteWrapper) {
            this.text = text;
            this.noteWrapper = noteWrapper;
        }
    }

    private static class Position {
        private final double x;
        private final double y;

        Position(double x, double y) {
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
    }
}
