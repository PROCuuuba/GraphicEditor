package org.example.graphiceditor.model;

import javafx.scene.paint.Color;

public final class Circle extends Pixel {
    public Circle(double x, double y, double size, Color color) {
        super(x, y, size, color);
    }

    @Override
    public void render(PixelRenderer renderer) {
        renderer.circle(this);
    }
}
