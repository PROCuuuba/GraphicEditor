package org.example.graphiceditor.factory;

import javafx.scene.paint.Color;
import org.example.graphiceditor.model.Point;
import org.example.graphiceditor.model.Pixel;

public final class PointFactory extends PixelFactory {
    @Override
    public Pixel create(double x, double y, double size, Color color) {
        return new Point(x, y, size, color);
    }
}
