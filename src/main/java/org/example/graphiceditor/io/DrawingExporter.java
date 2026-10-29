package org.example.graphiceditor.io;

import org.example.graphiceditor.model.Drawing;
import org.example.graphiceditor.model.Pixel;
import org.example.graphiceditor.model.vector.Shape;

import java.io.IOException;
import java.util.Objects;

public final class DrawingExporter {
    private DrawingExporter() { }

    public static <R> R export(Drawing drawing, DrawingBuilder<R> builder) throws IOException {
        Objects.requireNonNull(drawing, "drawing");
        Objects.requireNonNull(builder, "builder");
        ProjectXmlCodec.dimensions(drawing.getWidth(), drawing.getHeight());
        if (drawing.size() > ProjectXmlCodec.MAX_PIXELS || drawing.getShapes().size() > ProjectXmlCodec.MAX_POLYGONS) {
            throw new IOException("Слишком много объектов рисунка");
        }
        builder.start(drawing.getWidth(), drawing.getHeight(), drawing.getBackgroundColor());
        for (Pixel pixel : drawing.getPixels()) builder.pixel(pixel);
        for (Shape shape : drawing.getShapes()) builder.shape(shape);
        return builder.finish();
    }
}
