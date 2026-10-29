package org.example.graphiceditor.model;

import javafx.scene.paint.Color;
import org.example.graphiceditor.model.vector.Polygon;
import org.example.graphiceditor.model.vector.Vertex;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DrawingTest {
    @Test
    void aggregateContainsBothObjectKindsAndRestoresImmutableSnapshot() {
        Drawing drawing = new Drawing(900, 650);
        Point point = new Point(10, 20, 1, Color.RED);
        Polygon polygon = new Polygon(List.of(new Vertex(100, 100), new Vertex(200, 100), new Vertex(100, 200)));
        drawing.add(point);
        drawing.addShape(polygon);
        drawing.setBackgroundColor(Color.BEIGE);
        var before = drawing.snapshot();
        assertEquals(List.of(point, polygon), drawing.getObjects());
        assertThrows(UnsupportedOperationException.class, () -> before.pixels().clear());
        drawing.removePixel(0);
        drawing.removeShape(0);
        drawing.setBackgroundColor(Color.BLACK);
        drawing.restore(before);
        assertEquals(List.of(point, polygon), drawing.getObjects());
        assertEquals(Color.BEIGE, drawing.getBackgroundColor());
    }

    @Test
    void invalidSnapshotAndReplacementCannotPartlyChangeDrawing() {
        Drawing drawing = new Drawing(50, 50);
        Point point = new Point(10, 10, 1, Color.RED);
        drawing.add(point);
        var outside = new Drawing.Snapshot(List.of(new Point(100, 100, 1, Color.BLACK)), List.of(), Color.BLUE);
        assertThrows(IllegalArgumentException.class, () -> drawing.restore(outside));
        assertEquals(List.of(point), drawing.getPixels());
        assertEquals(Color.WHITE, drawing.getBackgroundColor());
        assertThrows(IllegalArgumentException.class, () -> drawing.replacePixel(0, new Point(50, 0, 1, Color.BLACK)));
        assertSame(point, drawing.getPixels().getFirst());
    }
}
