package org.example.graphiceditor.controller;

import javafx.scene.paint.Color;
import org.example.graphiceditor.model.Circle;
import org.example.graphiceditor.model.DrawingModel;
import org.example.graphiceditor.model.Pixel;
import org.example.graphiceditor.model.Point;
import org.example.graphiceditor.model.Square;
import org.example.graphiceditor.model.Tool;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EditorControllerTest {
    @Test
    @DisplayName("T15: выбранный инструмент через контроллер и фабрику создаёт объект модели")
    void eachSelectedToolCreatesItsPixelInTheModel() {
        DrawingModel model = new DrawingModel(200, 160);
        EditorController controller = new EditorController(model);
        controller.setColor(Color.RED);
        controller.setBrushSize(24);

        for (Tool tool : Tool.values()) {
            controller.setTool(tool);
            List<Pixel> added = controller.beginStroke(30, 40);
            controller.endStroke();

            assertEquals(tool, controller.getTool());
            assertEquals(1, added.size());
            Pixel pixel = added.getFirst();
            switch (tool) {
                case POINT -> assertInstanceOf(Point.class, pixel);
                case CIRCLE -> assertInstanceOf(Circle.class, pixel);
                case SQUARE -> assertInstanceOf(Square.class, pixel);
            }
            assertAll(
                    () -> assertSame(pixel, model.getPixels().getLast()),
                    () -> assertEquals(30, pixel.getX()),
                    () -> assertEquals(40, pixel.getY()),
                    () -> assertEquals(Color.RED, pixel.getColor()),
                    () -> assertEquals(tool == Tool.POINT ? 1 : 24, pixel.getSize())
            );
        }

        assertEquals(3, model.size());
    }

    @Test
    @DisplayName("Контрольный пример: красные точки непрерывно повторяют движение мыши")
    void redPointStrokeInterpolatesTheEntireMousePath() {
        DrawingModel model = new DrawingModel(200, 160);
        EditorController controller = new EditorController(model);
        controller.setColor(Color.RED);
        controller.setTool(Tool.POINT);

        controller.beginStroke(10, 10);
        controller.continueStroke(70, 90);
        controller.endStroke();

        List<Pixel> pixels = model.getPixels();
        assertEquals(101, pixels.size());
        assertPathEndsAt(pixels, 10, 10, 70, 90);
        assertContinuousPath(pixels, 1);
        assertTrue(pixels.stream().allMatch(pixel -> pixel instanceof Point));
        assertTrue(pixels.stream().allMatch(pixel -> Color.RED.equals(pixel.getColor())));
        assertTrue(pixels.stream().allMatch(pixel -> pixel.getSize() == 1));
    }

    @Test
    void circleAndSquareBrushesFillGapsBetweenMouseEvents() {
        for (Tool tool : new Tool[]{Tool.CIRCLE, Tool.SQUARE}) {
            DrawingModel model = new DrawingModel(200, 160);
            EditorController controller = new EditorController(model);
            controller.setTool(tool);
            controller.setBrushSize(24);

            controller.beginStroke(10, 10);
            List<Pixel> added = controller.continueStroke(70, 90);
            controller.endStroke();

            assertFalse(added.isEmpty());
            assertPathEndsAt(model.getPixels(), 10, 10, 70, 90);
            assertContinuousPath(model.getPixels(), 6);
            assertTrue(model.getPixels().stream().allMatch(pixel -> pixel.getSize() == 24));
        }
    }

    @Test
    void mouseMovementWithoutPressingOrAfterReleaseDoesNotDraw() {
        DrawingModel model = new DrawingModel(200, 160);
        EditorController controller = new EditorController(model);

        assertTrue(controller.continueStroke(10, 10).isEmpty());
        assertEquals(0, model.size());
        controller.beginStroke(20, 20);
        controller.endStroke();
        controller.endStroke();

        assertTrue(controller.continueStroke(30, 30).isEmpty());
        assertEquals(1, model.size());
    }

    @Test
    void leavingAndReenteringCanvasDoesNotConnectAcrossTheGap() {
        DrawingModel model = new DrawingModel(100, 80);
        EditorController controller = new EditorController(model);
        controller.beginStroke(10, 10);

        assertTrue(controller.continueStroke(120, 10).isEmpty());
        List<Pixel> reentered = controller.continueStroke(80, 10);

        assertEquals(1, reentered.size());
        assertEquals(2, model.size());
        assertEquals(80, reentered.getFirst().getX());
        assertEquals(10, reentered.getFirst().getY());
        controller.continueStroke(83, 10);
        assertEquals(5, model.size());
    }

    @Test
    void pressingOutsideCanvasDoesNotStartAStroke() {
        DrawingModel model = new DrawingModel(100, 80);
        EditorController controller = new EditorController(model);

        for (double[] coordinate : new double[][]{
                {-1, 10}, {10, -1}, {100, 10}, {10, 80},
                {Double.NaN, 10}, {10, Double.POSITIVE_INFINITY}
        }) {
            assertTrue(controller.beginStroke(coordinate[0], coordinate[1]).isEmpty());
            assertTrue(controller.continueStroke(10, 10).isEmpty());
        }

        assertEquals(0, model.size());
    }

    @Test
    void brushSettingsAreCapturedUntilMouseRelease() {
        DrawingModel model = new DrawingModel(200, 160);
        EditorController controller = new EditorController(model);
        controller.setTool(Tool.POINT);
        controller.setColor(Color.RED);
        controller.beginStroke(10, 10);

        controller.setTool(Tool.SQUARE);
        controller.setColor(Color.BLUE);
        controller.setBrushSize(24);
        controller.continueStroke(15, 10);

        assertTrue(model.getPixels().stream().allMatch(pixel -> pixel instanceof Point));
        assertTrue(model.getPixels().stream().allMatch(pixel -> Color.RED.equals(pixel.getColor())));
        assertTrue(model.getPixels().stream().allMatch(pixel -> pixel.getSize() == 1));
        controller.endStroke();
        Pixel nextStroke = controller.beginStroke(30, 30).getFirst();

        assertInstanceOf(Square.class, nextStroke);
        assertEquals(Color.BLUE, nextStroke.getColor());
        assertEquals(24, nextStroke.getSize());
    }

    @Test
    void clearingDuringAStrokeCancelsDrawingAndAllowsANewStroke() {
        DrawingModel model = new DrawingModel(200, 160);
        EditorController controller = new EditorController(model);
        controller.setTool(Tool.CIRCLE);
        controller.setColor(Color.GREEN);
        controller.setBrushSize(20);
        controller.beginStroke(10, 10);
        controller.continueStroke(30, 10);

        controller.clear();

        assertEquals(0, model.size());
        assertTrue(model.getPixels().isEmpty());
        assertTrue(controller.continueStroke(50, 10).isEmpty());
        assertEquals(0, model.size());
        Pixel newPixel = controller.beginStroke(70, 10).getFirst();
        assertEquals(1, model.size());
        assertInstanceOf(Circle.class, newPixel);
        assertEquals(Color.GREEN, newPixel.getColor());
        assertEquals(20, newPixel.getSize());
    }

    @Test
    void consecutiveStrokesDoNotConnectToPreviousEndpoints() {
        DrawingModel model = new DrawingModel(200, 160);
        EditorController controller = new EditorController(model);
        controller.beginStroke(10, 10);
        controller.endStroke();

        controller.beginStroke(150, 120);

        assertEquals(2, model.size());
        assertEquals(150, model.getPixels().getLast().getX());
        assertEquals(120, model.getPixels().getLast().getY());
    }

    @Test
    void stationaryMouseDoesNotAddDuplicateSamples() {
        DrawingModel model = new DrawingModel(200, 160);
        EditorController controller = new EditorController(model);
        controller.beginStroke(10, 10);

        assertTrue(controller.continueStroke(10, 10).isEmpty());
        assertEquals(1, model.size());
    }

    @Test
    void brushSizeAcceptsBothLimitsAndRejectsInvalidValues() {
        EditorController controller = new EditorController(new DrawingModel(200, 160));
        controller.setBrushSize(1);
        assertEquals(1, controller.getBrushSize());
        controller.setBrushSize(100);
        assertEquals(100, controller.getBrushSize());

        for (double size : new double[]{0, -1, 100.1, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> controller.setBrushSize(size));
            assertEquals(100, controller.getBrushSize());
        }
    }

    @Test
    void invalidToolSelectionDoesNotChangeTheCurrentTool() {
        EditorController controller = new EditorController(new DrawingModel(200, 160));
        controller.setTool(Tool.CIRCLE);

        assertThrows(IllegalArgumentException.class, () -> controller.setTool(null));

        assertEquals(Tool.CIRCLE, controller.getTool());
    }

    private static void assertPathEndsAt(List<Pixel> pixels, double startX, double startY, double endX, double endY) {
        assertAll(
                () -> assertEquals(startX, pixels.getFirst().getX(), 1e-9),
                () -> assertEquals(startY, pixels.getFirst().getY(), 1e-9),
                () -> assertEquals(endX, pixels.getLast().getX(), 1e-9),
                () -> assertEquals(endY, pixels.getLast().getY(), 1e-9)
        );
    }

    private static void assertContinuousPath(List<Pixel> pixels, double maximumGap) {
        for (int index = 1; index < pixels.size(); index++) {
            Pixel previous = pixels.get(index - 1);
            Pixel current = pixels.get(index);
            double gap = Math.hypot(current.getX() - previous.getX(), current.getY() - previous.getY());
            assertTrue(gap > 0 && gap <= maximumGap + 1e-9,
                    "Разрыв между соседними элементами: " + gap);
        }
    }
}
