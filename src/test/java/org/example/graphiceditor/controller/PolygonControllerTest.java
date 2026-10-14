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

class PolygonControllerTest {
    @Test
    void finishingPolygonStoresStyledGeometryAndSelectsIt() {
        DrawingModel model = new DrawingModel(200, 160);
        PolygonController controller = new PolygonController(model);
        VectorStyle style = style(Color.RED, Color.BLUE, true);
        controller.setStyle(style);
        addTriangle(controller, 10, 20);

        Shape shape = controller.finishPolygon();

        assertSame(shape, model.getShape(0));
        assertEquals(List.of(new Vertex(10, 20), new Vertex(30, 20), new Vertex(10, 40)), shape.getVertices());
        assertEquals(style, VectorStyle.from(shape));
        assertEquals(0, controller.getSelectedIndex());
        assertSame(shape, controller.getSelectedShape());
        assertTrue(controller.getDraftVertices().isEmpty());
        assertEquals(0, model.size());
    }

    @Test
    void unfinishedPolygonDoesNotChangeTheModelOrDraft() {
        DrawingModel model = new DrawingModel(200, 160);
        PolygonController controller = new PolygonController(model);
        controller.addVertex(10, 20);
        controller.addVertex(30, 20);
        List<Vertex> draft = controller.getDraftVertices();

        assertThrows(IllegalStateException.class, controller::finishPolygon);

        assertEquals(draft, controller.getDraftVertices());
        assertTrue(model.getShapes().isEmpty());
        assertEquals(-1, controller.getSelectedIndex());
    }

    @Test
    void verticesAreBoundedAndConsecutiveDuplicatesAreIgnored() {
        DrawingModel model = new DrawingModel(200, 160);
        PolygonController controller = new PolygonController(model);

        for (double[] coordinate : new double[][]{
                {-1, 10}, {10, -1}, {200, 10}, {10, 160}, {Double.NaN, 10},
                {10, Double.POSITIVE_INFINITY}, {Double.NEGATIVE_INFINITY, 10}
        }) {
            assertFalse(controller.addVertex(coordinate[0], coordinate[1]));
        }
        assertTrue(controller.addVertex(0, 0));
        assertFalse(controller.addVertex(0, 0));
        assertTrue(controller.addVertex(10, 10));
        assertTrue(controller.addVertex(0, 0));
        assertEquals(List.of(new Vertex(0, 0), new Vertex(10, 10), new Vertex(0, 0)), controller.getDraftVertices());
    }

    @Test
    void draftVerticesAreImmutableSnapshots() {
        PolygonController controller = new PolygonController(new DrawingModel(200, 160));
        controller.addVertex(10, 20);
        List<Vertex> snapshot = controller.getDraftVertices();

        assertThrows(UnsupportedOperationException.class, snapshot::clear);
        controller.addVertex(30, 40);

        assertEquals(List.of(new Vertex(10, 20)), snapshot);
        assertEquals(2, controller.getDraftVertices().size());
    }

    @Test
    void cancelDiscardsOnlyDraftAndKeepsCompletedPolygon() {
        DrawingModel model = new DrawingModel(200, 160);
        PolygonController controller = new PolygonController(model);
        addTriangle(controller, 10, 20);
        Shape complete = controller.finishPolygon();
        controller.addVertex(50, 50);

        controller.cancelPolygon();

        assertTrue(controller.getDraftVertices().isEmpty());
        assertEquals(List.of(complete), model.getShapes());
        assertSame(complete, controller.getSelectedShape());
    }

    @Test
    void selectingExistingPolygonLoadsItsStyle() {
        DrawingModel model = new DrawingModel(200, 160);
        PolygonController controller = new PolygonController(model);
        VectorStyle firstStyle = style(Color.RED, Color.BLUE, true);
        VectorStyle secondStyle = style(Color.GREEN, Color.BLACK, false);
        controller.setStyle(firstStyle);
        addTriangle(controller, 10, 20);
        Shape first = controller.finishPolygon();
        controller.setStyle(secondStyle);
        addTriangle(controller, 60, 20);
        controller.finishPolygon();

        controller.selectShape(0);

        assertSame(first, controller.getSelectedShape());
        assertEquals(firstStyle, controller.getStyle());
        assertEquals(0, controller.getSelectedIndex());
        controller.selectShape(-1);
        assertNull(controller.getSelectedShape());
        assertEquals(firstStyle, controller.getStyle());
    }

    @Test
    void applyingStyleReplacesSelectedDecoratorsWithoutChangingVerticesOrOrder() {
        DrawingModel model = new DrawingModel(200, 160);
        PolygonController controller = new PolygonController(model);
        addTriangle(controller, 10, 20);
        Shape first = controller.finishPolygon();
        addTriangle(controller, 60, 20);
        Shape second = controller.finishPolygon();
        controller.selectShape(0);
        VectorStyle replacement = style(Color.ORANGE, Color.PURPLE, true);
        controller.setStyle(replacement);

        assertTrue(controller.applyStyleToSelected());

        Shape changed = model.getShape(0);
        assertNotSame(first, changed);
        assertEquals(first.getVertices(), changed.getVertices());
        assertEquals(replacement, VectorStyle.from(changed));
        assertSame(second, model.getShape(1));
        assertSame(changed, controller.getSelectedShape());
        assertEquals(0, controller.getSelectedIndex());
        assertTrue(controller.applyStyleToSelected());
        assertEquals(replacement, VectorStyle.from(model.getShape(0)));
    }

    @Test
    void changingFutureStyleDoesNotAlterExistingPolygonUntilApplied() {
        DrawingModel model = new DrawingModel(200, 160);
        PolygonController controller = new PolygonController(model);
        VectorStyle original = controller.getStyle();
        addTriangle(controller, 10, 20);
        Shape shape = controller.finishPolygon();

        controller.setStyle(style(Color.ORANGE, Color.PURPLE, true));

        assertSame(shape, model.getShape(0));
        assertEquals(original, VectorStyle.from(shape));
        addTriangle(controller, 60, 20);
        Shape next = controller.finishPolygon();
        assertEquals(controller.getStyle(), VectorStyle.from(next));
    }

    @Test
    void deletingSelectionRemovesOnlyItAndDeselects() {
        DrawingModel model = new DrawingModel(200, 160);
        PolygonController controller = new PolygonController(model);
        addTriangle(controller, 10, 20);
        Shape first = controller.finishPolygon();
        addTriangle(controller, 60, 20);
        Shape second = controller.finishPolygon();
        controller.selectShape(0);

        assertTrue(controller.deleteSelected());

        assertEquals(List.of(second), model.getShapes());
        assertFalse(model.getShapes().contains(first));
        assertEquals(-1, controller.getSelectedIndex());
        assertNull(controller.getSelectedShape());
        assertFalse(controller.deleteSelected());
        assertFalse(controller.applyStyleToSelected());
    }

    @Test
    void selectionFollowsTheSameShapeWhenEarlierObjectIsRemoved() {
        DrawingModel model = new DrawingModel(200, 160);
        PolygonController controller = new PolygonController(model);
        addTriangle(controller, 10, 20);
        controller.finishPolygon();
        addTriangle(controller, 60, 20);
        Shape selected = controller.finishPolygon();

        model.removeShape(0);

        assertEquals(0, controller.getSelectedIndex());
        assertSame(selected, controller.getSelectedShape());
        model.removeShape(0);
        model.addShape(new Polygon(List.of(new Vertex(10, 10), new Vertex(30, 10), new Vertex(10, 30))));
        assertEquals(-1, controller.getSelectedIndex());
        assertFalse(controller.deleteSelected());
        assertEquals(1, model.getShapes().size());
    }

    @Test
    void invalidSelectionLeavesPreviousSelectionAndStyleIntact() {
        DrawingModel model = new DrawingModel(200, 160);
        PolygonController controller = new PolygonController(model);
        addTriangle(controller, 10, 20);
        Shape shape = controller.finishPolygon();
        VectorStyle selectedStyle = controller.getStyle();

        assertThrows(IndexOutOfBoundsException.class, () -> controller.selectShape(2));
        assertThrows(IndexOutOfBoundsException.class, () -> controller.selectShape(-2));

        assertSame(shape, controller.getSelectedShape());
        assertEquals(selectedStyle, controller.getStyle());
    }

    @Test
    void clearingCancelsDraftAndSelectionAndBothKindsOfObjectsButKeepsStyle() {
        DrawingModel model = new DrawingModel(200, 160);
        EditorController raster = new EditorController(model);
        PolygonController controller = new PolygonController(model);
        VectorStyle style = style(Color.RED, Color.BLUE, true);
        controller.setStyle(style);
        raster.beginStroke(20, 20);
        raster.endStroke();
        addTriangle(controller, 10, 20);
        controller.finishPolygon();
        controller.addVertex(100, 100);

        controller.clear();

        assertTrue(model.getPixels().isEmpty());
        assertTrue(model.getShapes().isEmpty());
        assertTrue(controller.getDraftVertices().isEmpty());
        assertEquals(-1, controller.getSelectedIndex());
        assertEquals(style, controller.getStyle());
        addTriangle(controller, 50, 50);
        assertEquals(style, VectorStyle.from(controller.finishPolygon()));
    }

    @Test
    void rasterClearInvalidatesVectorSelectionWithoutDeletingAnUnrelatedNewPolygon() {
        DrawingModel model = new DrawingModel(200, 160);
        PolygonController controller = new PolygonController(model);
        addTriangle(controller, 10, 20);
        controller.finishPolygon();

        new EditorController(model).clear();
        model.addShape(new Polygon(List.of(new Vertex(10, 10), new Vertex(30, 10), new Vertex(10, 30))));

        assertNull(controller.getSelectedShape());
        assertFalse(controller.applyStyleToSelected());
        assertFalse(controller.deleteSelected());
        assertEquals(1, model.getShapes().size());
    }

    private static void addTriangle(PolygonController controller, double x, double y) {
        assertTrue(controller.addVertex(x, y));
        assertTrue(controller.addVertex(x + 20, y));
        assertTrue(controller.addVertex(x, y + 20));
    }

    private static VectorStyle style(Color fill, Color border, boolean shadow) {
        return new VectorStyle(true, fill, true, border, 3, shadow, Color.GRAY, 8, 6, 12);
    }
}
