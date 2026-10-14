package org.example.graphiceditor.model.vector;

import javafx.scene.paint.Color;

import java.util.Objects;

public final class FillDecorator extends ShapeDecorator {
    private final Color color;

    public FillDecorator(Shape wrapped, Color color) {
        super(wrapped);
        this.color = Objects.requireNonNull(color, "color");
    }

    public Color getColor() {
        return color;
    }

    @Override
    public void render(ShapeRenderer renderer) {
        getWrapped().render(Objects.requireNonNull(renderer, "renderer").withFill(color));
    }
}
