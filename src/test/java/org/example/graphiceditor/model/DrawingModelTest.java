package org.example.graphiceditor.model;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DrawingModelTest {
    @Test
    @DisplayName("T12: начало координат входит в рабочую область")
    void acceptsCanvasOriginAndCoordinatesJustInsideTheEdges() {
        DrawingModel model = new DrawingModel(100, 80);

        assertAll(
                () -> assertTrue(model.contains(0, 0)),
                () -> assertTrue(model.contains(Math.nextDown(100.0), Math.nextDown(80.0))),
                () -> assertTrue(model.add(new Point(0, 0, 1, Color.BLACK))),
                () -> assertTrue(model.add(new Point(Math.nextDown(100.0), Math.nextDown(80.0), 1, Color.BLACK)))
        );
        assertEquals(2, model.size());
    }

    @Test
    @DisplayName("T14: максимальные координаты и выход за границы обработаны")
    void rejectsCoordinatesOnOrBeyondMaximumBounds() {
        DrawingModel model = new DrawingModel(100, 80);

        assertAll(
                () -> assertFalse(model.contains(100, 20)),
                () -> assertFalse(model.contains(20, 80)),
                () -> assertFalse(model.contains(100, 80)),
                () -> assertFalse(model.add(new Point(100, 20, 1, Color.BLACK))),
                () -> assertFalse(model.add(new Circle(20, 80, 10, Color.BLACK))),
                () -> assertFalse(model.add(new Square(101, 81, 10, Color.BLACK)))
        );
        assertEquals(0, model.size());
    }

    @Test
    @DisplayName("T13: отрицательные и неконечные координаты вне холста")
    void negativeAndNonfiniteCoordinatesAreNotOnCanvas() {
        DrawingModel model = new DrawingModel(100, 80);

        for (double coordinate : new double[]{-1, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY}) {
            assertAll(
                    () -> assertFalse(model.contains(coordinate, 10)),
                    () -> assertFalse(model.contains(10, coordinate))
            );
        }
    }

    @Test
    @DisplayName("T9: добавленный элемент присутствует в модели")
    void addedPixelIsPresentInTheModel() {
        DrawingModel model = new DrawingModel(100, 80);
        Point point = new Point(10, 10, 1, Color.RED);

        assertTrue(model.add(point));

        assertEquals(1, model.size());
        assertSame(point, model.getPixels().getFirst());
    }

    @Test
    void canvasDimensionsMustBeFiniteAndPositive() {
        for (double dimension : new double[]{0, -1, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY}) {
            assertAll(
                    () -> assertThrows(IllegalArgumentException.class, () -> new DrawingModel(dimension, 80)),
                    () -> assertThrows(IllegalArgumentException.class, () -> new DrawingModel(100, dimension))
            );
        }
    }

    @Test
    @DisplayName("T10: несколько элементов сохраняют количество и порядок")
    void readOnlyPixelsPreserveDrawingOrder() {
        DrawingModel model = new DrawingModel(100, 80);
        Point point = new Point(10, 10, 1, Color.RED);
        Circle circle = new Circle(20, 20, 10, Color.BLUE);
        model.add(point);
        model.add(circle);
        List<Pixel> pixels = model.getPixels();

        assertEquals(List.of(point, circle), pixels);
        assertAll(
                () -> assertThrows(UnsupportedOperationException.class, pixels::clear),
                () -> assertThrows(UnsupportedOperationException.class, () -> pixels.remove(0)),
                () -> assertThrows(UnsupportedOperationException.class, () -> pixels.add(point))
        );
        assertEquals(2, model.size());
    }

    @Test
    @DisplayName("T11: очистка удаляет все элементы и позволяет рисовать снова")
    void clearRemovesAllPixelsAndAllowsNewDrawing() {
        DrawingModel model = new DrawingModel(100, 80);
        model.add(new Point(10, 10, 1, Color.RED));
        model.add(new Square(20, 20, 8, Color.BLUE));

        model.clear();

        assertEquals(0, model.size());
        assertTrue(model.getPixels().isEmpty());
        assertTrue(model.add(new Circle(30, 30, 12, Color.GREEN)));
        assertEquals(1, model.size());
    }
}
