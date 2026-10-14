package org.example.graphiceditor.ui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.PixelReader;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.example.graphiceditor.controller.EditorController;
import org.example.graphiceditor.model.Circle;
import org.example.graphiceditor.model.DrawingModel;
import org.example.graphiceditor.model.Point;
import org.example.graphiceditor.model.Square;
import org.example.graphiceditor.model.Tool;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("gui")
class EditorAcceptanceTest {
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

    @Test
    void canvasRendersWhiteBackgroundAndMixedPixelShapes() throws Exception {
        withEditor(editor -> {
            assertWhite(editor.canvas());

            editor.model().add(new Point(20.8, 20.9, 64, Color.RED));
            editor.model().add(new Circle(100, 100, 30, Color.GREEN));
            editor.model().add(new Square(200, 100, 30, Color.BLUE));
            editor.canvas().renderAll();

            PixelReader image = snapshot(editor.canvas());
            assertColor(image, 20, 20, Color.RED);
            assertColor(image, 21, 20, Color.WHITE);
            assertColor(image, 20, 21, Color.WHITE);
            assertColor(image, 100, 100, Color.GREEN);
            assertColor(image, 87, 87, Color.WHITE);
            assertColor(image, 200, 100, Color.BLUE);
            assertColor(image, 187, 87, Color.BLUE);
            assertColor(image, 184, 100, Color.WHITE);
        });
    }

    @Test
    void toolButtonsAlwaysKeepExactlyOneToolSelected() throws Exception {
        withEditor(editor -> {
            assertTrue(editor.button("tool-point").isSelected());
            Tool[] tools = {Tool.CIRCLE, Tool.SQUARE, Tool.POINT};
            String[] ids = {"tool-circle", "tool-square", "tool-point"};

            for (int index = 0; index < tools.length; index++) {
                ToggleButton button = editor.button(ids[index]);
                button.fire();
                assertEquals(tools[index], editor.controller().getTool());
                assertTrue(button.isSelected());
                assertEquals(1, selectedTools(editor));

                button.fire();
                assertEquals(tools[index], editor.controller().getTool());
                assertTrue(button.isSelected());
                assertEquals(1, selectedTools(editor));
            }
        });
    }

    @Test
    void primaryDraggingProducesContinuousPointStrokeAndStopsAfterRelease() throws Exception {
        withEditor(editor -> {
            chooseColor(editor, Color.RED);
            mouse(editor.canvas(), MouseEvent.MOUSE_PRESSED, 40.2, 50.2, MouseButton.PRIMARY, true);
            mouse(editor.canvas(), MouseEvent.MOUSE_DRAGGED, 65.2, 50.2, MouseButton.PRIMARY, true);
            mouse(editor.canvas(), MouseEvent.MOUSE_DRAGGED, 85.2, 70.2, MouseButton.PRIMARY, true);
            mouse(editor.canvas(), MouseEvent.MOUSE_RELEASED, 85.2, 70.2, MouseButton.PRIMARY, false);

            PixelReader image = snapshot(editor.canvas());
            for (int x = 40; x <= 65; x++) {
                assertColor(image, x, 50, Color.RED);
            }
            for (int x = 65; x <= 85; x++) {
                assertColor(image, x, x - 15, Color.RED);
            }
            assertColor(image, 55, 49, Color.WHITE);
            assertColor(image, 55, 51, Color.WHITE);

            int pixelsAfterRelease = editor.model().size();
            mouse(editor.canvas(), MouseEvent.MOUSE_MOVED, 90, 50, MouseButton.NONE, false);
            mouse(editor.canvas(), MouseEvent.MOUSE_DRAGGED, 110, 50, MouseButton.PRIMARY, false);
            assertEquals(pixelsAfterRelease, editor.model().size());
            assertColor(snapshot(editor.canvas()), 90, 50, Color.WHITE);
        });
    }

    @Test
    void secondaryAndMiddleMouseButtonsDoNotDraw() throws Exception {
        withEditor(editor -> {
            for (MouseButton button : new MouseButton[]{MouseButton.SECONDARY, MouseButton.MIDDLE}) {
                mouse(editor.canvas(), MouseEvent.MOUSE_PRESSED, 40, 40, button, false);
                mouse(editor.canvas(), MouseEvent.MOUSE_DRAGGED, 100, 40, button, false);
                mouse(editor.canvas(), MouseEvent.MOUSE_RELEASED, 100, 40, button, false);
            }
            assertEquals(0, editor.model().size());
            assertWhite(editor.canvas());
        });
    }

    @Test
    void leavingCanvasBreaksTheStrokeConnection() throws Exception {
        withEditor(editor -> {
            chooseColor(editor, Color.RED);
            mouse(editor.canvas(), MouseEvent.MOUSE_PRESSED, 20.2, 20.2, MouseButton.PRIMARY, true);
            mouse(editor.canvas(), MouseEvent.MOUSE_DRAGGED, 30.2, 20.2, MouseButton.PRIMARY, true);
            mouse(editor.canvas(), MouseEvent.MOUSE_EXITED, -1, 20.2, MouseButton.PRIMARY, true);
            mouse(editor.canvas(), MouseEvent.MOUSE_DRAGGED, 200.2, 20.2, MouseButton.PRIMARY, true);
            mouse(editor.canvas(), MouseEvent.MOUSE_RELEASED, 200.2, 20.2, MouseButton.PRIMARY, false);

            PixelReader image = snapshot(editor.canvas());
            assertColor(image, 20, 20, Color.RED);
            assertColor(image, 30, 20, Color.RED);
            assertColor(image, 200, 20, Color.RED);
            assertColor(image, 100, 20, Color.WHITE);
        });
    }

    @Test
    void changingColorAndSizeAffectsOnlyNewBrushMarks() throws Exception {
        withEditor(editor -> {
            editor.button("tool-circle").fire();
            chooseColor(editor, Color.RED);
            editor.brush().setValue(10);
            click(editor.canvas(), 100, 100);

            editor.button("tool-square").fire();
            chooseColor(editor, Color.BLUE);
            editor.brush().setValue(30);
            click(editor.canvas(), 200, 100);

            assertEquals(2, editor.model().size());
            assertEquals(Circle.class, editor.model().getPixels().get(0).getClass());
            assertEquals(Color.RED, editor.model().getPixels().get(0).getColor());
            assertEquals(10, editor.model().getPixels().get(0).getSize());
            assertEquals(Square.class, editor.model().getPixels().get(1).getClass());
            assertEquals(Color.BLUE, editor.model().getPixels().get(1).getColor());
            assertEquals(30, editor.model().getPixels().get(1).getSize());

            PixelReader image = snapshot(editor.canvas());
            assertColor(image, 100, 100, Color.RED);
            assertColor(image, 106, 100, Color.WHITE);
            assertColor(image, 200, 100, Color.BLUE);
            assertColor(image, 187, 100, Color.BLUE);
        });
    }

    @Test
    void clearRemovesMarksAndPreservesTheSelectedBrush() throws Exception {
        withEditor(editor -> {
            editor.button("tool-square").fire();
            chooseColor(editor, Color.BLUE);
            editor.brush().setValue(30);
            click(editor.canvas(), 100, 100);
            assertFalse(editor.model().getPixels().isEmpty());

            editor.node("clear-canvas", Button.class).fire();

            assertEquals(0, editor.model().size());
            assertWhite(editor.canvas());
            assertTrue(editor.button("tool-square").isSelected());
            assertEquals(1, selectedTools(editor));
            assertEquals(Tool.SQUARE, editor.controller().getTool());
            assertEquals(Color.BLUE, editor.controller().getColor());
            assertEquals(30, editor.controller().getBrushSize());
            assertTrue(editor.node("drawing-status", Label.class).getText().contains("0"));

            click(editor.canvas(), 200, 100);
            assertEquals(1, editor.model().size());
            assertColor(snapshot(editor.canvas()), 200, 100, Color.BLUE);
        });
    }

    @Test
    void pointsAtFractionalCoordinatesAndCanvasLimitsOccupyExactlyOnePixel() throws Exception {
        withEditor(editor -> {
            chooseColor(editor, Color.RED);
            click(editor.canvas(), 0, 0);
            click(editor.canvas(), 899.99, 649.99);
            click(editor.canvas(), 125.99, 75.99);
            PixelReader image = snapshot(editor.canvas());
            assertColor(image, 0, 0, Color.RED);
            assertColor(image, 1, 0, Color.WHITE);
            assertColor(image, 899, 649, Color.RED);
            assertColor(image, 898, 649, Color.WHITE);
            assertColor(image, 125, 75, Color.RED);
            assertColor(image, 126, 75, Color.WHITE);
            assertEquals(3, editor.model().size());
        });
    }

    @Test
    void brushesAreClippedAtTheCanvasEdges() throws Exception {
        withEditor(editor -> {
            editor.button("tool-square").fire();
            chooseColor(editor, Color.BLUE);
            editor.brush().setValue(30);
            click(editor.canvas(), 0, 0);
            click(editor.canvas(), 899, 649);
            PixelReader image = snapshot(editor.canvas());
            assertColor(image, 0, 0, Color.BLUE);
            assertColor(image, 14, 14, Color.BLUE);
            assertColor(image, 16, 16, Color.WHITE);
            assertColor(image, 899, 649, Color.BLUE);
            assertColor(image, 883, 633, Color.WHITE);
            assertEquals(2, editor.model().size());
        });
    }

    @Test
    void headingsAndControlsFitTheMinimumWindowSize() throws Exception {
        withEditor(960, 680, editor -> {
            for (javafx.scene.Node node : editor.pane().lookupAll(".section-title")) {
                Label label = (Label) node;
                assertTrue(label.getWidth() >= label.prefWidth(-1));
                assertTrue(label.getHeight() >= label.prefHeight(-1));
            }
            assertTrue(editor.node("clear-canvas", Button.class).localToScene(
                    editor.node("clear-canvas", Button.class).getBoundsInLocal()).getMaxY()
                    < editor.pane().getScene().getHeight());
            assertEquals(900, editor.canvas().getWidth());
            assertEquals(650, editor.canvas().getHeight());
        });
    }

    private static void withEditor(Consumer<Editor> assertions) throws Exception {
        withEditor(1320, 850, assertions);
    }

    private static void withEditor(double width, double height, Consumer<Editor> assertions) throws Exception {
        runFx(() -> {
            DrawingModel model = new DrawingModel(900, 650);
            EditorController controller = new EditorController(model);
            EditorPane pane = new EditorPane(model, controller);
            Stage stage = new Stage();
            try {
                Scene scene = new Scene(pane, width, height);
                scene.getStylesheets().add(Objects.requireNonNull(
                        EditorAcceptanceTest.class.getResource(
                                "/org/example/graphiceditor/css/application.css")).toExternalForm());
                stage.setScene(scene);
                stage.show();
                pane.applyCss();
                pane.layout();
                DrawingCanvas canvas = (DrawingCanvas) pane.lookup("#drawing-canvas");
                assertNotNull(canvas, "Холст должен быть доступен по ID drawing-canvas");
                assertions.accept(new Editor(model, controller, pane, canvas));
            } finally {
                stage.close();
            }
            return null;
        });
    }

    private static int selectedTools(Editor editor) {
        int count = 0;
        for (String id : new String[]{"tool-point", "tool-circle", "tool-square"}) {
            if (editor.button(id).isSelected()) {
                count++;
            }
        }
        return count;
    }

    private static void click(DrawingCanvas canvas, double x, double y) {
        mouse(canvas, MouseEvent.MOUSE_PRESSED, x, y, MouseButton.PRIMARY, true);
        mouse(canvas, MouseEvent.MOUSE_RELEASED, x, y, MouseButton.PRIMARY, false);
    }

    private static void chooseColor(Editor editor, Color color) {
        ColorPicker picker = editor.colors();
        picker.setValue(color);
        picker.fireEvent(new ActionEvent(picker, picker));
    }

    private static void mouse(DrawingCanvas canvas, EventType<MouseEvent> type,
                              double x, double y, MouseButton button, boolean primaryDown) {
        Point2D screen = canvas.localToScreen(x, y);
        MouseEvent event = new MouseEvent(type,
                x, y, screen.getX(), screen.getY(), button, 1,
                false, false, false, false, primaryDown,
                button == MouseButton.MIDDLE && type != MouseEvent.MOUSE_RELEASED,
                button == MouseButton.SECONDARY && type != MouseEvent.MOUSE_RELEASED,
                true, false, type == MouseEvent.MOUSE_PRESSED,
                new PickResult(canvas, new Point3D(x, y, 0), 0));
        Event.fireEvent(canvas, event);
    }

    private static PixelReader snapshot(DrawingCanvas canvas) {
        return canvas.snapshot(null, null).getPixelReader();
    }

    private static void assertWhite(DrawingCanvas canvas) {
        PixelReader image = snapshot(canvas);
        for (int y = 0; y < (int) canvas.getHeight(); y++) {
            for (int x = 0; x < (int) canvas.getWidth(); x++) {
                if (image.getArgb(x, y) != 0xffffffff) {
                    assertColor(image, x, y, Color.WHITE);
                }
            }
        }
    }

    private static void assertColor(PixelReader image, int x, int y, Color expected) {
        assertEquals(expected, image.getColor(x, y), "Цвет точки (" + x + ", " + y + ")");
    }

    private static <T> T runFx(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(10, TimeUnit.SECONDS);
    }

    private record Editor(DrawingModel model, EditorController controller,
                          EditorPane pane, DrawingCanvas canvas) {
        private <T extends Node> T node(String id, Class<T> type) {
            Node node = pane.lookup("#" + id);
            assertNotNull(node, "Элемент должен быть доступен по ID " + id);
            return type.cast(node);
        }

        private ToggleButton button(String id) {
            return node(id, ToggleButton.class);
        }

        private ColorPicker colors() {
            return node("color-picker", ColorPicker.class);
        }

        private Slider brush() {
            return node("brush-size", Slider.class);
        }
    }
}
