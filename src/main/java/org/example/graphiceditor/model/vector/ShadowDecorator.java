package org.example.graphiceditor.model.vector;

import javafx.scene.paint.Color;

import java.util.Objects;

public final class ShadowDecorator extends ShapeDecorator {
    private final Color color;
    private final double offsetX;
    private final double offsetY;
    private final double blur;

    public ShadowDecorator(Shape wrapped, Color color, double offsetX, double offsetY, double blur) {
        super(wrapped);
        this.color = Objects.requireNonNull(color, "color");
        if (!Double.isFinite(offsetX) || !Double.isFinite(offsetY)) {
            throw new IllegalArgumentException("Смещение тени должно быть конечным");
        }
        if (!Double.isFinite(blur) || blur < 0 || blur > 50) {
            throw new IllegalArgumentException("Размытие тени должно быть от 0 до 50");
        }
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.blur = blur;
    }

    public Color getColor() {
        return color;
    }

    public double getOffsetX() {
        return offsetX;
    }

    public double getOffsetY() {
        return offsetY;
    }

    public double getBlur() {
        return blur;
    }

    @Override
    public void render(ShapeRenderer renderer) {
        getWrapped().render(Objects.requireNonNull(renderer, "renderer")
                .withShadow(color, offsetX, offsetY, blur));
    }
}
