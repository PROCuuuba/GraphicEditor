package org.example.graphiceditor.controller;

import javafx.scene.paint.Color;
import org.example.graphiceditor.model.Circle;
import org.example.graphiceditor.model.DrawingModel;
import org.example.graphiceditor.model.Point;
import org.example.graphiceditor.model.Square;
import org.example.graphiceditor.model.vector.BorderDecorator;
import org.example.graphiceditor.model.vector.FillDecorator;
import org.example.graphiceditor.model.vector.Polygon;
import org.example.graphiceditor.model.vector.ShadowDecorator;
import org.example.graphiceditor.model.vector.Shape;
import org.example.graphiceditor.model.vector.Vertex;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DrawingSelectionTest {
    @Test
    void picksTopmostVectorBeforeRasterAndRejectsConcaveCutout() {
        DrawingModel drawing = new DrawingModel(900, 650);
        drawing.add(new Circle(120, 120, 100, Color.RED));
        Shape concave = new Polygon(List.of(new Vertex(100, 100), new Vertex(300, 100),
                new Vertex(300, 180), new Vertex(180, 180), new Vertex(180, 300), new Vertex(100, 300)));
        drawing.addShape(concave);
        DrawingSelection selection = new DrawingSelection(drawing);
        assertTrue(selection.selectAt(120, 120, 0));
        assertEquals(0, selection.getSelectedShapeIndex());
        assertEquals(-1, selection.getSelectedPixelIndex());
        assertFalse(selection.selectAt(240, 240, 0));
        assertNull(selection.getSelectedObject());
        assertTrue(selection.selectAt(120, 98, 3));
        assertSame(concave, selection.getSelectedObject());
    }

    @Test
    void rasterCanBeSelectedMovedDeletedAndStaleSelectionIsCleared() {
        DrawingModel drawing = new DrawingModel(900, 650);
        drawing.add(new Point(10.75, 20.75, 1, Color.RED));
        drawing.add(new Square(100, 100, 20, Color.BLUE));
        DrawingSelection selection = new DrawingSelection(drawing);
        assertTrue(selection.selectAt(10.1, 20.1, 0));
        assertInstanceOf(Point.class, selection.getSelectedObject());
        assertTrue(selection.moveSelected(10, 10));
        assertEquals(20.75, drawing.getPixels().getFirst().getX());
        assertTrue(selection.removeSelected());
        assertEquals(1, drawing.size());
        assertNull(selection.getSelectedObject());
        selection.selectPixel(0);
        drawing.clear();
        assertNull(selection.getSelectedObject());
        assertFalse(selection.moveSelected(1, 1));
    }

    @Test
    void polygonTranslationPreservesActualDecoratorOrderAndClampsAllVertices() {
        DrawingModel drawing = new DrawingModel(900, 650);
        Polygon polygon = new Polygon(List.of(new Vertex(10, 10), new Vertex(200, 10), new Vertex(10, 200)));
        Shape styled = new FillDecorator(new ShadowDecorator(new BorderDecorator(polygon, Color.RED, 3),
                Color.GRAY, -10, 5, 6), Color.BLUE);
        drawing.addShape(styled);
        DrawingSelection selection = new DrawingSelection(drawing);
        selection.selectShape(0);
        assertTrue(selection.moveSelected(-1000, -1000));
        Shape moved = drawing.getShape(0);
        FillDecorator fill = assertInstanceOf(FillDecorator.class, moved);
        ShadowDecorator shadow = assertInstanceOf(ShadowDecorator.class, fill.getWrapped());
        BorderDecorator border = assertInstanceOf(BorderDecorator.class, shadow.getWrapped());
        assertEquals(Color.BLUE, fill.getColor());
        assertEquals(-10, shadow.getOffsetX());
        assertEquals(3, border.getWidth());
        assertEquals(List.of(new Vertex(0, 0), new Vertex(190, 0), new Vertex(0, 190)), moved.getVertices());
        assertFalse(selection.moveSelected(-1, -1));
        selection.moveSelected(2000, 2000);
        assertTrue(drawing.getShape(0).getVertices().stream().allMatch(v -> drawing.contains(v.x(), v.y())));
        assertEquals(190, drawing.getShape(0).getVertices().get(1).x() - drawing.getShape(0).getVertices().getFirst().x(), 1e-9);
    }
}
