package trashsoftware.trashMusic.fxml;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import trashsoftware.trashMusic.core.wav.WavFile;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;

public class MainView implements Initializable {

    private ResourceBundle resources;
    private Stage stage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.resources = resources;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    void createNewAction() {

    }

    @FXML
    void openAction() throws IOException {
        EditorView.openFile(stage, resources);
    }

    @FXML
    void viewWaveAction() throws IOException {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(resources.getString("waveFileDescription"), "*.wav"));
        chooser.setInitialDirectory(EditorView.getInitDir());  // todo: temporary
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
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
            visualizer.setup(WavFile.fromFile(file));

            newStage.show();
        }
    }
}
