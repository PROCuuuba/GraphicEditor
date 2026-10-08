package org.example.graphiceditor;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.graphiceditor.ui.EditorPane;

import java.util.Objects;

public class GraphicsEditorApplication extends Application {
    @Override
    public void start(Stage stage) {
        Scene scene = new Scene(new EditorPane(), 1280, 820);
        scene.getStylesheets().add(Objects.requireNonNull(
                GraphicsEditorApplication.class.getResource("/org/example/graphiceditor/css/application.css"),
                "Не найден ресурс application.css").toExternalForm());
        stage.setTitle("Графический редактор — растровый редактор");
        stage.setMinWidth(960);
        stage.setMinHeight(680);
        stage.setScene(scene);
        stage.show();
    }
}
