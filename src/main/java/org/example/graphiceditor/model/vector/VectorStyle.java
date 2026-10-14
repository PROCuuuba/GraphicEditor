package org.example.graphiceditor.model.vector;

import javafx.scene.paint.Color;

import java.util.Objects;

public record VectorStyle(boolean fillEnabled, Color fillColor,
                          boolean borderEnabled, Color borderColor, double borderWidth,
                          boolean shadowEnabled, Color shadowColor,
                          double shadowOffsetX, double shadowOffsetY, double shadowBlur) {
    public VectorStyle {
        Objects.requireNonNull(fillColor, "fillColor");
        Objects.requireNonNull(borderColor, "borderColor");
        Objects.requireNonNull(shadowColor, "shadowColor");
        if (!Double.isFinite(borderWidth) || borderWidth <= 0) {
            throw new IllegalArgumentException("Толщина контура должна быть конечной и положительной");
        }
        if (!Double.isFinite(shadowOffsetX) || !Double.isFinite(shadowOffsetY)) {
            throw new IllegalArgumentException("Смещение тени должно быть конечным");
        }
        if (!Double.isFinite(shadowBlur) || shadowBlur < 0 || shadowBlur > 50) {
            throw new IllegalArgumentException("Размытие тени должно быть от 0 до 50");
        }
    }

    public static VectorStyle defaults() {
        return new VectorStyle(false, Color.CORNFLOWERBLUE,
                true, Color.BLACK, 2,
                false, Color.color(0, 0, 0, 0.35), 8, 8, 5);
    }

    public Shape decorate(Shape geometry) {
        Shape decorated = Objects.requireNonNull(geometry, "geometry");
        if (fillEnabled) {
            decorated = new FillDecorator(decorated, fillColor);
        }
        if (borderEnabled) {
            decorated = new BorderDecorator(decorated, borderColor, borderWidth);
        }
        if (shadowEnabled) {
            decorated = new ShadowDecorator(decorated, shadowColor,
                    shadowOffsetX, shadowOffsetY, shadowBlur);
        }
        return decorated;
    }

    public static VectorStyle from(Shape decorated) {
        Objects.requireNonNull(decorated, "decorated");
        VectorStyle defaults = defaults();
        boolean fillEnabled = false;
        Color fillColor = defaults.fillColor;
        boolean borderEnabled = false;
        Color borderColor = defaults.borderColor;
        double borderWidth = defaults.borderWidth;
        boolean shadowEnabled = false;
        Color shadowColor = defaults.shadowColor;
        double offsetX = defaults.shadowOffsetX;
        double offsetY = defaults.shadowOffsetY;
        double blur = defaults.shadowBlur;
        Shape current = decorated;
        while (current instanceof ShapeDecorator decorator) {
            if (decorator instanceof FillDecorator fill) {
                fillEnabled = true;
                fillColor = fill.getColor();
            } else if (decorator instanceof BorderDecorator border) {
                borderEnabled = true;
                borderColor = border.getColor();
                borderWidth = border.getWidth();
            } else if (decorator instanceof ShadowDecorator shadow) {
                shadowEnabled = true;
                shadowColor = shadow.getColor();
                offsetX = shadow.getOffsetX();
                offsetY = shadow.getOffsetY();
                blur = shadow.getBlur();
            }
            current = decorator.getWrapped();
        }
        return new VectorStyle(fillEnabled, fillColor, borderEnabled, borderColor, borderWidth,
                shadowEnabled, shadowColor, offsetX, offsetY, blur);
    }

    public static Polygon geometryOf(Shape decorated) {
        Shape current = Objects.requireNonNull(decorated, "decorated");
        while (current instanceof ShapeDecorator decorator) {
            current = decorator.getWrapped();
        }
        if (current instanceof Polygon polygon) {
            return polygon;
        }
        throw new IllegalArgumentException("Фигура должна содержать полигон");
    }
}
