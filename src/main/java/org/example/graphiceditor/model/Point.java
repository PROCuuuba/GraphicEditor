package org.example.graphiceditor.model;

import javafx.scene.paint.Color;

public final class Point extends Pixel {
    public Point(double x, double y, double size, Color color) {
        super(x, y, 1, color);
        if (!Double.isFinite(size) || size <= 0) {
            throw new IllegalArgumentException("Размер должен быть конечным и положительным");
        }
    }

    @Override
    public void render(PixelRenderer renderer) {
        renderer.point(this);
    }
}
