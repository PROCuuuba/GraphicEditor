package org.example.graphiceditor.model.vector;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VectorStyleTest {
    @Test
    void defaultsProduceAVisibleBorderWithoutFillOrShadow() {
        VectorStyle defaults = VectorStyle.defaults();
        Shape shape = defaults.decorate(PolygonTest.triangle());

        assertInstanceOf(BorderDecorator.class, shape);
        assertTrue(defaults.borderEnabled());
        assertFalse(defaults.fillEnabled());
        assertFalse(defaults.shadowEnabled());
        assertEquals(Color.BLACK, defaults.borderColor());
        assertEquals(2, defaults.borderWidth());
        assertEquals(defaults, VectorStyle.from(shape));
    }

    @Test
    void decorationAndInspectionRetainTheCompleteStyleAndOriginalGeometry() {
        Polygon polygon = PolygonTest.triangle();
        VectorStyle style = new VectorStyle(true, Color.LIME, true, Color.MAGENTA, 4.5,
                true, Color.color(0.2, 0.3, 0.4, 0.6), -10, 15.5, 8);

        Shape shape = style.decorate(polygon);

        assertInstanceOf(ShadowDecorator.class, shape);
        assertEquals(style, VectorStyle.from(shape));
        assertSame(polygon, VectorStyle.geometryOf(shape));
        assertSame(polygon.getVertices(), shape.getVertices());
    }

    @Test
    void changingFormattingReusesTheGeometryAndRemovesDisabledProperties() {
        Polygon polygon = PolygonTest.triangle();
        Shape original = new VectorStyle(true, Color.RED, true, Color.BLACK, 3,
                true, Color.GRAY, 8, 8, 5).decorate(polygon);
        VectorStyle replacement = new VectorStyle(true, Color.BLUE, false, Color.BLACK, 2,
                false, VectorStyle.defaults().shadowColor(), 8, 8, 5);

        Shape updated = replacement.decorate(VectorStyle.geometryOf(original));

        assertInstanceOf(FillDecorator.class, updated);
        assertEquals(replacement, VectorStyle.from(updated));
        assertSame(polygon, VectorStyle.geometryOf(updated));
        assertTrue(VectorStyle.from(original).shadowEnabled());
        assertTrue(VectorStyle.from(original).borderEnabled());
        assertEquals(Color.RED, VectorStyle.from(original).fillColor());
    }

    @Test
    void bareGeometryHasNoDecorationAndEmptyStyleReturnsTheSameShape() {
        Polygon polygon = PolygonTest.triangle();
        VectorStyle empty = VectorStyle.from(polygon);

        assertFalse(empty.fillEnabled());
        assertFalse(empty.borderEnabled());
        assertFalse(empty.shadowEnabled());
        assertSame(polygon, empty.decorate(polygon));
        assertSame(polygon, VectorStyle.geometryOf(polygon));
    }

    @Test
    void inspectionMatchesInnermostRepeatedDecoratorValuesUsedForRendering() {
        Shape shape = new FillDecorator(new FillDecorator(PolygonTest.triangle(), Color.RED), Color.BLUE);
        RecordingShapeRenderer renderer = new RecordingShapeRenderer();

        shape.render(renderer);

        assertEquals(Color.RED, VectorStyle.from(shape).fillColor());
        assertEquals(VectorStyle.from(shape), renderer.observation.style);
    }

    @Test
    void unknownGeometryCannotBeUsedAsAPolygon() {
        Shape other = new Shape() {
            @Override
            public List<Vertex> getVertices() {
                return List.of(new Vertex(1, 1));
            }

            @Override
            public void render(ShapeRenderer renderer) {
                renderer.polygon(getVertices());
            }
        };

        assertThrows(IllegalArgumentException.class, () -> VectorStyle.geometryOf(other));
        assertThrows(NullPointerException.class, () -> VectorStyle.geometryOf(null));
        assertThrows(NullPointerException.class, () -> VectorStyle.from(null));
        assertThrows(NullPointerException.class, () -> VectorStyle.defaults().decorate(null));
    }

    @Test
    void styleRejectsInvalidParametersEvenWhenThePropertyIsDisabled() {
        for (double width : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new VectorStyle(false, Color.BLUE,
                    false, Color.BLACK, width, false, Color.GRAY, 0, 0, 5));
        }
        for (double offset : new double[]{Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new VectorStyle(false, Color.BLUE,
                    false, Color.BLACK, 1, false, Color.GRAY, offset, 0, 5));
        }
        for (double blur : new double[]{-0.1, 50.1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new VectorStyle(false, Color.BLUE,
                    false, Color.BLACK, 1, false, Color.GRAY, 0, 0, blur));
        }
        assertThrows(NullPointerException.class, () -> new VectorStyle(false, null,
                false, Color.BLACK, 1, false, Color.GRAY, 0, 0, 5));
        assertThrows(NullPointerException.class, () -> new VectorStyle(false, Color.BLUE,
                false, null, 1, false, Color.GRAY, 0, 0, 5));
        assertThrows(NullPointerException.class, () -> new VectorStyle(false, Color.BLUE,
                false, Color.BLACK, 1, false, null, 0, 0, 5));
    }
}
