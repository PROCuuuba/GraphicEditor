package org.example.graphiceditor.model.vector;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ShapeDecoratorTest {
    @Test
    @DisplayName("T2-04: заливка передаёт свой цвет и сохраняет геометрию")
    void fillAddsItsPropertyAndDelegatesToTheSamePolygon() {
        Polygon polygon = PolygonTest.triangle();
        FillDecorator fill = new FillDecorator(polygon, Color.ORANGE);
        RecordingShapeRenderer renderer = new RecordingShapeRenderer();

        fill.render(renderer);

        assertSame(polygon, fill.getWrapped());
        assertSame(polygon.getVertices(), fill.getVertices());
        assertSame(polygon.getVertices(), renderer.observation.vertices);
        assertTrue(renderer.observation.style.fillEnabled());
        assertEquals(Color.ORANGE, renderer.observation.style.fillColor());
        assertFalse(renderer.observation.style.borderEnabled());
        assertFalse(renderer.observation.style.shadowEnabled());
        assertEquals(List.of("fill", "polygon"), renderer.observation.calls);
    }

    @Test
    @DisplayName("T2-05: контур передаёт цвет и толщину")
    void borderAddsItsColorAndWidth() {
        BorderDecorator border = new BorderDecorator(PolygonTest.triangle(), Color.RED, 4.5);
        RecordingShapeRenderer renderer = new RecordingShapeRenderer();

        border.render(renderer);

        assertTrue(renderer.observation.style.borderEnabled());
        assertEquals(Color.RED, renderer.observation.style.borderColor());
        assertEquals(4.5, renderer.observation.style.borderWidth());
        assertFalse(renderer.observation.style.fillEnabled());
        assertFalse(renderer.observation.style.shadowEnabled());
        assertEquals(List.of("border", "polygon"), renderer.observation.calls);
    }

    @Test
    @DisplayName("T2-06: тень передаёт цвет, смещение и размытие")
    void shadowAllowsNegativeOffsetsAndRetainsItsProperties() {
        ShadowDecorator shadow = new ShadowDecorator(PolygonTest.triangle(), Color.GRAY, -12, 7.5, 50);
        RecordingShapeRenderer renderer = new RecordingShapeRenderer();

        shadow.render(renderer);

        assertTrue(renderer.observation.style.shadowEnabled());
        assertEquals(Color.GRAY, renderer.observation.style.shadowColor());
        assertEquals(-12, renderer.observation.style.shadowOffsetX());
        assertEquals(7.5, renderer.observation.style.shadowOffsetY());
        assertEquals(50, renderer.observation.style.shadowBlur());
        assertFalse(renderer.observation.style.fillEnabled());
        assertFalse(renderer.observation.style.borderEnabled());
        assertEquals(List.of("shadow", "polygon"), renderer.observation.calls);
    }

    @Test
    @DisplayName("T2-07 и T2-09: цепочка декораторов делегирует вызовы и сохраняет все свойства")
    void completeChainDelegatesFromOuterToInnerAndRendersOneGeometry() {
        Polygon polygon = PolygonTest.triangle();
        Shape shape = new ShadowDecorator(new BorderDecorator(new FillDecorator(polygon, Color.BLUE),
                Color.RED, 3), Color.GRAY, 8, 9, 5);
        RecordingShapeRenderer renderer = new RecordingShapeRenderer();

        shape.render(renderer);

        assertEquals(List.of("shadow", "border", "fill", "polygon"), renderer.observation.calls);
        assertEquals(1, renderer.observation.polygonCount);
        assertSame(polygon.getVertices(), renderer.observation.vertices);
        assertTrue(renderer.observation.style.fillEnabled());
        assertTrue(renderer.observation.style.borderEnabled());
        assertTrue(renderer.observation.style.shadowEnabled());
    }

    @Test
    @DisplayName("T2-08: все восемь комбинаций оформления работают независимо")
    void allEightIndependentPropertyCombinationsAreRendered() {
        Polygon polygon = PolygonTest.triangle();
        for (int combination = 0; combination < 8; combination++) {
            VectorStyle style = new VectorStyle((combination & 1) != 0, Color.BLUE,
                    (combination & 2) != 0, Color.RED, 3,
                    (combination & 4) != 0, Color.GRAY, -5, 8, 0);
            RecordingShapeRenderer renderer = new RecordingShapeRenderer();

            style.decorate(polygon).render(renderer);

            assertEquals(style.fillEnabled(), renderer.observation.style.fillEnabled());
            assertEquals(style.borderEnabled(), renderer.observation.style.borderEnabled());
            assertEquals(style.shadowEnabled(), renderer.observation.style.shadowEnabled());
            assertSame(polygon.getVertices(), renderer.observation.vertices);
            assertEquals(1, renderer.observation.polygonCount);
        }
    }

    @Test
    @DisplayName("T2-13: все шесть порядков декораторов сохраняют свойства фигуры")
    void everyPermutationPreservesAllPropertiesAndDelegatesInOrder() {
        Polygon polygon = PolygonTest.triangle();
        for (String order : List.of("FBS", "FSB", "BFS", "BSF", "SFB", "SBF")) {
            Shape shape = polygon;
            for (char effect : order.toCharArray()) {
                shape = switch (effect) {
                    case 'F' -> new FillDecorator(shape, Color.BLUE);
                    case 'B' -> new BorderDecorator(shape, Color.RED, 3);
                    case 'S' -> new ShadowDecorator(shape, Color.GRAY, -5, 8, 4);
                    default -> throw new AssertionError(effect);
                };
            }
            RecordingShapeRenderer renderer = new RecordingShapeRenderer();

            shape.render(renderer);

            assertEquals(new VectorStyle(true, Color.BLUE, true, Color.RED, 3,
                    true, Color.GRAY, -5, 8, 4), renderer.observation.style, order);
            List<String> expected = new StringBuilder(order).reverse().chars()
                    .mapToObj(effect -> switch (effect) {
                        case 'F' -> "fill";
                        case 'B' -> "border";
                        case 'S' -> "shadow";
                        default -> throw new AssertionError(effect);
                    }).toList();
            assertEquals(expected, renderer.observation.calls.subList(0, 3), order);
            assertEquals("polygon", renderer.observation.calls.getLast());
            assertEquals(1, renderer.observation.polygonCount);
        }
    }

    @Test
    @DisplayName("T2-10: новый декоратор прозрачности подключается без изменения Polygon")
    void customTransparencyDecoratorComposesWithExistingDecorators() {
        Polygon polygon = PolygonTest.triangle();
        Shape shape = new TransparencyDecorator(new FillDecorator(polygon, Color.BLUE), 0.4);
        RecordingShapeRenderer renderer = new RecordingShapeRenderer();

        shape.render(renderer);

        assertSame(polygon.getVertices(), renderer.observation.vertices);
        assertEquals(Color.BLUE, renderer.observation.style.fillColor());
        assertEquals(0.4, renderer.observation.opacity);
        assertEquals(List.of("opacity", "fill", "polygon"), renderer.observation.calls);
        assertEquals(1, renderer.observation.polygonCount);
    }

    @Test
    void invalidParametersCannotEnterADecoratorChain() {
        Polygon polygon = PolygonTest.triangle();
        for (double width : new double[]{0, -1, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new BorderDecorator(polygon, Color.BLACK, width));
        }
        for (double offset : new double[]{Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class,
                    () -> new ShadowDecorator(polygon, Color.BLACK, offset, 0, 5));
            assertThrows(IllegalArgumentException.class,
                    () -> new ShadowDecorator(polygon, Color.BLACK, 0, offset, 5));
        }
        for (double blur : new double[]{-1, 50.1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class,
                    () -> new ShadowDecorator(polygon, Color.BLACK, 0, 0, blur));
        }
        assertThrows(NullPointerException.class, () -> new FillDecorator(null, Color.BLUE));
        assertThrows(NullPointerException.class, () -> new FillDecorator(polygon, null));
        assertThrows(NullPointerException.class, () -> new BorderDecorator(polygon, null, 1));
        assertThrows(NullPointerException.class, () -> new ShadowDecorator(polygon, null, 0, 0, 5));
        assertDoesNotThrow(() -> new ShadowDecorator(polygon, Color.BLACK, -100, -100, 0));
    }

    private static final class TransparencyDecorator extends ShapeDecorator {
        private final double opacity;

        TransparencyDecorator(Shape wrapped, double opacity) {
            super(wrapped);
            this.opacity = opacity;
        }

        @Override
        public void render(ShapeRenderer renderer) {
            getWrapped().render(renderer.withOpacity(opacity));
        }
    }
}
