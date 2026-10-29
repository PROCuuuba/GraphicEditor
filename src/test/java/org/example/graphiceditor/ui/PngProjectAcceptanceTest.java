package org.example.graphiceditor.ui;

import javafx.event.Event;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.paint.Color;
import org.example.graphiceditor.io.PngProjectIO;
import org.example.graphiceditor.model.Point;
import org.example.graphiceditor.model.vector.VectorStyle;
import org.example.graphiceditor.model.vector.Vertex;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@Tag("gui")
class PngProjectAcceptanceTest extends VectorGuiTestSupport {
    @TempDir
    Path directory;

    @Test
    void exportedPngRestoresEditableFiguresAndBrushesWithoutZoomOrSelectionOverlay() throws Exception {
        withEditor(editor -> {
            try {
                editor.model().add(new Point(20.25, 30.75, 1, Color.BLUE));
                enable(editor, "fill-enabled", true);
                setColor(editor, "fill-color", Color.RED);
                finishSquare(editor, 100, 100, 100);
                editor.node("canvas-viewport", CanvasViewport.class).setZoom(3);
                Path first = directory.resolve("first.png");
                editor.pane().saveProject(first);

                var preview = ImageIO.read(first.toFile());
                assertEquals(900, preview.getWidth());
                assertEquals(650, preview.getHeight());
                assertEquals(0xffff0000, preview.getRGB(150, 150));
                assertEquals(0xff000000, preview.getRGB(100, 100));
                assertEquals(0xff0000ff, preview.getRGB(20, 30));

                editor.node("clear-canvas", Button.class).fire();
                assertTrue(editor.model().getShapes().isEmpty());
                editor.pane().replaceProject(editor.pane().readProject(first));
                assertEquals(1, editor.model().getShapes().size());
                assertEquals(20.25, editor.model().getPixels().getFirst().getX());
                setColor(editor, "fill-color", Color.LIME);
                mouse(editor.canvas(), MouseEvent.MOUSE_PRESSED, 100, 100, true);
                mouse(editor.canvas(), MouseEvent.MOUSE_DRAGGED, 120, 130, true);
                mouse(editor.canvas(), MouseEvent.MOUSE_RELEASED, 120, 130, false);
                assertEquals(new Vertex(120, 130), editor.model().getShape(0).getVertices().getFirst());

                Path second = directory.resolve("edited.png");
                editor.pane().saveProject(second);
                var restored = PngProjectIO.read(second);
                assertEquals(Color.LIME, VectorStyle.from(restored.getShape(0)).fillColor());
                assertEquals(new Vertex(120, 130), restored.getShape(0).getVertices().getFirst());
                assertEquals(0xff00ff00, ImageIO.read(second.toFile()).getRGB(150, 150));
            } catch (IOException exception) {
                throw new AssertionError(exception);
            }
        });
    }

    @Test
    void invalidImportLeavesExistingDrawingAndDraftCannotBeSilentlyLostOnExport() throws Exception {
        withEditor(editor -> {
            finishSquare(editor, 100, 100, 100);
            var original = editor.model().getShape(0);
            assertThrows(IOException.class, () -> editor.pane().readProject(directory.resolve("absent.png")));
            assertSame(original, editor.model().getShape(0));
            click(editor, 400, 300);
            assertThrows(IOException.class, () -> editor.pane().saveProject(directory.resolve("draft.png")));
            assertSame(original, editor.model().getShape(0));
        });
    }

    private static void mouse(DrawingCanvas canvas, javafx.event.EventType<MouseEvent> type,
                              double x, double y, boolean down) {
        Point2D screen = canvas.localToScreen(x, y);
        Event.fireEvent(canvas, new MouseEvent(type, x, y, screen.getX(), screen.getY(),
                MouseButton.PRIMARY, 1, false, false, false, false, down, false, false,
                true, false, type == MouseEvent.MOUSE_PRESSED,
                new PickResult(canvas, new Point3D(x, y, 0), 0)));
    }
}
