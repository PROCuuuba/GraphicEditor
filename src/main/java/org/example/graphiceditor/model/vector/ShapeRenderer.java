package org.example.graphiceditor.model.vector;

import javafx.scene.paint.Color;

import java.util.List;

public interface ShapeRenderer {
    ShapeRenderer withFill(Color color);
    ShapeRenderer withBorder(Color color, double width);
    ShapeRenderer withShadow(Color color, double offsetX, double offsetY, double blur);
    ShapeRenderer withOpacity(double opacity);
    void polygon(List<Vertex> vertices);
}
