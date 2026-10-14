package org.example.graphiceditor.controller;

import javafx.scene.paint.Color;
import org.example.graphiceditor.model.DrawingModel;
import org.example.graphiceditor.model.vector.Polygon;
import org.example.graphiceditor.model.vector.Shape;
import org.example.graphiceditor.model.vector.VectorStyle;
import org.example.graphiceditor.model.vector.Vertex;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.example.graphiceditor.controller.PolygonController.VertexAction.ADDED;
import static org.example.graphiceditor.controller.PolygonController.VertexAction.CLOSED;
import static org.example.graphiceditor.controller.PolygonController.VertexAction.IGNORED;
import static org.junit.jupiter.api.Assertions.*;

class PolygonSnappingTest {
    @Test
    void findsTheNearestVertexAcrossDraftAndCompletedShapes() {
        DrawingModel model = new DrawingModel(200, 160);
        Vertex completedVertex = new Vertex(24.25, 30.5);
        model.addShape(new Polygon(List.of(completedVertex, new Vertex(50, 30), new Vertex(50, 60))));
        PolygonController controller = new PolygonController(model);
        controller.addVertex(20, 30);

        assertSame(completedVertex, controller.findSnapVertex(24, 30, 10));
        assertEquals(new Vertex(20, 30), controller.findSnapVertex(21, 30, 10));
        assertNull(controller.findSnapVertex(100, 100, 10));
    }

    @Test
    void includesTheRadiusBoundaryButNotPointsOutsideIt() {
        PolygonController controller = controller();
        controller.addVertex(20, 20);

        assertEquals(new Vertex(20, 20), controller.findSnapVertex(23, 24, 5));
        assertNull(controller.findSnapVertex(23, 24, Math.nextDown(5)));
        assertEquals(new Vertex(20, 20), controller.findSnapVertex(20, 20, 0));
        assertNull(controller.findSnapVertex(20.01, 20, 0));
    }

    @Test
    void prefersTheFirstDraftVertexInEqualDistanceTies() {
        DrawingModel model = new DrawingModel(200, 160);
        model.addShape(new Polygon(List.of(new Vertex(30, 20), new Vertex(80, 20), new Vertex(80, 60))));
        PolygonController controller = new PolygonController(model);
        controller.addVertex(20, 20);
        controller.addVertex(30, 20);
        controller.addVertex(20, 60);

        assertEquals(new Vertex(20, 20), controller.findSnapVertex(25, 20, 5));
        assertEquals(CLOSED, controller.placeVertex(25, 20, 5));
        assertEquals(2, model.getShapes().size());
    }

    @Test
    void prefersOtherDraftVerticesBeforeCompletedVerticesOnTies() {
        DrawingModel model = new DrawingModel(200, 160);
        model.addShape(new Polygon(List.of(new Vertex(40, 20), new Vertex(80, 20), new Vertex(80, 60))));
        PolygonController controller = new PolygonController(model);
        controller.addVertex(10, 10);
        controller.addVertex(30, 20);

        assertEquals(new Vertex(30, 20), controller.findSnapVertex(35, 20, 5));
    }

    @Test
    void ignoresOutsideAndNonFiniteCoordinatesEvenNearAnEdgeVertex() {
        PolygonController controller = controller();
        controller.addVertex(0, 0);
        List<Vertex> draft = controller.getDraftVertices();

        for (double[] coordinate : new double[][]{
                {-1, 0}, {0, -1}, {200, 10}, {10, 160}, {Double.NaN, 10},
                {10, Double.POSITIVE_INFINITY}, {Double.NEGATIVE_INFINITY, 10}
        }) {
            assertNull(controller.findSnapVertex(coordinate[0], coordinate[1], 10));
            assertEquals(IGNORED, controller.placeVertex(coordinate[0], coordinate[1], 10));
        }
        assertEquals(draft, controller.getDraftVertices());
    }

    @Test
    void rejectsInvalidTolerance() {
        PolygonController controller = controller();

        for (double tolerance : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> controller.findSnapVertex(10, 10, tolerance));
            assertThrows(IllegalArgumentException.class, () -> controller.placeVertex(10, 10, tolerance));
        }
        assertTrue(controller.getDraftVertices().isEmpty());
    }

    @Test
    void preventsClosingWithOnlyOneOrTwoVertices() {
        PolygonController controller = controller();

        assertEquals(ADDED, controller.placeVertex(20, 20, 10));
        assertEquals(IGNORED, controller.placeVertex(22, 22, 10));
        assertEquals(ADDED, controller.placeVertex(60, 20, 10));
        assertEquals(IGNORED, controller.placeVertex(20, 20, 10));
        assertEquals(List.of(new Vertex(20, 20), new Vertex(60, 20)), controller.getDraftVertices());
        assertEquals(ADDED, controller.placeVertex(20, 60, 10));
        assertEquals(CLOSED, controller.placeVertex(22, 22, 10));
    }

    @Test
    void closingCreatesOnePolygonWithoutDuplicatingTheFirstVertexAndSelectsIt() {
        DrawingModel model = new DrawingModel(200, 160);
        PolygonController controller = new PolygonController(model);
        controller.placeVertex(20, 20, 10);
        controller.placeVertex(60, 20, 10);
        controller.placeVertex(20, 60, 10);
        List<Vertex> vertices = controller.getDraftVertices();

        assertEquals(CLOSED, controller.placeVertex(25, 20, 10));

        assertEquals(1, model.getShapes().size());
        assertEquals(vertices, model.getShape(0).getVertices());
        assertEquals(3, model.getShape(0).getVertices().stream().distinct().count());
        assertTrue(controller.getDraftVertices().isEmpty());
        assertSame(model.getShape(0), controller.getSelectedShape());
        assertEquals(0, controller.getSelectedIndex());
    }

    @Test
    void closingUsesTheCurrentStyleAndKeepsTheGeometry() {
        DrawingModel model = new DrawingModel(200, 160);
        PolygonController controller = new PolygonController(model);
        controller.placeVertex(20, 20, 10);
        controller.placeVertex(60, 20, 10);
        controller.placeVertex(20, 60, 10);
        List<Vertex> vertices = controller.getDraftVertices();
        VectorStyle style = new VectorStyle(true, Color.RED, true, Color.BLUE, 4,
                true, Color.GRAY, 6, 7, 8);
        controller.setStyle(style);

        assertEquals(CLOSED, controller.placeVertex(20, 20, 10));

        Shape shape = model.getShape(0);
        assertEquals(style, VectorStyle.from(shape));
        assertEquals(vertices, VectorStyle.geometryOf(shape).getVertices());
    }

    @Test
    void canShareExactCoordinatesWithAnExistingPolygonWithoutClosingTheDraft() {
        DrawingModel model = new DrawingModel(200, 160);
        Vertex shared = new Vertex(40.25, 40.5);
        Shape original = new Polygon(List.of(shared, new Vertex(80, 40), new Vertex(80, 80)));
        model.addShape(original);
        PolygonController controller = new PolygonController(model);
        controller.placeVertex(10, 10, 10);
        controller.placeVertex(10, 80, 10);
        controller.placeVertex(40, 40, 10);

        assertEquals(List.of(new Vertex(10, 10), new Vertex(10, 80), shared), controller.getDraftVertices());
        assertEquals(1, model.getShapes().size());
        assertSame(original, model.getShape(0));
        assertEquals(CLOSED, controller.placeVertex(10, 10, 10));
        assertEquals(shared, model.getShape(1).getVertices().getLast());
        assertSame(original, model.getShape(0));
    }

    @Test
    void allowsStartingAnotherPolygonAtAnExistingVertex() {
        DrawingModel model = new DrawingModel(200, 160);
        Vertex shared = new Vertex(40.25, 40.5);
        model.addShape(new Polygon(List.of(shared, new Vertex(80, 40), new Vertex(80, 80))));
        PolygonController controller = new PolygonController(model);

        assertEquals(ADDED, controller.placeVertex(40, 40, 10));
        assertEquals(List.of(shared), controller.getDraftVertices());
        assertEquals(IGNORED, controller.placeVertex(40, 40, 10));
        assertEquals(1, model.getShapes().size());
    }

    @Test
    void ignoresConsecutiveSnappedDuplicatesButAllowsNonFirstVertexReuse() {
        PolygonController controller = controller();
        controller.placeVertex(20, 20, 10);
        controller.placeVertex(60, 20, 10);

        assertEquals(IGNORED, controller.placeVertex(61, 21, 10));
        assertEquals(ADDED, controller.placeVertex(20, 60, 10));
        assertEquals(ADDED, controller.placeVertex(61, 21, 10));
        assertEquals(List.of(new Vertex(20, 20), new Vertex(60, 20),
                new Vertex(20, 60), new Vertex(60, 20)), controller.getDraftVertices());
    }

    @Test
    void deletedShapesAreNoLongerSnapTargets() {
        DrawingModel model = new DrawingModel(200, 160);
        model.addShape(new Polygon(List.of(new Vertex(20, 20), new Vertex(60, 20), new Vertex(20, 60))));
        PolygonController controller = new PolygonController(model);
        assertNotNull(controller.findSnapVertex(22, 22, 10));

        model.removeShape(0);

        assertNull(controller.findSnapVertex(22, 22, 10));
        assertEquals(ADDED, controller.placeVertex(22, 22, 10));
        assertEquals(List.of(new Vertex(22, 22)), controller.getDraftVertices());
    }

    private static PolygonController controller() {
        return new PolygonController(new DrawingModel(200, 160));
    }
}
