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
import javafx.scene.control.Control;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.PixelReader;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.example.graphiceditor.controller.EditorController;
import org.example.graphiceditor.model.DrawingModel;
import org.example.graphiceditor.model.Point;
import org.example.graphiceditor.model.vector.BorderDecorator;
import org.example.graphiceditor.model.vector.FillDecorator;
import org.example.graphiceditor.model.vector.Polygon;
import org.example.graphiceditor.model.vector.ShadowDecorator;
import org.example.graphiceditor.model.vector.Shape;
import org.example.graphiceditor.model.vector.VectorStyle;
import org.example.graphiceditor.model.vector.Vertex;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("gui")
class VectorAcceptanceTest {
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
    void polygonModeShowsItsControlsAndRequiresThreeVertices() throws Exception {
        withEditor(editor -> {
            assertTrue(editor.toggle("mode-raster").isSelected());
            assertFalse(effectivelyVisible(editor.node("polygon-finish", Button.class)));
            editor.toggle("mode-polygon").fire();
            assertTrue(editor.toggle("mode-polygon").isSelected());
            assertTrue(effectivelyVisible(editor.node("polygon-finish", Button.class)));
            assertTrue(editor.node("polygon-finish", Button.class).isDisabled());

            click(editor.canvas(), 100, 100);
            click(editor.canvas(), 220, 100);
            assertTrue(editor.node("polygon-finish", Button.class).isDisabled());
            assertTrue(editor.model().getShapes().isEmpty());
            click(editor.canvas(), 100, 220);
            assertFalse(editor.node("polygon-finish", Button.class).isDisabled());
            editor.node("polygon-finish", Button.class).fire();

            assertEquals(1, editor.model().getShapes().size());
            assertEquals(List.of(new Vertex(100, 100), new Vertex(220, 100),
                    new Vertex(100, 220)), editor.model().getShapes().getFirst().getVertices());
            assertTrue(editor.model().getPixels().isEmpty());
            assertTrue(editor.node("polygon-finish", Button.class).isDisabled());
            editor.canvas().setSelectedShape(null);
            editor.canvas().renderAll();
            PixelReader image = snapshot(editor.canvas());
            assertColor(image, 160, 100, Color.BLACK);
            assertColor(image, 130, 130, Color.WHITE);
        });
    }

    @Test
    void cancelRemovesTheDraftWithoutAddingOrDeletingFinishedPolygons() throws Exception {
        withEditor(editor -> {
            editor.toggle("mode-polygon").fire();
            finishSquare(editor, 100, 100, 80);
            Shape finished = editor.model().getShapes().getFirst();
            click(editor.canvas(), 350, 100);
            click(editor.canvas(), 450, 100);
            assertFalse(editor.node("polygon-cancel", Button.class).isDisabled());

            editor.node("polygon-cancel", Button.class).fire();

            assertEquals(List.of(finished), editor.model().getShapes());
            assertTrue(editor.node("polygon-finish", Button.class).isDisabled());
            editor.canvas().setSelectedShape(null);
            editor.canvas().renderAll();
            assertColor(snapshot(editor.canvas()), 400, 100, Color.WHITE);
        });
    }

    @Test
    void finishingSeveralPolygonsMaintainsSeparateGeometryAndSelectsTheLatest() throws Exception {
        withEditor(editor -> {
            editor.toggle("mode-polygon").fire();
            finishSquare(editor, 100, 100, 80);
            finishSquare(editor, 300, 100, 100);
            assertEquals(2, editor.model().getShapes().size());
            ListView<?> polygons = editor.node("polygon-list", ListView.class);
            assertEquals(2, polygons.getItems().size());
            assertEquals(1, polygons.getSelectionModel().getSelectedIndex());
            assertEquals(new Vertex(100, 100), editor.model().getShapes().getFirst().getVertices().getFirst());
            assertEquals(new Vertex(300, 100), editor.model().getShapes().getLast().getVertices().getFirst());
            assertTrue(editor.model().getPixels().isEmpty());
        });
    }

    @Test
    void applyChangesOnlyTheSelectedPolygonAndDrawsFillBorderAndShadow() throws Exception {
        withEditor(editor -> {
            editor.toggle("mode-polygon").fire();
            finishSquare(editor, 100, 100, 80);
            finishSquare(editor, 400, 100, 80);
            Shape untouched = editor.model().getShapes().getLast();
            Shape original = editor.model().getShapes().getFirst();
            editor.node("polygon-list", ListView.class).getSelectionModel().select(0);
            enable(editor, "fill-enabled", true);
            editor.node("fill-color", ColorPicker.class).setValue(Color.RED);
            enable(editor, "border-enabled", true);
            editor.node("border-color", ColorPicker.class).setValue(Color.BLACK);
            setSpinner(editor, "border-width", 6);
            enable(editor, "shadow-enabled", true);
            editor.node("shadow-color", ColorPicker.class).setValue(Color.MAGENTA);
            setSpinner(editor, "shadow-x", 40);
            setSpinner(editor, "shadow-y", 30);
            setSpinner(editor, "shadow-blur", 0);
            editor.node("polygon-apply", Button.class).fire();

            Shape updated = editor.model().getShapes().getFirst();
            assertNotSame(original, updated);
            assertEquals(original.getVertices(), updated.getVertices());
            assertSame(untouched, editor.model().getShapes().getLast());
            assertEquals(new VectorStyle(true, Color.RED, true, Color.BLACK, 6,
                    true, Color.MAGENTA, 40, 30, 0), VectorStyle.from(updated));
            editor.canvas().setSelectedShape(null);
            editor.canvas().renderAll();
            PixelReader image = snapshot(editor.canvas());
            assertColor(image, 140, 140, Color.RED);
            assertColor(image, 140, 100, Color.BLACK);
            assertColor(image, 200, 170, Color.MAGENTA);
            assertColor(image, 440, 140, Color.WHITE);
        });
    }

    @Test
    void selectingAnotherPolygonRestoresItsOwnStyleAndDeleteRemovesOnlySelection() throws Exception {
        withEditor(editor -> {
            editor.toggle("mode-polygon").fire();
            finishSquare(editor, 100, 100, 80);
            enable(editor, "fill-enabled", true);
            editor.node("fill-color", ColorPicker.class).setValue(Color.RED);
            editor.node("polygon-apply", Button.class).fire();
            finishSquare(editor, 400, 100, 80);
            enable(editor, "fill-enabled", true);
            editor.node("fill-color", ColorPicker.class).setValue(Color.BLUE);
            editor.node("polygon-apply", Button.class).fire();
            Shape second = editor.model().getShapes().getLast();

            ListView<?> polygons = editor.node("polygon-list", ListView.class);
            polygons.getSelectionModel().select(0);
            assertEquals(Color.RED, editor.node("fill-color", ColorPicker.class).getValue());
            polygons.getSelectionModel().select(1);
            assertEquals(Color.BLUE, editor.node("fill-color", ColorPicker.class).getValue());
            polygons.getSelectionModel().select(0);
            editor.node("polygon-delete", Button.class).fire();

            assertEquals(List.of(second), editor.model().getShapes());
            assertEquals(1, polygons.getItems().size());
            editor.canvas().setSelectedShape(null);
            editor.canvas().renderAll();
            assertColor(snapshot(editor.canvas()), 140, 140, Color.WHITE);
            assertColor(snapshot(editor.canvas()), 440, 140, Color.BLUE);
        });
    }

    @Test
    void rasterDrawingStillWorksAfterPolygonDrawingAndVectorsStayAboveRaster() throws Exception {
        withEditor(editor -> {
            editor.toggle("mode-polygon").fire();
            finishSquare(editor, 100, 100, 80);
            enable(editor, "fill-enabled", true);
            editor.node("fill-color", ColorPicker.class).setValue(Color.RED);
            editor.node("polygon-apply", Button.class).fire();
            editor.toggle("mode-raster").fire();
            editor.toggle("tool-square").fire();
            editor.node("brush-size", Slider.class).setValue(20);
            editor.node("color-picker", ColorPicker.class).setValue(Color.BLUE);
            click(editor.canvas(), 300, 150);
            click(editor.canvas(), 140, 140);
            assertEquals(2, editor.model().getPixels().size());
            assertEquals(1, editor.model().getShapes().size());
            editor.canvas().setSelectedShape(null);
            editor.canvas().renderAll();
            PixelReader image = snapshot(editor.canvas());
            assertColor(image, 300, 150, Color.BLUE);
            assertColor(image, 140, 140, Color.RED);
        });
    }

    @Test
    void clearRemovesRasterPolygonsAndUnfinishedDraft() throws Exception {
        withEditor(editor -> {
            click(editor.canvas(), 50, 50);
            editor.toggle("mode-polygon").fire();
            finishSquare(editor, 100, 100, 80);
            click(editor.canvas(), 400, 100);
            click(editor.canvas(), 480, 100);
            editor.node("clear-canvas", Button.class).fire();

            assertTrue(editor.model().getPixels().isEmpty());
            assertTrue(editor.model().getShapes().isEmpty());
            ListView<?> polygons = editor.node("polygon-list", ListView.class);
            assertTrue(polygons.getItems().isEmpty());
            assertEquals(-1, polygons.getSelectionModel().getSelectedIndex());
            assertTrue(editor.node("polygon-finish", Button.class).isDisabled());
            assertTrue(editor.node("polygon-apply", Button.class).isDisabled());
            assertTrue(editor.node("polygon-delete", Button.class).isDisabled());
            assertWhite(editor.canvas());
            click(editor.canvas(), 200, 200);
            assertTrue(editor.node("polygon-finish", Button.class).isDisabled());
            assertTrue(editor.model().getShapes().isEmpty());
        });
    }

    @Test
    void allDecoratorOrdersRenderEquivalentFillBorderAndShadowWithoutStateLeaks() throws Exception {
        withEditor(editor -> {
            List<List<String>> orders = List.of(
                    List.of("fill", "border", "shadow"), List.of("fill", "shadow", "border"),
                    List.of("border", "fill", "shadow"), List.of("border", "shadow", "fill"),
                    List.of("shadow", "fill", "border"), List.of("shadow", "border", "fill"));
            for (int index = 0; index < orders.size(); index++) {
                Shape shape = square(60 + index * 120, 60, 60);
                for (String effect : orders.get(index)) {
                    shape = switch (effect) {
                        case "fill" -> new FillDecorator(shape, Color.RED);
                        case "border" -> new BorderDecorator(shape, Color.BLACK, 6);
                        case "shadow" -> new ShadowDecorator(shape, Color.MAGENTA, 0, 140, 0);
                        default -> throw new IllegalStateException(effect);
                    };
                }
                editor.model().addShape(shape);
            }
            editor.model().addShape(new FillDecorator(square(60, 350, 60), Color.BLUE));
            editor.model().add(new Point(60, 500, 1, Color.GREEN));
            editor.canvas().renderAll();
            PixelReader image = snapshot(editor.canvas());
            for (int index = 0; index < orders.size(); index++) {
                int centerX = 90 + index * 120;
                assertColor(image, centerX, 90, Color.RED);
                assertColor(image, centerX, 60, Color.BLACK);
                assertColor(image, centerX, 230, Color.MAGENTA);
            }
            assertColor(image, 90, 380, Color.BLUE);
            assertColor(image, 57, 380, Color.WHITE);
            assertColor(image, 90, 520, Color.WHITE);
            assertColor(image, 60, 500, Color.GREEN);
        });
    }

    @Test
    void blurredShadowSoftensItsEdgesAndDoesNotAffectFollowingRasterMarks() throws Exception {
        withEditor(editor -> {
            Shape polygon = new Polygon(List.of(new Vertex(50, 50), new Vertex(100, 50),
                    new Vertex(100, 100), new Vertex(50, 100)));
            editor.model().addShape(new ShadowDecorator(polygon, Color.BLACK, 80, 40, 10));
            editor.canvas().renderAll();
            PixelReader image = snapshot(editor.canvas());
            double edge = image.getColor(182, 115).getRed();
            assertTrue(edge > 0 && edge < 1);
            assertColor(image, 220, 115, Color.WHITE);
            Point point = new Point(230.5, 200.5, 1, Color.RED);
            editor.model().add(point);
            editor.canvas().drawPixels(List.of(point));
            assertColor(snapshot(editor.canvas()), 230, 200, Color.RED);
            assertColor(snapshot(editor.canvas()), 231, 200, Color.WHITE);
        });
    }

    @Test
    void polygonControlTooltipsCanBeDisplayedAsActualPopups() throws Exception {
        withEditor(editor -> {
            editor.toggle("mode-polygon").fire();
            for (String id : List.of("mode-polygon", "polygon-finish", "polygon-cancel",
                    "fill-enabled", "border-enabled", "shadow-enabled", "polygon-apply")) {
                Control control = editor.node(id, Control.class);
                Tooltip tooltip = control.getTooltip();
                assertNotNull(tooltip, "Подсказка должна быть задана для " + id);
                assertFalse(tooltip.getText().isBlank());
                Point2D screen = control.localToScreen(0, 0);
                assertNotNull(screen);
                try {
                    tooltip.show(control, screen.getX() + 10, screen.getY() + 10);
                    assertTrue(tooltip.isShowing(), "Подсказка должна открываться для " + id);
                } finally {
                    tooltip.hide();
                }
            }
        });
    }

    private static Polygon square(double x, double y, double size) {
        return new Polygon(List.of(new Vertex(x, y), new Vertex(x + size, y),
                new Vertex(x + size, y + size), new Vertex(x, y + size)));
    }

    private static void finishSquare(Editor editor, double x, double y, double size) {
        click(editor.canvas(), x, y);
        click(editor.canvas(), x + size, y);
        click(editor.canvas(), x + size, y + size);
        click(editor.canvas(), x, y + size);
        assertFalse(editor.node("polygon-finish", Button.class).isDisabled());
        editor.node("polygon-finish", Button.class).fire();
    }

    private static void enable(Editor editor, String id, boolean enabled) {
        CheckBox box = editor.node(id, CheckBox.class);
        if (box.isSelected() != enabled) {
            box.fire();
        }
        assertEquals(enabled, box.isSelected());
    }

    private static void setSpinner(Editor editor, String id, double value) {
        @SuppressWarnings("unchecked")
        Spinner<Double> spinner = (Spinner<Double>) editor.node(id, Spinner.class);
        spinner.getValueFactory().setValue(value);
    }

    private static boolean effectivelyVisible(Node node) {
        for (Node current = node; current != null; current = current.getParent()) {
            if (!current.isVisible()) {
                return false;
            }
        }
        return true;
    }

    private static void withEditor(Consumer<Editor> assertions) throws Exception {
        runFx(() -> {
            DrawingModel model = new DrawingModel(900, 650);
            EditorController controller = new EditorController(model);
            EditorPane pane = new EditorPane(model, controller);
            Stage stage = new Stage();
            try {
                Scene scene = new Scene(pane, 1250, 850);
                scene.getStylesheets().add(Objects.requireNonNull(
                        VectorAcceptanceTest.class.getResource(
                                "/org/example/graphiceditor/css/application.css")).toExternalForm());
                stage.setScene(scene);
                stage.show();
                pane.applyCss();
                pane.layout();
                DrawingCanvas canvas = (DrawingCanvas) pane.lookup("#drawing-canvas");
                assertNotNull(canvas);
                assertions.accept(new Editor(model, pane, canvas));
            } finally {
                stage.close();
            }
            return null;
        });
    }

    private static void click(DrawingCanvas canvas, double x, double y) {
        mouse(canvas, MouseEvent.MOUSE_PRESSED, x, y, MouseButton.PRIMARY, true);
        mouse(canvas, MouseEvent.MOUSE_RELEASED, x, y, MouseButton.PRIMARY, false);
    }

    private static void mouse(DrawingCanvas canvas, EventType<MouseEvent> type,
                              double x, double y, MouseButton button, boolean primaryDown) {
        Point2D screen = canvas.localToScreen(x, y);
        MouseEvent event = new MouseEvent(type, x, y, screen.getX(), screen.getY(), button, 1,
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
        return task.get(15, TimeUnit.SECONDS);
    }

    private record Editor(DrawingModel model, EditorPane pane, DrawingCanvas canvas) {
        private <T extends Node> T node(String id, Class<T> type) {
            Node node = pane.lookup("#" + id);
            assertNotNull(node, "Элемент должен быть доступен по ID " + id);
            return type.cast(node);
        }

        private ToggleButton toggle(String id) {
            return node(id, ToggleButton.class);
        }
    }
}
