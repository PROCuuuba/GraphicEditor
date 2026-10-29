package org.example.graphiceditor.io;

import javafx.scene.paint.Color;
import org.example.graphiceditor.model.Pixel;
import org.example.graphiceditor.model.vector.Shape;

import java.io.IOException;

public interface DrawingBuilder<R> {
    void start(double width, double height, Color background) throws IOException;
    void pixel(Pixel pixel) throws IOException;
    void shape(Shape shape) throws IOException;
    R finish() throws IOException;
}
