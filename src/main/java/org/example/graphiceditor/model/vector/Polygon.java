package org.example.graphiceditor.model.vector;

import java.util.List;
import java.util.Objects;

public final class Polygon implements Shape {
    private final List<Vertex> vertices;

    public Polygon(List<Vertex> vertices) {
        Objects.requireNonNull(vertices, "vertices");
        if (vertices.size() < 3) {
            throw new IllegalArgumentException("Полигон должен содержать не менее трёх вершин");
        }
        this.vertices = List.copyOf(vertices);
    }

    @Override
    public List<Vertex> getVertices() {
        return vertices;
    }

    @Override
    public void render(ShapeRenderer renderer) {
        Objects.requireNonNull(renderer, "renderer").polygon(vertices);
    }
}
