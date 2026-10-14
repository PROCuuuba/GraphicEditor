package org.example.graphiceditor.ui;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Spinner;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.PixelReader;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.example.graphiceditor.controller.EditorController;
import org.example.graphiceditor.model.DrawingModel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

abstract class VectorGuiTestSupport {
    @BeforeAll
    static void startJavaFx() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            started.countDown();
        });
        assertTrue(started.await(10, TimeUnit.SECONDS), "JavaFX должен запуститься за 10 секунд");
    }

    @AfterAll
    static void stopJavaFx() {
        Platform.exit();
    }

    static void withEditor(Consumer<Editor> assertions) throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            DrawingModel model = new DrawingModel(900, 650);
            EditorPane pane = new EditorPane(model, new EditorController(model));
            Stage stage = new Stage();
            try {
                Scene scene = new Scene(pane, 1250, 850);
                scene.getStylesheets().add(Objects.requireNonNull(
                        VectorGuiTestSupport.class.getResource(
                                "/org/example/graphiceditor/css/application.css")).toExternalForm());
                stage.setScene(scene);
                stage.show();
                pane.applyCss();
                pane.layout();
                DrawingCanvas canvas = (DrawingCanvas) pane.lookup("#drawing-canvas");
                assertNotNull(canvas);
                CanvasViewport viewport = (CanvasViewport) pane.lookup("#canvas-viewport");
                assertNotNull(viewport);
                viewport.setZoom(1);
                pane.applyCss();
                pane.layout();
                Editor editor = new Editor(model, pane, canvas);
                editor.node("mode-polygon", ToggleButton.class).fire();
                assertions.accept(editor);
            } finally {
                stage.close();
            }
            return null;
        });
        Platform.runLater(task);
        task.get(15, TimeUnit.SECONDS);
    }

    static void click(Editor editor, double x, double y) {
        mouse(editor.canvas(), MouseEvent.MOUSE_PRESSED, x, y, true);
        mouse(editor.canvas(), MouseEvent.MOUSE_RELEASED, x, y, false);
    }

    private static void mouse(DrawingCanvas canvas, EventType<MouseEvent> type,
                              double x, double y, boolean primaryDown) {
        Point2D screen = canvas.localToScreen(x, y);
        assertNotNull(screen);
        MouseEvent event = new MouseEvent(type, x, y, screen.getX(), screen.getY(), MouseButton.PRIMARY, 1,
                false, false, false, false, primaryDown, false, false,
                true, false, type == MouseEvent.MOUSE_PRESSED,
                new PickResult(canvas, new Point3D(x, y, 0), 0));
        Event.fireEvent(canvas, event);
    }

    static void finishSquare(Editor editor, double x, double y, double size) {
        click(editor, x, y);
        click(editor, x + size, y);
        click(editor, x + size, y + size);
        click(editor, x, y + size);
        finish(editor);
    }

    static void finish(Editor editor) {
        Button button = editor.node("polygon-finish", Button.class);
        assertFalse(button.isDisabled());
        button.fire();
        assertTrue(button.isDisabled());
    }

    static void enable(Editor editor, String id, boolean enabled) {
        CheckBox box = editor.node(id, CheckBox.class);
        if (box.isSelected() != enabled) box.fire();
        assertEquals(enabled, box.isSelected());
    }

    static void setColor(Editor editor, String id, Color color) {
        editor.node(id, ColorPicker.class).setValue(color);
    }

    static void setSpinner(Editor editor, String id, double value) {
        @SuppressWarnings("unchecked")
        Spinner<Double> spinner = (Spinner<Double>) editor.node(id, Spinner.class);
        spinner.getValueFactory().setValue(value);
        assertEquals(value, spinner.getValue());
    }

    static PixelReader image(Editor editor) {
        editor.canvas().setSelectedShape(null);
        editor.canvas().renderAll();
        return editor.canvas().snapshot(null, null).getPixelReader();
    }

    static void assertColor(PixelReader image, int x, int y, Color expected) {
        assertEquals(expected, image.getColor(x, y), "Цвет точки (" + x + ", " + y + ")");
    }

    record Editor(DrawingModel model, EditorPane pane, DrawingCanvas canvas) {
        <T extends Node> T node(String id, Class<T> type) {
            Node node = pane.lookup("#" + id);
            assertNotNull(node, "Элемент должен быть доступен по ID " + id);
            return type.cast(node);
        }
    }
}
