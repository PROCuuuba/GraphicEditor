package org.example.graphiceditor.model;

import javafx.scene.paint.Color;
import org.example.graphiceditor.controller.EditorController;
import org.example.graphiceditor.controller.PolygonController;
import org.example.graphiceditor.model.vector.VectorStyle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EditorSettingsTest {
    @Test
    void controllersUseSameSettingsObjectAndChangesAffectNewObjects() {
        DrawingModel drawing = new DrawingModel(900, 650);
        EditorSettings settings = new EditorSettings();
        EditorController brushes = new EditorController(drawing, settings);
        PolygonController polygons = new PolygonController(drawing, settings);
        settings.setTool(Tool.CIRCLE);
        settings.setColor(Color.CORAL);
        settings.setBrushSize(25);
        brushes.beginStroke(200, 200);
        assertInstanceOf(Circle.class, drawing.getPixels().getFirst());
        assertEquals(Color.CORAL, drawing.getPixels().getFirst().getColor());
        assertEquals(25, drawing.getPixels().getFirst().getSize());
        var style = new VectorStyle(true, Color.LIME, true, Color.RED, 5, false, Color.BLACK, 1, 2, 3);
        settings.setVectorStyle(style);
        polygons.addVertex(10, 10);
        polygons.addVertex(100, 10);
        polygons.addVertex(10, 100);
        assertEquals(style.fillColor(), VectorStyle.from(polygons.finishPolygon()).fillColor());
        assertSame(settings, brushes.getSettings());
        assertEquals(style, polygons.getStyle());
        settings.setSnapEnabled(false);
        settings.setSnapTolerance(20);
        settings.setBackgroundColor(Color.BEIGE);
        assertFalse(settings.isSnapEnabled());
        assertEquals(20, settings.getSnapTolerance());
        assertEquals(Color.BEIGE, settings.getBackgroundColor());
    }

    @Test
    void invalidSettingsAreRejectedWithoutChangingExistingValues() {
        EditorSettings settings = new EditorSettings();
        assertThrows(IllegalArgumentException.class, () -> settings.setBrushSize(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> settings.setSnapTolerance(-1));
        assertThrows(IllegalArgumentException.class, () -> settings.setTool(null));
        assertEquals(16, settings.getBrushSize());
        assertEquals(10, settings.getSnapTolerance());
        assertEquals(Tool.POINT, settings.getTool());
    }
}
