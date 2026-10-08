package org.example.graphiceditor.factory;

import javafx.scene.paint.Color;
import org.example.graphiceditor.model.Circle;
import org.example.graphiceditor.model.Pixel;

public final class CircleFactory extends PixelFactory {
    @Override
    public Pixel create(double x, double y, double size, Color color) {
        return new Circle(x, y, size, color);
    }
}
