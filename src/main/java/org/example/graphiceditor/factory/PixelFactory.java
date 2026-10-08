package org.example.graphiceditor.factory;

import javafx.scene.paint.Color;
import org.example.graphiceditor.model.Pixel;
import org.example.graphiceditor.model.Tool;

public abstract class PixelFactory {
    public abstract Pixel create(double x, double y, double size, Color color);

    public static PixelFactory forTool(Tool tool) {
        if (tool == null) {
            throw new IllegalArgumentException("Выберите инструмент");
        }
        return switch (tool) {
            case POINT -> new PointFactory();
            case CIRCLE -> new CircleFactory();
            case SQUARE -> new SquareFactory();
        };
    }
}
