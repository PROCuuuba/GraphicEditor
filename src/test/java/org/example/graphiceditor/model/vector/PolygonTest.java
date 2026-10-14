package org.example.graphiceditor.model.vector;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PolygonTest {
    @Test
    @DisplayName("T2-01: полигон сохраняет вершины и защищает их от изменения")
    void polygonKeepsAnImmutableCopyOfTheOrderedVertices() {
        List<Vertex> source = new ArrayList<>(List.of(
                new Vertex(0, 0), new Vertex(50.5, 10.25), new Vertex(25, 70)));
        List<Vertex> expected = List.copyOf(source);
        Polygon polygon = new Polygon(source);

        source.clear();

        assertEquals(expected, polygon.getVertices());
        assertThrows(UnsupportedOperationException.class,
                () -> polygon.getVertices().add(new Vertex(100, 100)));
    }

    @Test
    void fewerThanThreeVerticesAndMissingVerticesAreRejected() {
        for (int count = 0; count < 3; count++) {
            List<Vertex> vertices = new ArrayList<>();
            for (int index = 0; index < count; index++) {
                vertices.add(new Vertex(index, index));
            }
            assertThrows(IllegalArgumentException.class, () -> new Polygon(vertices));
        }
        assertThrows(NullPointerException.class, () -> new Polygon(null));
        assertThrows(NullPointerException.class,
                () -> new Polygon(Arrays.asList(new Vertex(1, 1), null, new Vertex(20, 20))));
    }

    @Test
    void concaveAndSelfIntersectingPolygonsRetainTheirSpecifiedGeometry() {
        List<Vertex> concave = List.of(new Vertex(10, 10), new Vertex(90, 10),
                new Vertex(40, 40), new Vertex(90, 90), new Vertex(10, 90));
        List<Vertex> crossing = List.of(new Vertex(10, 10), new Vertex(90, 90),
                new Vertex(90, 10), new Vertex(10, 90));

        assertEquals(concave, new Polygon(concave).getVertices());
        assertEquals(crossing, new Polygon(crossing).getVertices());
    }

    @Test
    @DisplayName("T2-02: отрисовка получает все вершины в исходном порядке")
    void renderingPassesGeometryToTheRendererExactlyOnce() {
        Polygon polygon = triangle();
        RecordingShapeRenderer renderer = new RecordingShapeRenderer();

        polygon.render(renderer);

        assertSame(polygon.getVertices(), renderer.observation.vertices);
        assertEquals(1, renderer.observation.polygonCount);
        assertEquals(List.of("polygon"), renderer.observation.calls);
        assertThrows(NullPointerException.class, () -> polygon.render(null));
    }

    @Test
    void verticesRejectNegativeAndNonfiniteCoordinates() {
        for (double value : new double[]{-1, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new Vertex(value, 10));
            assertThrows(IllegalArgumentException.class, () -> new Vertex(10, value));
        }
        assertEquals(new Vertex(0, 0), new Vertex(0, 0));
        assertEquals(12.75, new Vertex(12.75, 1.5).x());
    }

    static Polygon triangle() {
        return new Polygon(List.of(new Vertex(10, 10), new Vertex(100, 10), new Vertex(55, 100)));
    }
}
