package org.example.graphiceditor.io;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import javafx.scene.paint.Color;
import org.example.graphiceditor.model.Pixel;
import org.example.graphiceditor.model.vector.BorderDecorator;
import org.example.graphiceditor.model.vector.FillDecorator;
import org.example.graphiceditor.model.vector.ShadowDecorator;
import org.example.graphiceditor.model.vector.Shape;
import org.example.graphiceditor.model.vector.ShapeDecorator;
import org.example.graphiceditor.model.vector.Vertex;

import java.io.IOException;

public final class JsonDrawingBuilder implements DrawingBuilder<JsonFile> {
    private final BoundedTextWriter output = new BoundedTextWriter();
    private final XmlDrawingBuilder validation = new XmlDrawingBuilder(false);
    private JsonGenerator json;

    @Override public void start(double width, double height, Color background) throws IOException {
        validation.start(width, height, background);
        json = new JsonFactory().createGenerator(output);
        json.useDefaultPrettyPrinter();
        json.writeStartObject();
        json.writeStringField("format", "GraphicEditor");
        json.writeNumberField("version", 1);
        json.writeNumberField("width", width);
        json.writeNumberField("height", height);
        color("background", background);
        json.writeArrayFieldStart("objects");
    }

    @Override public void pixel(Pixel pixel) throws IOException {
        validation.pixel(pixel);
        json.writeStartObject();
        json.writeStringField("type", XmlDrawingBuilder.pixelType(pixel));
        json.writeNumberField("x", pixel.getX());
        json.writeNumberField("y", pixel.getY());
        json.writeNumberField("size", pixel.getSize());
        color("color", pixel.getColor());
        json.writeEndObject();
    }

    @Override public void shape(Shape shape) throws IOException {
        validation.shape(shape);
        json.writeStartObject();
        json.writeStringField("type", "polygon");
        json.writeArrayFieldStart("vertices");
        for (Vertex vertex : shape.getVertices()) {
            json.writeStartArray(); json.writeNumber(vertex.x()); json.writeNumber(vertex.y()); json.writeEndArray();
        }
        json.writeEndArray();
        json.writeArrayFieldStart("decorators");
        for (ShapeDecorator decorator : XmlDrawingBuilder.decorators(shape)) {
            json.writeStartObject();
            switch (decorator) {
                case FillDecorator fill -> {
                    json.writeStringField("type", "fill"); color("color", fill.getColor());
                }
                case BorderDecorator border -> {
                    json.writeStringField("type", "border"); color("color", border.getColor());
                    json.writeNumberField("width", border.getWidth());
                }
                case ShadowDecorator shadow -> {
                    json.writeStringField("type", "shadow"); color("color", shadow.getColor());
                    json.writeNumberField("offsetX", shadow.getOffsetX()); json.writeNumberField("offsetY", shadow.getOffsetY());
                    json.writeNumberField("blur", shadow.getBlur());
                }
                default -> throw new IOException("Неизвестный декоратор");
            }
            json.writeEndObject();
        }
        json.writeEndArray();
        json.writeEndObject();
    }

    @Override public JsonFile finish() throws IOException {
        validation.finish();
        json.writeEndArray(); json.writeEndObject(); json.close();
        return new JsonFile(output.toString());
    }

    private void color(String name, Color color) throws IOException {
        json.writeArrayFieldStart(name);
        json.writeNumber(color.getRed()); json.writeNumber(color.getGreen());
        json.writeNumber(color.getBlue()); json.writeNumber(color.getOpacity()); json.writeEndArray();
    }
}
