package trashsoftware.trashMusic.fxml;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import trashsoftware.trashMusic.core.AudioPlayer;
import trashsoftware.trashMusic.core.Pitch;
import trashsoftware.trashMusic.core.PlainMeasurePart;
import trashsoftware.trashMusic.core.TrashMusicNotation;
import trashsoftware.trashMusic.core.wav.WavFile;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class EditorView implements Initializable {

    @FXML
    ToggleGroup timeValueGroup, sharpFlatGroup;

    @FXML
    CheckBox extendCheckBox;

    @FXML
    ScrollPane scrollPane;

    @FXML
    Button baseCHighBtn, baseCLowBtn;
    @FXML
    Label baseCLabel;

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

    @FXML
    Button highBtn, lowBtn;
    @FXML
    Label lowHighLabel;

    private GraphicsContext graphics;

    private NotationWrapper notationWrapper;
    private Stage stage;
    private ResourceBundle resources;

    private AudioPlayer audioPlayer;
    private long wavChecksum;  // 磁盘上wav文件对应的TrashMusicNotation校验码
    private long tmnChecksum;  // 磁盘上tmn文件的校验码

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.resources = resources;

        graphics = canvas.getGraphicsContext2D();

        setBoxes();
    }

    private void setBoxes() {
        for (int s = 40; s < 209; ++s) speedBox.getItems().add(s);
        basePitchBox.getItems().addAll("C", "♯C", "♭D", "D", "♯D", "♭E", "E", "F", "♯F", "♭G", "G", "♯G", "♭A",
                "A", "♯A", "♭B", "B");
        timeValueGroup.selectToggle(timeValueGroup.getToggles().get(2));
        sharpFlatGroup.selectToggle(sharpFlatGroup.getToggles().get(1));
        
        basePitchBox.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> 
                basePitchChanged()));
    }

    public void setup(Stage stage, File tmnFile) {
        this.stage = stage;
        setFile(tmnFile);
    }

    public void setFile(File tmnFile) {
        if (notationWrapper != null) {
            notationWrapper.unbindListeners();
//            basePitchBox.getSelectionModel().selectedItemProperty().removeListener();
        }
        this.notationWrapper = new NotationWrapper(TrashMusicNotation.fromTmnFile(tmnFile), this, canvas);
        wavChecksum = notationWrapper.getNotation().notationChecksum();
        tmnChecksum = wavChecksum;

        speedBox.getSelectionModel().select(Integer.valueOf(notationWrapper.getNotation().getSpeed()));
        basePitchBox.getSelectionModel().select(notationWrapper.getNotation().getBasePitch().toStringMusical());
        beatsCountLabel.setText(String.valueOf(notationWrapper.getNotation().getBeatsCount()));
        beatLengthLabel.setText(String.valueOf((int) (1 / notationWrapper.getNotation().getBeatLength())));
        
        baseCLabel.setText(String.valueOf(notationWrapper.getNotation().getBasePitch().getMajor()));

        notationWrapper.refresh();
        notationWrapper.bindListeners(scrollPane);
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
    
    private void basePitchChanged() {
        String letter = basePitchBox.getValue();
        String major = baseCLabel.getText();
        Pitch pitch = Pitch.fromStdRep(letter + major);
        notationWrapper.getNotation().setBasePitch(pitch);
    }

    @FXML
    void refreshWavAction() {
        notationWrapper.getNotation().writeWav(false);
    }

    @FXML
    void undoAction() {

    }

    @FXML
    void saveAction() throws IOException {
        notationWrapper.getNotation().saveAsTmn();
        tmnChecksum = notationWrapper.getNotation().notationChecksum();
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
            tmnChecksum = notationWrapper.getNotation().notationChecksum();
        }
    }

    @FXML
    void playAction() {
        playBtn.setDisable(true);
        pauseBtn.setDisable(false);
        stopBtn.setDisable(false);
        if (audioPlayer == null) {
            checkMusicFile();
            audioPlayer = new AudioPlayer(notationWrapper.getNotation().waveFile(), 
                    this::terminateCallback);
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

    @FXML
    void measurePartButtonsAction(ActionEvent buttonEvent) {
        Button button = (Button) buttonEvent.getSource();
        notationWrapper.addMeasurePart(button.getText().strip());
    }

    @FXML
    void baseCHighAction() {
        int cur = Integer.parseInt(baseCLabel.getText());
        baseCLabel.setText(String.valueOf(cur + 1));
        baseCLowBtn.setDisable(false);
        if (cur == 6) baseCHighBtn.setDisable(true);
        
        basePitchChanged();
    }

    @FXML
    void baseCLowAction() {
        int cur = Integer.parseInt(baseCLabel.getText());
        baseCLabel.setText(String.valueOf(cur - 1));
        baseCHighBtn.setDisable(false);
        if (cur == 2) baseCLowBtn.setDisable(true);
        
        basePitchChanged();
    }

    @FXML
    void highAction() {
        int cur = Integer.parseInt(lowHighLabel.getText());
        lowHighLabel.setText(String.valueOf(cur + 1));
        lowBtn.setDisable(false);
        if (cur == 2) highBtn.setDisable(true);
    }

    @FXML
    void lowAction() {
        int cur = Integer.parseInt(lowHighLabel.getText());
        lowHighLabel.setText(String.valueOf(cur - 1));
        highBtn.setDisable(false);
        if (cur == -2) lowBtn.setDisable(true);
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
        if (newChecksum == wavChecksum) {
            if (notationWrapper.getNotation().waveFile().exists()) return;
        }
        notationWrapper.getNotation().writeWav(false);
        wavChecksum = newChecksum;
    }
}
