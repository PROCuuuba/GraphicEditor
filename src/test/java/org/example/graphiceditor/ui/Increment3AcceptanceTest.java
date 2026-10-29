package org.example.graphiceditor.ui;

import javafx.event.Event;
import javafx.event.EventType;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.paint.Color;
import org.example.graphiceditor.io.ProjectFiles;
import org.example.graphiceditor.model.DrawingModel;
import org.example.graphiceditor.model.vector.VectorStyle;
import org.example.graphiceditor.model.vector.Vertex;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("gui")
class Increment3AcceptanceTest extends VectorGuiTestSupport {
    @TempDir
    Path directory;

    @Test
    void wholeStrokeIsOneUndoAndNewDrawingActionClearsRedo() throws Exception {
        withEditor(editor -> {
            editor.node("tool-circle", ToggleButton.class).fire();
            mouse(editor.canvas(), MouseEvent.MOUSE_PRESSED, 40, 50, true);
            mouse(editor.canvas(), MouseEvent.MOUSE_DRAGGED, 100, 50, true);
            mouse(editor.canvas(), MouseEvent.MOUSE_DRAGGED, 160, 100, true);
            mouse(editor.canvas(), MouseEvent.MOUSE_RELEASED, 160, 100, false);
            int strokeSize = editor.model().size();
            assertTrue(strokeSize > 2);
            undo(editor);
            assertEquals(0, editor.model().size());
            redo(editor);
            assertEquals(strokeSize, editor.model().size());
            click(editor, 300, 300);
            assertEquals(strokeSize + 1, editor.model().size());
            undo(editor);
            assertEquals(strokeSize, editor.model().size());
            click(editor, 400, 400);
            assertTrue(editor.node("history-redo", Button.class).isDisabled());
            assertEquals(400, editor.model().getPixels().getLast().getX());
        });
    }

    @Test
    void polygonInteriorAndBrushAreSelectableAndDeletionIsUndoable() throws Exception {
        withEditor(editor -> {
            enable(editor, "fill-enabled", true);
            setColor(editor, "fill-color", Color.CORAL);
            finishSquare(editor, 100, 100, 100);
            editor.node("tool-square", ToggleButton.class).fire();
            click(editor, 400, 300);
            editor.node("mode-select", ToggleButton.class).fire();
            click(editor, 150, 150);
            assertFalse(editor.node("object-delete", Button.class).isDisabled());
            var selected = editor.canvas().snapshot(null, null).getPixelReader();
            assertNotEquals(Color.BLACK, selected.getColor(100, 100));
            editor.node("object-delete", Button.class).fire();
            assertTrue(editor.model().getShapes().isEmpty());
            assertEquals(1, editor.model().size());
            undo(editor);
            assertEquals(1, editor.model().getShapes().size());
            click(editor, 400, 300);
            editor.node("object-delete", Button.class).fire();
            assertEquals(0, editor.model().size());
            assertEquals(1, editor.model().getShapes().size());
            undo(editor);
            assertEquals(1, editor.model().size());
        });
    }

    @Test
    void objectDragAndStyleChangeAreIndependentUndoableActions() throws Exception {
        withEditor(editor -> {
            enable(editor, "fill-enabled", true);
            setColor(editor, "fill-color", Color.RED);
            finishSquare(editor, 100, 100, 100);
            List<Vertex> original = editor.model().getShape(0).getVertices();
            editor.node("mode-select", ToggleButton.class).fire();
            mouse(editor.canvas(), MouseEvent.MOUSE_PRESSED, 150, 150, true);
            mouse(editor.canvas(), MouseEvent.MOUSE_DRAGGED, 190, 180, true);
            mouse(editor.canvas(), MouseEvent.MOUSE_RELEASED, 190, 180, false);
            assertEquals(new Vertex(140, 130), editor.model().getShape(0).getVertices().getFirst());
            setColor(editor, "fill-color", Color.LIME);
            assertEquals(Color.LIME, VectorStyle.from(editor.model().getShape(0)).fillColor());
            undo(editor);
            assertEquals(Color.RED, VectorStyle.from(editor.model().getShape(0)).fillColor());
            assertEquals(new Vertex(140, 130), editor.model().getShape(0).getVertices().getFirst());
            undo(editor);
            assertEquals(original, editor.model().getShape(0).getVertices());
            redo(editor);
            assertEquals(new Vertex(140, 130), editor.model().getShape(0).getVertices().getFirst());
        });
    }

    @Test
    void backgroundIsDrawnAndUndoableAndNewProjectStartsFreshHistory() throws Exception {
        withEditor(editor -> {
            editor.node("background-color", ColorPicker.class).setValue(Color.BEIGE);
            assertEquals(Color.BEIGE, editor.model().getBackgroundColor());
            assertColor(editor.canvas().snapshot(null, null).getPixelReader(), 450, 450, Color.BEIGE);
            undo(editor);
            assertEquals(Color.WHITE, editor.model().getBackgroundColor());
            redo(editor);
            assertEquals(Color.BEIGE, editor.model().getBackgroundColor());
            finishSquare(editor, 100, 100, 100);
            editor.pane().newProject();
            assertTrue(editor.model().getObjects().isEmpty());
            assertTrue(editor.node("history-undo", Button.class).isDisabled());
            assertTrue(editor.node("history-redo", Button.class).isDisabled());
            assertEquals(Color.BEIGE, editor.model().getBackgroundColor());
        });
    }

    @Test
    void eachSupportedFormatReopensEditableMixedDrawingAndStartsFreshHistory() throws Exception {
        withEditor(editor -> {
            editor.node("tool-circle", ToggleButton.class).fire();
            click(editor, 40, 50);
            editor.node("mode-polygon", ToggleButton.class).fire();
            enable(editor, "fill-enabled", true);
            setColor(editor, "fill-color", Color.CORAL);
            enable(editor, "shadow-enabled", true);
            finishSquare(editor, 100, 100, 100);
            editor.node("background-color", ColorPicker.class).setValue(Color.BEIGE);
            var originalPixels = editor.model().getPixels().stream().map(pixel -> List.of(pixel.getX(), pixel.getY(), pixel.getSize())).toList();
            var originalVertices = editor.model().getShape(0).getVertices();
            for (ProjectFiles.Format format : ProjectFiles.Format.values()) {
                try {
                    Path path = directory.resolve("drawing-" + format.name());
                    editor.pane().saveProject(path, format);
                    DrawingModel loaded = editor.pane().readProject(path);
                    editor.pane().newProject();
                    editor.pane().replaceProject(loaded);
                    assertEquals(originalPixels, editor.model().getPixels().stream().map(pixel -> List.of(pixel.getX(), pixel.getY(), pixel.getSize())).toList());
                    assertEquals(originalVertices, editor.model().getShape(0).getVertices());
                    assertEquals(Color.BEIGE, editor.model().getBackgroundColor());
                    assertTrue(VectorStyle.from(editor.model().getShape(0)).shadowEnabled());
                    assertTrue(editor.node("history-undo", Button.class).isDisabled());
                    assertTrue(editor.node("history-redo", Button.class).isDisabled());
                    setColor(editor, "fill-color", Color.BLUE);
                    assertEquals(Color.BLUE, VectorStyle.from(editor.model().getShape(0)).fillColor());
                    undo(editor);
                    assertEquals(Color.CORAL, VectorStyle.from(editor.model().getShape(0)).fillColor());
                    assertColor(editor.canvas().snapshot(null, null).getPixelReader(), 150, 150, Color.CORAL);
                } catch (IOException exception) {
                    throw new AssertionError(format.name(), exception);
                }
            }
        });
    }

    private static void undo(Editor editor) { editor.node("history-undo", Button.class).fire(); }
    private static void redo(Editor editor) { editor.node("history-redo", Button.class).fire(); }

    private static void mouse(DrawingCanvas canvas, EventType<MouseEvent> type, double x, double y, boolean down) {
        Point2D screen = canvas.localToScreen(x, y);
        assertNotNull(screen);
        Event.fireEvent(canvas, new MouseEvent(type, x, y, screen.getX(), screen.getY(), MouseButton.PRIMARY, 1,
                false, false, false, false, down, false, false, true, false,
                type == MouseEvent.MOUSE_PRESSED, new PickResult(canvas, new Point3D(x, y, 0), 0)));
    }
}
