package org.example.graphiceditor.model;

import javafx.scene.paint.Color;
import java.util.Objects;

public abstract class Pixel {
    private final double x;
    private final double y;
    private final double size;
    private final Color color;

    protected Pixel(double x, double y, double size, Color color) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || x < 0 || y < 0) {
            throw new IllegalArgumentException("Координаты должны быть конечными и неотрицательными");
        }
        if (!Double.isFinite(size) || size <= 0) {
            throw new IllegalArgumentException("Размер должен быть конечным и положительным");
        }
        this.x = x;
        this.y = y;
        this.size = size;
        this.color = Objects.requireNonNull(color, "color");
    }

    public final double getX() { return x; }
    public final double getY() { return y; }
    public final double getSize() { return size; }
    public final Color getColor() { return color; }
    public abstract void render(PixelRenderer renderer);
}
