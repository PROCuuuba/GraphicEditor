package org.example.graphiceditor.factory;

import javafx.scene.paint.Color;
import org.example.graphiceditor.model.Square;
import org.example.graphiceditor.model.Pixel;

public final class SquareFactory extends PixelFactory {
    @Override
    public Pixel create(double x, double y, double size, Color color) {
        return new Square(x, y, size, color);
    }
}
