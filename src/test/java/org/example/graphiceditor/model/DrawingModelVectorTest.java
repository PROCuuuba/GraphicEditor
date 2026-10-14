package org.example.graphiceditor.model;

import javafx.scene.paint.Color;
import org.example.graphiceditor.model.vector.Polygon;
import org.example.graphiceditor.model.vector.Shape;
import org.example.graphiceditor.model.vector.VectorStyle;
import org.example.graphiceditor.model.vector.Vertex;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DrawingModelVectorTest {
    @Test
    void vectorShapesPreserveOrderWithoutChangingRasterCount() {
        DrawingModel model = new DrawingModel(100, 80);
        Shape first = polygon(10, 10);
        Shape second = polygon(30, 30);
        model.add(new Point(5, 5, 1, Color.RED));

        assertTrue(model.addShape(first));
        assertTrue(model.addShape(second));

        assertEquals(1, model.size());
        assertEquals(List.of(first, second), model.getShapes());
        assertSame(first, model.getShape(0));
        assertSame(second, model.getShape(1));
    }

    @Test
    void vectorShapeViewIsReadOnlyAndReflectsModelChanges() {
        DrawingModel model = new DrawingModel(100, 80);
        List<Shape> shapes = model.getShapes();
        Shape first = polygon(10, 10);
        model.addShape(first);

        assertEquals(List.of(first), shapes);
        assertAll(
                () -> assertThrows(UnsupportedOperationException.class, shapes::clear),
                () -> assertThrows(UnsupportedOperationException.class, () -> shapes.remove(0)),
                () -> assertThrows(UnsupportedOperationException.class, () -> shapes.add(first))
        );
    }

    @Test
    void vectorVerticesOnOrOutsideMaximumBoundaryAreRejected() {
        DrawingModel model = new DrawingModel(100, 80);

        for (Vertex vertex : List.of(new Vertex(100, 10), new Vertex(10, 80), new Vertex(120, 90))) {
            Shape shape = new Polygon(List.of(new Vertex(0, 0), new Vertex(20, 20), vertex));
            assertFalse(model.addShape(shape));
        }

        assertTrue(model.getShapes().isEmpty());
        assertTrue(model.addShape(new Polygon(List.of(
                new Vertex(0, 0), new Vertex(Math.nextDown(100.0), 0),
                new Vertex(0, Math.nextDown(80.0))))));
    }

    @Test
    void replacementPreservesPositionAndRejectsOutOfBoundsGeometry() {
        DrawingModel model = new DrawingModel(100, 80);
        Shape first = polygon(10, 10);
        Shape second = polygon(30, 30);
        Shape replacement = VectorStyle.defaults().decorate(polygon(20, 20));
        model.addShape(first);
        model.addShape(second);

        model.replaceShape(0, replacement);

        assertEquals(List.of(replacement, second), model.getShapes());
        Shape outside = new Polygon(List.of(new Vertex(0, 0), new Vertex(10, 10), new Vertex(100, 20)));
        assertThrows(IllegalArgumentException.class, () -> model.replaceShape(0, outside));
        assertSame(replacement, model.getShape(0));
    }

    @Test
    void removalOnlyRemovesTheRequestedVectorObject() {
        DrawingModel model = new DrawingModel(100, 80);
        Shape first = polygon(10, 10);
        Shape second = polygon(30, 30);
        model.addShape(first);
        model.addShape(second);
        model.add(new Point(5, 5, 1, Color.RED));

        model.removeShape(0);

        assertEquals(List.of(second), model.getShapes());
        assertEquals(1, model.size());
        assertThrows(IndexOutOfBoundsException.class, () -> model.removeShape(1));
    }

    @Test
    void clearRemovesRasterAndVectorObjectsAndAllowsNewShapes() {
        DrawingModel model = new DrawingModel(100, 80);
        model.add(new Point(5, 5, 1, Color.RED));
        model.addShape(polygon(10, 10));

        model.clear();

        assertTrue(model.getPixels().isEmpty());
        assertTrue(model.getShapes().isEmpty());
        assertEquals(0, model.size());
        assertTrue(model.addShape(polygon(20, 20)));
    }

    private static Shape polygon(double x, double y) {
        return new Polygon(List.of(new Vertex(x, y), new Vertex(x + 10, y), new Vertex(x, y + 10)));
    }
}
