package org.example.graphiceditor.factory;

import javafx.scene.paint.Color;
import org.example.graphiceditor.model.Circle;
import org.example.graphiceditor.model.Pixel;
import org.example.graphiceditor.model.Point;
import org.example.graphiceditor.model.Square;
import org.example.graphiceditor.model.Tool;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PixelFactoryTest {
    @Test
    @DisplayName("T5: фабрика POINT создаёт точку")
    void pointFactoryCreatesPoint() {
        assertInstanceOf(Point.class, PixelFactory.forTool(Tool.POINT).create(5, 10, 12, Color.RED));
    }

    @Test
    @DisplayName("T6: фабрика CIRCLE создаёт круг")
    void circleFactoryCreatesCircle() {
        assertInstanceOf(Circle.class, PixelFactory.forTool(Tool.CIRCLE).create(5, 10, 12, Color.RED));
    }

    @Test
    @DisplayName("T7: фабрика SQUARE создаёт квадрат")
    void squareFactoryCreatesSquare() {
        assertInstanceOf(Square.class, PixelFactory.forTool(Tool.SQUARE).create(5, 10, 12, Color.RED));
    }

    @Test
    void creatorsRetainCoordinatesAndSelectedColor() {
        for (Tool tool : Tool.values()) {
            Pixel pixel = PixelFactory.forTool(tool).create(5.5, 10.25, 12, Color.MEDIUMPURPLE);

            assertAll(
                    () -> assertEquals(5.5, pixel.getX()),
                    () -> assertEquals(10.25, pixel.getY()),
                    () -> assertEquals(Color.MEDIUMPURPLE, pixel.getColor()),
                    () -> assertEquals(tool == Tool.POINT ? 1 : 12, pixel.getSize())
            );
        }
    }

    @Test
    @DisplayName("T8: неизвестный инструмент приводит к контролируемой ошибке")
    void rejectsMissingTool() {
        assertThrows(IllegalArgumentException.class, () -> PixelFactory.forTool(null));
    }
}
