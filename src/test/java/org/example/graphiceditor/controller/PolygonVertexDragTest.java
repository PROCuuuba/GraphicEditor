package org.example.graphiceditor.controller;

import javafx.scene.paint.Color;
import org.example.graphiceditor.model.DrawingModel;
import org.example.graphiceditor.model.vector.Polygon;
import org.example.graphiceditor.model.vector.Shape;
import org.example.graphiceditor.model.vector.VectorStyle;
import org.example.graphiceditor.model.vector.Vertex;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PolygonVertexDragTest {
    @Test
    void movesOnlyTheChosenVertexAndPreservesAppliedStyleAndOriginalGeometry() {
        DrawingModel model = new DrawingModel(200, 160);
        VectorStyle applied = new VectorStyle(true, Color.RED, true, Color.BLUE, 4,
                true, Color.GRAY, 6, 7, 8);
        Shape original = applied.decorate(triangle(20, 20));
        Shape other = triangle(100, 80);
        model.addShape(original);
        model.addShape(other);
        PolygonController controller = new PolygonController(model);

        assertTrue(controller.startVertexDrag(21, 21, 10));
        assertEquals(applied, controller.getStyle());
        controller.setStyle(VectorStyle.defaults());
        assertTrue(controller.dragVertexTo(30.25, 35.5));

        Shape moved = model.getShape(0);
        assertEquals(List.of(new Vertex(30.25, 35.5), new Vertex(60, 20), new Vertex(20, 60)), moved.getVertices());
        assertEquals(applied, VectorStyle.from(moved));
        assertEquals(List.of(new Vertex(20, 20), new Vertex(60, 20), new Vertex(20, 60)), original.getVertices());
        assertSame(other, model.getShape(1));
        assertSame(moved, controller.getSelectedShape());
        assertEquals(0, controller.getSelectedIndex());
        assertTrue(controller.isDraggingVertex());
        assertTrue(controller.dragVertexTo(40, 50));
        assertEquals(new Vertex(40, 50), model.getShape(0).getVertices().getFirst());
        assertEquals(applied, VectorStyle.from(model.getShape(0)));
    }

    @Test
    void endingTheGestureKeepsTheMoveAndDisablesFurtherChanges() {
        DrawingModel model = new DrawingModel(200, 160);
        model.addShape(triangle(20, 20));
        PolygonController controller = new PolygonController(model);
        assertTrue(controller.startVertexDrag(20, 20, 0));
        assertTrue(controller.dragVertexTo(30, 30));
        Shape moved = model.getShape(0);

        controller.endVertexDrag();

        assertFalse(controller.isDraggingVertex());
        assertFalse(controller.dragVertexTo(50, 50));
        assertSame(moved, model.getShape(0));
        controller.cancelVertexDrag();
        assertSame(moved, model.getShape(0));
    }

    @Test
    void cancelRestoresTheObjectFromBeforeTheGesture() {
        DrawingModel model = new DrawingModel(200, 160);
        Shape original = VectorStyle.defaults().decorate(triangle(20, 20));
        model.addShape(original);
        PolygonController controller = new PolygonController(model);
        assertTrue(controller.startVertexDrag(60, 20, 10));
        assertTrue(controller.dragVertexTo(70, 30));
        assertTrue(controller.dragVertexTo(80, 40));

        controller.cancelVertexDrag();

        assertFalse(controller.isDraggingVertex());
        assertSame(original, model.getShape(0));
        assertSame(original, controller.getSelectedShape());
        assertEquals(new Vertex(60, 20), original.getVertices().get(1));
    }

    @Test
    void selectedShapeHasPriorityOverAnOverlappingCloserVertex() {
        DrawingModel model = new DrawingModel(200, 160);
        Shape selected = triangle(20, 20);
        Shape top = triangle(22, 20);
        model.addShape(selected);
        model.addShape(top);
        PolygonController controller = new PolygonController(model);
        controller.selectShape(0);

        assertTrue(controller.startVertexDrag(22, 20, 5));
        assertTrue(controller.dragVertexTo(30, 30));

        assertEquals(0, controller.getSelectedIndex());
        assertEquals(new Vertex(30, 30), model.getShape(0).getVertices().getFirst());
        assertSame(top, model.getShape(1));
    }

    @Test
    void withoutSelectionChoosesNearestVertexAndTopmostOnTies() {
        DrawingModel model = new DrawingModel(200, 160);
        Shape lower = triangle(20, 20);
        Shape top = triangle(24, 20);
        model.addShape(lower);
        model.addShape(top);
        PolygonController controller = new PolygonController(model);

        assertTrue(controller.startVertexDrag(21, 20, 5));
        assertEquals(0, controller.getSelectedIndex());
        controller.endVertexDrag();
        controller.selectShape(-1);
        assertTrue(controller.startVertexDrag(22, 20, 5));
        assertEquals(1, controller.getSelectedIndex());
        assertSame(top, controller.getSelectedShape());
    }

    @Test
    void changingSelectionEndsTheGestureWithoutChangingTheNewShape() {
        DrawingModel model = new DrawingModel(200, 160);
        model.addShape(triangle(20, 20));
        Shape other = triangle(100, 80);
        model.addShape(other);
        PolygonController controller = new PolygonController(model);
        assertTrue(controller.startVertexDrag(20, 20, 10));
        assertTrue(controller.dragVertexTo(30, 30));
        Shape moved = model.getShape(0);

        controller.selectShape(1);
        controller.cancelVertexDrag();

        assertFalse(controller.isDraggingVertex());
        assertFalse(controller.dragVertexTo(110, 90));
        assertSame(moved, model.getShape(0));
        assertSame(other, model.getShape(1));
    }

    @Test
    void outsideAndNonFiniteCoordinatesLeaveTheLastValidMoveIntact() {
        DrawingModel model = new DrawingModel(200, 160);
        model.addShape(triangle(20, 20));
        PolygonController controller = new PolygonController(model);
        assertTrue(controller.startVertexDrag(20, 20, 10));
        assertTrue(controller.dragVertexTo(0, 0));
        Shape valid = model.getShape(0);

        for (double[] coordinate : new double[][]{
                {-1, 0}, {0, -1}, {200, 10}, {10, 160}, {Double.NaN, 10},
                {10, Double.POSITIVE_INFINITY}, {Double.NEGATIVE_INFINITY, 10}
        }) {
            assertFalse(controller.dragVertexTo(coordinate[0], coordinate[1]));
            assertSame(valid, model.getShape(0));
        }
        assertFalse(controller.dragVertexTo(0, 0));
        assertTrue(controller.isDraggingVertex());
    }

    @Test
    void doesNotStartOutsideTheCanvasOrWhenNoVertexIsInRange() {
        DrawingModel model = new DrawingModel(200, 160);
        Shape original = triangle(0, 0);
        model.addShape(original);
        PolygonController controller = new PolygonController(model);

        assertFalse(controller.startVertexDrag(-1, 0, 10));
        assertFalse(controller.startVertexDrag(Double.NaN, 0, 10));
        assertFalse(controller.startVertexDrag(100, 100, 10));
        assertFalse(controller.isDraggingVertex());
        assertFalse(controller.dragVertexTo(20, 20));
        assertSame(original, model.getShape(0));
        assertTrue(controller.startVertexDrag(3, 4, 5));
    }

    @Test
    void refusesDraggingWhileThereIsAnUnfinishedPolygon() {
        DrawingModel model = new DrawingModel(200, 160);
        model.addShape(triangle(20, 20));
        PolygonController controller = new PolygonController(model);
        controller.addVertex(100, 100);

        assertFalse(controller.startVertexDrag(20, 20, 10));
        assertFalse(controller.isDraggingVertex());
        assertEquals(List.of(new Vertex(100, 100)), controller.getDraftVertices());
    }

    @Test
    void rejectsInvalidSelectionTolerance() {
        PolygonController controller = new PolygonController(new DrawingModel(200, 160));

        for (double tolerance : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> controller.startVertexDrag(20, 20, tolerance));
        }
        assertFalse(controller.isDraggingVertex());
    }

    @Test
    void clearingOrDeletingDuringAGestureClearsDragState() {
        DrawingModel model = new DrawingModel(200, 160);
        model.addShape(triangle(20, 20));
        PolygonController controller = new PolygonController(model);
        assertTrue(controller.startVertexDrag(20, 20, 10));

        assertTrue(controller.deleteSelected());
        assertFalse(controller.isDraggingVertex());
        assertTrue(model.getShapes().isEmpty());
        model.addShape(triangle(20, 20));
        assertTrue(controller.startVertexDrag(20, 20, 10));
        controller.clear();
        assertFalse(controller.isDraggingVertex());
        assertTrue(model.getShapes().isEmpty());
        controller.cancelVertexDrag();
        assertTrue(model.getShapes().isEmpty());
    }

    @Test
    void externalDeletionEndsTheGestureWithoutChangingAnotherObject() {
        DrawingModel model = new DrawingModel(200, 160);
        model.addShape(triangle(20, 20));
        Shape other = triangle(100, 80);
        model.addShape(other);
        PolygonController controller = new PolygonController(model);
        assertTrue(controller.startVertexDrag(20, 20, 10));

        model.removeShape(0);

        assertFalse(controller.dragVertexTo(30, 30));
        assertFalse(controller.isDraggingVertex());
        controller.cancelVertexDrag();
        assertEquals(List.of(other), model.getShapes());
    }

    @Test
    void selectionAndCancellationFollowTheShapeWhenAnEarlierObjectIsRemoved() {
        DrawingModel model = new DrawingModel(200, 160);
        model.addShape(triangle(100, 80));
        Shape original = triangle(20, 20);
        model.addShape(original);
        PolygonController controller = new PolygonController(model);
        assertTrue(controller.startVertexDrag(20, 20, 10));
        assertTrue(controller.dragVertexTo(30, 30));

        model.removeShape(0);
        assertTrue(controller.dragVertexTo(40, 40));
        controller.cancelVertexDrag();

        assertEquals(0, controller.getSelectedIndex());
        assertEquals(List.of(original), model.getShapes());
        assertSame(original, controller.getSelectedShape());
    }

    private static Polygon triangle(double x, double y) {
        return new Polygon(List.of(new Vertex(x, y), new Vertex(x + 40, y), new Vertex(x, y + 40)));
    }
}
