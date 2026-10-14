package org.example.graphiceditor.model.vector;

public record Vertex(double x, double y) {
    public Vertex {
        if (!Double.isFinite(x) || !Double.isFinite(y) || x < 0 || y < 0) {
            throw new IllegalArgumentException("Координаты вершины должны быть конечными и неотрицательными");
        }
    }
}
