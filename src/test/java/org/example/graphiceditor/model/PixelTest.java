package org.example.graphiceditor.model;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PixelTest {
    @Test
    @DisplayName("T1: точка сохраняет координаты и имеет размер один пиксель")
    void pointAlwaysHasOnePixelSize() {
        Point point = new Point(12.5, 8.75, 64, Color.RED);

        assertAll(
                () -> assertEquals(12.5, point.getX()),
                () -> assertEquals(8.75, point.getY()),
                () -> assertEquals(1, point.getSize()),
                () -> assertEquals(Color.RED, point.getColor())
        );
    }

    @Test
    @DisplayName("T2: круг сохраняет координаты, размер и цвет")
    void circlePreservesFractionalCoordinatesSizeAndColor() {
        assertBrushParameters(new Circle(12.5, 8.75, 24.5, Color.CORNFLOWERBLUE));
    }

    @Test
    @DisplayName("T3: квадрат сохраняет координаты, размер и цвет")
    void squarePreservesFractionalCoordinatesSizeAndColor() {
        assertBrushParameters(new Square(12.5, 8.75, 24.5, Color.CORNFLOWERBLUE));
    }

    @Test
    void invalidCoordinatesCannotEnterTheModel() {
        for (double coordinate : new double[]{-1, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY}) {
            assertAll(
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> new Point(coordinate, 1, 1, Color.BLACK)),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> new Circle(1, coordinate, 10, Color.BLACK)),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> new Square(coordinate, 1, 10, Color.BLACK))
            );
        }
    }

    @Test
    void circleAndSquareRejectInvalidSizes() {
        for (double size : new double[]{0, -1, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY}) {
            assertAll(
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> new Circle(1, 1, size, Color.BLACK)),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> new Square(1, 1, size, Color.BLACK))
            );
        }
    }

    @Test
    @DisplayName("T4: элементы списка Pixel полиморфно вызывают операции отрисовки")
    void eachPixelDispatchesToItsRendererOperation() {
        RecordingRenderer renderer = new RecordingRenderer();
        Point point = new Point(1, 2, 1, Color.RED);
        Circle circle = new Circle(3, 4, 10, Color.GREEN);
        Square square = new Square(5, 6, 20, Color.BLUE);

        List<Pixel> pixels = List.of(point, circle, square);
        pixels.forEach(pixel -> pixel.render(renderer));

        assertAll(
                () -> assertSame(point, renderer.point),
                () -> assertSame(circle, renderer.circle),
                () -> assertSame(square, renderer.square),
                () -> assertEquals(3, renderer.calls)
        );
    }

    private static void assertBrushParameters(Pixel pixel) {
        assertAll(
                () -> assertEquals(12.5, pixel.getX()),
                () -> assertEquals(8.75, pixel.getY()),
                () -> assertEquals(24.5, pixel.getSize()),
                () -> assertEquals(Color.CORNFLOWERBLUE, pixel.getColor())
        );
    }

    private static final class RecordingRenderer implements PixelRenderer {
        private Point point;
        private Circle circle;
        private Square square;
        private int calls;

        @Override
        public void point(Point point) {
            this.point = point;
            calls++;
        }

        @Override
        public void circle(Circle circle) {
            this.circle = circle;
            calls++;
        }

        @Override
        public void square(Square square) {
            this.square = square;
            calls++;
        }
    }
}
