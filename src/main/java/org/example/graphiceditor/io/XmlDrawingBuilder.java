package org.example.graphiceditor.io;

import javafx.scene.paint.Color;
import org.example.graphiceditor.model.Circle;
import org.example.graphiceditor.model.DrawingModel;
import org.example.graphiceditor.model.Pixel;
import org.example.graphiceditor.model.Point;
import org.example.graphiceditor.model.Square;
import org.example.graphiceditor.model.vector.BorderDecorator;
import org.example.graphiceditor.model.vector.FillDecorator;
import org.example.graphiceditor.model.vector.ShadowDecorator;
import org.example.graphiceditor.model.vector.Shape;
import org.example.graphiceditor.model.vector.ShapeDecorator;
import org.example.graphiceditor.model.vector.Vertex;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class XmlDrawingBuilder implements DrawingBuilder<String> {
    private final BoundedTextWriter output;
    private final Writer sink;
    private XMLStreamWriter writer;
    private DrawingModel bounds;
    private boolean polygons;
    private int totalVertices;

    XmlDrawingBuilder() { this(true); }

    XmlDrawingBuilder(boolean retainText) {
        output = retainText ? new BoundedTextWriter() : null;
        sink = retainText ? output : Writer.nullWriter();
    }

    @Override public void start(double width, double height, Color background) throws IOException {
        bounds = new DrawingModel(width, height);
        try {
            writer = XMLOutputFactory.newFactory().createXMLStreamWriter(sink);
            writer.writeStartDocument("UTF-8", "1.0");
            writer.writeStartElement("graphic-editor-project");
            writer.writeAttribute("version", "1");
            number("width", width);
            number("height", height);
            number("background-r", background.getRed());
            number("background-g", background.getGreen());
            number("background-b", background.getBlue());
            number("background-a", background.getOpacity());
            writer.writeStartElement("pixels");
        } catch (XMLStreamException exception) { throw invalid(exception); }
    }

    @Override public void pixel(Pixel pixel) throws IOException {
        if (polygons) throw new IOException("Растровые объекты должны предшествовать векторным");
        ProjectXmlCodec.coordinate(bounds, pixel.getX(), pixel.getY());
        ProjectXmlCodec.positive(pixel.getSize(), "размер мазка");
        try {
            writer.writeEmptyElement(pixelType(pixel));
            number("x", pixel.getX());
            number("y", pixel.getY());
            number("size", pixel.getSize());
            color(pixel.getColor());
        } catch (XMLStreamException exception) { throw invalid(exception); }
    }

    @Override public void shape(Shape shape) throws IOException {
        List<ShapeDecorator> decorators = decorators(shape);
        totalVertices += shape.getVertices().size();
        ProjectXmlCodec.vertexCount(shape.getVertices().size(), totalVertices);
        try {
            beginPolygons();
            writer.writeStartElement("polygon");
            writer.writeStartElement("vertices");
            for (Vertex vertex : shape.getVertices()) {
                ProjectXmlCodec.coordinate(bounds, vertex.x(), vertex.y());
                writer.writeEmptyElement("vertex");
                number("x", vertex.x());
                number("y", vertex.y());
            }
            writer.writeEndElement();
            writer.writeStartElement("decorators");
            for (ShapeDecorator decorator : decorators) {
                switch (decorator) {
                    case FillDecorator fill -> {
                        writer.writeEmptyElement("fill");
                        color(fill.getColor());
                    }
                    case BorderDecorator border -> {
                        ProjectXmlCodec.positive(border.getWidth(), "толщина контура");
                        writer.writeEmptyElement("border");
                        color(border.getColor());
                        number("width", border.getWidth());
                    }
                    case ShadowDecorator shadow -> {
                        ProjectXmlCodec.offset(shadow.getOffsetX());
                        ProjectXmlCodec.offset(shadow.getOffsetY());
                        writer.writeEmptyElement("shadow");
                        color(shadow.getColor());
                        number("offset-x", shadow.getOffsetX());
                        number("offset-y", shadow.getOffsetY());
                        number("blur", shadow.getBlur());
                    }
                    default -> throw new IOException("Неизвестный декоратор");
                }
            }
            writer.writeEndElement();
            writer.writeEndElement();
        } catch (XMLStreamException exception) { throw invalid(exception); }
    }

    @Override public String finish() throws IOException {
        try {
            beginPolygons();
            writer.writeEndElement();
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.close();
            return output == null ? "" : output.toString();
        } catch (XMLStreamException exception) { throw invalid(exception); }
    }

    static String pixelType(Pixel pixel) throws IOException {
        return switch (pixel) {
            case Point ignored -> "point";
            case Circle ignored -> "circle";
            case Square ignored -> "square";
            default -> throw new IOException("Неизвестный тип растрового объекта");
        };
    }

    static List<ShapeDecorator> decorators(Shape shape) throws IOException {
        List<ShapeDecorator> decorators = new ArrayList<>();
        while (shape instanceof ShapeDecorator decorator) {
            if (!(decorator instanceof FillDecorator || decorator instanceof BorderDecorator || decorator instanceof ShadowDecorator)
                    || decorators.size() >= 8) throw new IOException("Неизвестные декораторы фигуры");
            decorators.add(decorator);
            shape = decorator.getWrapped();
        }
        if (!(shape instanceof org.example.graphiceditor.model.vector.Polygon)) throw new IOException("Неизвестная геометрия фигуры");
        Collections.reverse(decorators);
        return decorators;
    }

    private void beginPolygons() throws XMLStreamException {
        if (!polygons) {
            writer.writeEndElement();
            writer.writeStartElement("polygons");
            polygons = true;
        }
    }

    private void color(Color color) throws XMLStreamException {
        number("r", color.getRed()); number("g", color.getGreen()); number("b", color.getBlue()); number("a", color.getOpacity());
    }

    private void number(String name, double number) throws XMLStreamException { writer.writeAttribute(name, Double.toString(number)); }
    private static IOException invalid(XMLStreamException exception) { return new IOException("Не удалось записать XML проекта", exception); }
}
