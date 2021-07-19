package trashsoftware.trashMusic.fxml;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

public class Client extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("mainView.fxml"),
                ResourceBundle.getBundle("trashsoftware.trashMusic.bundles.texts", Locale.getDefault())
        );
        Parent root = loader.load();

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);

        MainView mainView = loader.getController();
        mainView.setStage(primaryStage);

        primaryStage.show();
    }
}
