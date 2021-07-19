package trashsoftware.trashMusic.fxml;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import trashsoftware.trashMusic.core.AudioPlayer;
import trashsoftware.trashMusic.core.TrashMusicNotation;
import trashsoftware.trashMusic.core.wav.WavFile;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class EditorView implements Initializable {

    @FXML
    Label beatsCountLabel, beatLengthLabel;

    @FXML
    ComboBox<Integer> speedBox;

    @FXML
    ComboBox<String> basePitchBox;

    @FXML
    Canvas canvas;

    @FXML
    Button playBtn, pauseBtn, stopBtn;

    private GraphicsContext graphics;

    private NotationWrapper notationWrapper;
    private Stage stage;
    private ResourceBundle resources;

    private AudioPlayer audioPlayer;
    private long currentChecksum;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.resources = resources;

        graphics = canvas.getGraphicsContext2D();

        setBoxes();
    }

    private void setBoxes() {
        for (int s = 40; s < 209; ++s) speedBox.getItems().add(s);
        basePitchBox.getItems().addAll("C", "#C", "♭D", "D", "#D", "♭E", "E", "F", "#F", "♭G", "G", "#G", "♭A",
                "A", "#A", "♭B", "B");
    }

    public void setup(Stage stage, File tmnFile) {
        this.stage = stage;
        setFile(tmnFile);
    }

    public void setFile(File tmnFile) {
        if (notationWrapper != null) {
            notationWrapper.unbindListeners();
        }
        this.notationWrapper = new NotationWrapper(TrashMusicNotation.fromTmnFile(tmnFile), canvas);
        currentChecksum = notationWrapper.getNotation().notationChecksum();

        speedBox.getSelectionModel().select(Integer.valueOf(notationWrapper.getNotation().getSpeed()));
        basePitchBox.getSelectionModel().select(notationWrapper.getNotation().getBasePitch().toStringMusical());
        beatsCountLabel.setText(String.valueOf(notationWrapper.getNotation().getBeatsCount()));
        beatLengthLabel.setText(String.valueOf((int) (1 / notationWrapper.getNotation().getBeatLength())));

        long begin = System.currentTimeMillis();
        notationWrapper.draw();
        System.out.println(System.currentTimeMillis() - begin);
        notationWrapper.bindListeners();
    }

    @FXML
    void viewWaveAction() throws IOException {
        File wavFile = notationWrapper.getNotation().waveFile();
        checkMusicFile();

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("waveVisualizer.fxml"),
                resources
        );
        Parent root = loader.load();

        Stage newStage = new Stage();
        newStage.initOwner(stage);
        Scene scene = new Scene(root);
        newStage.setScene(scene);

        WaveVisualizer visualizer = loader.getController();
        visualizer.setup(WavFile.fromFile(wavFile));

        newStage.show();
    }

    @FXML
    void openFileAction() throws IOException {
        File file = chooseOpenFile(stage, resources);
        if (file != null) {
            setFile(file);
        }
    }

    private static File chooseOpenFile(Stage parentStage, ResourceBundle resources) {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(resources.getString("tmnFileDescription"), "*.tmn"));
        chooser.setInitialDirectory(new File("files"));  // todo: temporary
        return chooser.showOpenDialog(parentStage);
    }

    public static void openFile(Stage parentStage, ResourceBundle resources) throws IOException {
        File file = chooseOpenFile(parentStage, resources);
        if (file != null) {
            FXMLLoader loader = new FXMLLoader(
                    EditorView.class.getResource("editorView.fxml"),
                    resources
            );
            Parent root = loader.load();

            Stage newStage = new Stage();
            newStage.initOwner(parentStage);
            Scene scene = new Scene(root);
            newStage.setScene(scene);

            EditorView editorView = loader.getController();
            editorView.setup(newStage, file);

            newStage.show();
        }
    }

    @FXML
    void saveAction() throws IOException {
        notationWrapper.getNotation().saveAsTmn();
    }

    @FXML
    void saveAsAction() throws IOException {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(resources.getString("tmnFileDescription"), "*.tmn"));
        chooser.setInitialDirectory(EditorView.getInitDir());  // todo: temporary
        File file = chooser.showSaveDialog(stage);
        if (file != null) {
            notationWrapper.getNotation().saveAsTmn(file);
        }
    }

    @FXML
    void playAction() {
        playBtn.setDisable(true);
        pauseBtn.setDisable(false);
        stopBtn.setDisable(false);
        if (audioPlayer == null) {
            checkMusicFile();
            audioPlayer = new AudioPlayer(notationWrapper.getNotation().waveFile(), this::terminateCallback);
        }
        Thread thread = new Thread(() -> {
            try {
                audioPlayer.play();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    void pauseAction() {
        pauseBtn.setDisable(true);
        playBtn.setDisable(false);
        stopBtn.setDisable(false);
        if (audioPlayer != null) audioPlayer.pause();
    }

    @FXML
    void stopAction() throws IOException {
        playBtn.setDisable(false);
        pauseBtn.setDisable(true);
        stopBtn.setDisable(true);
        if (audioPlayer != null) audioPlayer.terminate();
    }

    private void terminateCallback() {
        Platform.runLater(() -> {
            audioPlayer = null;
            playBtn.setDisable(false);
            pauseBtn.setDisable(true);
            stopBtn.setDisable(true);
        });
    }

    public static File getInitDir() {
        return new File("files");
    }

    private void checkMusicFile() {
        long newChecksum = notationWrapper.getNotation().notationChecksum();
        if (newChecksum == currentChecksum) {
            if (notationWrapper.getNotation().waveFile().exists()) return;
        }
        notationWrapper.getNotation().writeWav(false);
        currentChecksum = newChecksum;
    }

//    private static class BasePitch {
//        private static BasePitch AFlat = new BasePitch("♭A", Pitch.fromStdRep("bA3"));
//        A("A", Pitch.fromStdRep("A3")),
//        ASharp("#A", Pitch.fromStdRep("#A3")),
//        BFlat("♭B", Pitch.fromStdRep("bB3")),
//        B("B", Pitch.fromStdRep("B3")),
//        C("C"),
//        CSharp("#C"),
//        DFlat("♭D"),
//        D("D"),
//        DSharp("#D"),
//        EFlat("♭E"),
//        E("E"),
//        F("F"),
//        FSharp("#F"),
//        GFlat("♭G"),
//        G("G"),
//        GSharp("#G");
//
//        private final String rep;
//        private final Pitch pitch;
//
//        BasePitch(String rep, Pitch pitch) {
//            this.rep = rep;
//            this.pitch = pitch;
//        }
//
//        BasePitch(String rep) {
//            this(rep, Pitch.fromStdRep(rep + "4"));
//        }
//
//        @Override
//        public String toString() {
//            return rep;
//        }
//
//        public Pitch getPitch() {
//            return pitch;
//        }
//    }
}
