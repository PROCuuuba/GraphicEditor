package org.example.graphiceditor.model.vector;

import javafx.scene.paint.Color;

import java.util.Objects;

public final class BorderDecorator extends ShapeDecorator {
    private final Color color;
    private final double width;

    public BorderDecorator(Shape wrapped, Color color, double width) {
        super(wrapped);
        this.color = Objects.requireNonNull(color, "color");
        if (!Double.isFinite(width) || width <= 0) {
            throw new IllegalArgumentException("Толщина контура должна быть конечной и положительной");
        }
        this.width = width;
    }

    public Color getColor() {
        return color;
    }

    public double getWidth() {
        return width;
    }

    @Override
    public void render(ShapeRenderer renderer) {
        getWrapped().render(Objects.requireNonNull(renderer, "renderer").withBorder(color, width));
    }
}
