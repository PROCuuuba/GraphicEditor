package org.example.graphiceditor.ui;

import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;
import org.example.graphiceditor.model.vector.Shape;
import org.example.graphiceditor.model.vector.Vertex;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("gui")
class PolygonFinalAcceptanceTest extends VectorGuiTestSupport {
    @Test
    void sixMouseClicksCreateAConcavePolygonWhoseCutoutRemainsUnfilled() throws Exception {
        withEditor(editor -> {
            enable(editor, "fill-enabled", true);
            setColor(editor, "fill-color", Color.DARKORANGE);
            List<Vertex> vertices = List.of(new Vertex(100, 100), new Vertex(300, 100),
                    new Vertex(300, 180), new Vertex(180, 180),
                    new Vertex(180, 300), new Vertex(100, 300));
            for (Vertex vertex : vertices) click(editor, vertex.x(), vertex.y());
            assertTrue(editor.model().getShapes().isEmpty());

            finish(editor);

            assertEquals(1, editor.model().getShapes().size());
            Shape polygon = editor.model().getShapes().getFirst();
            assertEquals(vertices, polygon.getVertices());
            assertTrue(editor.model().getPixels().isEmpty());
            PixelReader image = image(editor);
            assertColor(image, 250, 140, Color.DARKORANGE);
            assertColor(image, 140, 250, Color.DARKORANGE);
            assertColor(image, 240, 240, Color.WHITE);
            assertColor(image, 340, 140, Color.WHITE);
            assertColor(image, 140, 100, Color.BLACK);
        });
    }
}
