module trashMusic {
    requires javafx.base;
    requires javafx.fxml;
    requires javafx.controls;
    requires javafx.graphics;

    requires java.desktop;

    exports trashsoftware.trashMusic.fxml;
    exports trashsoftware.trashMusic.core;
    exports trashsoftware.trashMusic.core.wav;
    exports trashsoftware.trashMusic.core.eq;
    exports trashsoftware.trashMusic.core.volTransform;

    opens trashsoftware.trashMusic.fxml;
}