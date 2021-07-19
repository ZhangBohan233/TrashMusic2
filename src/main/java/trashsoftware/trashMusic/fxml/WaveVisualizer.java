package trashsoftware.trashMusic.fxml;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollBar;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import trashsoftware.trashMusic.core.wav.WavFile;

import java.net.URL;
import java.util.ResourceBundle;

public class WaveVisualizer implements Initializable {

    private static final double SECOND_TICKET_Y = 20.0;
    private static final double MILLS500_TICKET_Y = 16.0;
    private static final double MILLS100_TICKET_Y = 12.0;
    private static final double MILLS50_TICKET_Y = 8.0;
    private static final double TEXT_Y = 30.0;

    @FXML
    Canvas mainCanvas, timeBar;

    @FXML
    ScrollBar hScrollBar;

    private int channelIndex = 0;

    private double hGap;  // 每个采样之间的像素距离
    private double vMul;

    private int screenLeftFrame;

    private ResourceBundle resources;
    private WavFile wavFile;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.resources = resources;

        mainCanvas.getGraphicsContext2D().setFill(Color.WHITE);
        mainCanvas.getGraphicsContext2D().setStroke(Color.BLACK);
        timeBar.getGraphicsContext2D().setStroke(Color.BLACK);
        timeBar.getGraphicsContext2D().setTextAlign(TextAlignment.CENTER);

        hScrollBar.valueProperty().addListener(((observable, oldValue, newValue) -> {
            screenLeftFrame = (int) ((wavFile.getNumFrames() - screenFrameNum()) * newValue.doubleValue() / 100);
            refresh();
        }));
    }

    public void setup(WavFile wavFile) {
        this.wavFile = wavFile;

        calculateMultipliers();
        updateScrollBar();

        refresh();
    }

    private void calculateMultipliers() {
        hGap = mainCanvas.getWidth() / wavFile.getNumFrames();
        vMul = mainCanvas.getHeight() / maxHeight();
    }

    @FXML
    void zoomInHAction() {
        hGap *= 1.5;
        updateScrollBar();
        refresh();
    }

    @FXML
    void zoomOutHAction() {
        hGap /= 1.5;
        updateScrollBar();
        refresh();
    }

    @FXML
    void zoomInVAction() {
        calculateMultipliers();
        refresh();
    }

    @FXML
    void zoomOutVAction() {
        calculateMultipliers();
        refresh();
    }

    private void updateScrollBar() {
        hScrollBar.setVisibleAmount(mainCanvas.getWidth() / getTotalWidth() * 100.0);
    }

    private int maxHeight() {
        return wavFile.getBitsPerSample() == 16 ? 65536 : 256;
    }

    private void refresh() {
        refreshMain();
        refreshTimeBar();
    }

    private void refreshMain() {
        GraphicsContext graphics = mainCanvas.getGraphicsContext2D();
        graphics.fillRect(0, 0, mainCanvas.getWidth(), mainCanvas.getHeight());
        int[] data = wavFile.getChannel(channelIndex);

        int max = maxHeight();
        int half = max / 2;

        double x = 0, y = 0;
        for (int i = screenLeftFrame; i < Math.min(wavFile.getNumFrames(), screenLeftFrame + screenFrameNum()); ++i) {
            int datum = data[i];
            double lastX = x;
            double lastY = y;
            x += hGap;
            int sample = datum + half;
            if (sample > max) {
                sample -= max;
            }
            y = sample * vMul;
            graphics.strokeLine(lastX, lastY, x, y);
        }
    }

    private void refreshTimeBar() {
        GraphicsContext graphics = timeBar.getGraphicsContext2D();
        graphics.setFill(Color.WHITE);
        graphics.fillRect(0, 0, timeBar.getWidth(), timeBar.getHeight());
        graphics.setFill(Color.BLACK);

        double leftSec = (double) screenLeftFrame / wavFile.getSampleRate();
        double rightSec = (double) (screenLeftFrame + screenFrameNum()) / wavFile.getSampleRate();
        double interval = rightSec - leftSec;

        double tickLeft = leftSec - leftSec % 0.05;
        for (double sec = tickLeft; sec < rightSec; sec += 0.05) {
            double x = timeBarXofSecond(sec, leftSec, rightSec);
//            System.out.println(sec % 1.0 + " " + sec % 0.5 + " " + sec % 0.1);
            if (canDivide(sec, 1.0)) {
                graphics.strokeLine(x, 0, x, SECOND_TICKET_Y);
                graphics.fillText(secondToString(sec), x, TEXT_Y);
            }
            if (interval < 10) {
                if (canDivide(sec, 0.5)) {  // 浮点数误差
                    graphics.strokeLine(x, 0, x, MILLS500_TICKET_Y);
                }
            }
            if (interval < 5) {
                if (canDivide(sec, 0.1)) {  // 浮点数误差
                    graphics.strokeLine(x, 0, x, MILLS100_TICKET_Y);
                }
            }
            if (interval < 1) {
                if (canDivide(sec, 0.05)) {  // 浮点数误差
                    graphics.strokeLine(x, 0, x, MILLS50_TICKET_Y);
                }
            }
        }
    }

    private int screenFrameNum() {
        return (int) (mainCanvas.getWidth() / hGap) + 1;
    }

    private double getTotalWidth() {
        return hGap * wavFile.getNumFrames();
    }

    private double timeBarXofSecond(double second, double leftSec, double rightSec) {
        double width = timeBar.getWidth();
        double interval = rightSec - leftSec;
        return (second - leftSec) / interval * width;
    }

    private static String secondToString(double sec) {
        if (sec < 10) return "0" + (int) sec;
        else if (sec < 60) return String.valueOf((int) sec);
        else {
            return (int) sec / 60 + ":" + secondToString(sec % 60);
        }
    }

    private static boolean canDivide(double d, double divider) {
        double rem = d % divider;
        return rem < 0.0000001 || divider - rem < 0.0000001;
    }
}
