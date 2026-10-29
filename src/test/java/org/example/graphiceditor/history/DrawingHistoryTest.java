package org.example.graphiceditor.history;

import javafx.scene.paint.Color;
import org.example.graphiceditor.model.Drawing;
import org.example.graphiceditor.model.Point;
import org.example.graphiceditor.model.vector.Polygon;
import org.example.graphiceditor.model.vector.VectorStyle;
import org.example.graphiceditor.model.vector.Vertex;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DrawingHistoryTest {
    @Test
    void completeStrokeIsOneActionAndRedoRestoresExactObjects() {
        Drawing drawing = new Drawing(900, 650);
        DrawingHistory history = new DrawingHistory(drawing);
        history.begin("Штрих");
        for (int x = 0; x < 100; x++) drawing.add(new Point(x, 10, 1, Color.RED));
        assertTrue(history.commit());
        var stroke = drawing.snapshot();
        assertTrue(history.undo());
        assertEquals(0, drawing.size());
        assertFalse(history.canUndo());
        assertTrue(history.redo());
        assertEquals(stroke, drawing.snapshot());
    }

    @Test
    void changedPropertyAndDeleteAreIndependentUndoableActions() {
        Drawing drawing = new Drawing(900, 650);
        DrawingHistory history = new DrawingHistory(drawing);
        Polygon original = new Polygon(List.of(new Vertex(10, 10), new Vertex(100, 10), new Vertex(10, 100)));
        history.begin("Создать");
        drawing.addShape(original);
        history.commit();
        history.begin("Оформление");
        drawing.replaceShape(0, VectorStyle.defaults().decorate(original));
        drawing.setBackgroundColor(Color.BEIGE);
        history.commit();
        var styled = drawing.snapshot();
        history.begin("Удалить");
        drawing.removeShape(0);
        history.commit();
        history.undo();
        assertEquals(styled, drawing.snapshot());
        history.undo();
        assertSame(original, drawing.getShape(0));
        assertEquals(Color.WHITE, drawing.getBackgroundColor());
        history.redo();
        assertEquals(styled, drawing.snapshot());
    }

    @Test
    void noOpPreservesRedoButNewMutationDropsItAndCancelRestoresBefore() {
        Drawing drawing = new Drawing(900, 650);
        DrawingHistory history = new DrawingHistory(drawing);
        history.begin("Первый");
        drawing.add(new Point(1, 1, 1, Color.BLACK));
        history.commit();
        history.undo();
        history.begin("Без изменения");
        assertFalse(history.commit());
        assertTrue(history.canRedo());
        history.begin("Отменённый жест");
        drawing.add(new Point(2, 2, 1, Color.BLUE));
        history.cancel();
        assertTrue(drawing.getPixels().isEmpty());
        assertTrue(history.canRedo());
        history.begin("Новый");
        drawing.add(new Point(3, 3, 1, Color.RED));
        history.commit();
        assertFalse(history.canRedo());
        history.clear();
        assertFalse(history.canUndo());
        assertFalse(history.canRedo());
        assertEquals(1, drawing.size());
    }

    @Test
    void historyRetainsOnlyLastHundredActions() {
        Drawing drawing = new Drawing(900, 650);
        DrawingHistory history = new DrawingHistory(drawing);
        for (int x = 0; x < 102; x++) {
            history.begin("Точка");
            drawing.add(new Point(x, 0, 1, Color.BLACK));
            history.commit();
        }
        for (int x = 0; x < 100; x++) assertTrue(history.undo());
        assertFalse(history.undo());
        assertEquals(2, drawing.size());
    }
}
