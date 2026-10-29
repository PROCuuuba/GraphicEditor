package org.example.graphiceditor.io;

import javafx.scene.paint.Color;
import org.example.graphiceditor.model.Circle;
import org.example.graphiceditor.model.Pixel;
import org.example.graphiceditor.model.Point;
import org.example.graphiceditor.model.Square;
import org.example.graphiceditor.model.vector.Shape;
import org.example.graphiceditor.model.vector.VectorStyle;
import org.example.graphiceditor.model.vector.Vertex;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.util.List;

public final class SvgDrawingBuilder implements DrawingBuilder<SvgFile> {
    static final String SVG_NAMESPACE = "http://www.w3.org/2000/svg";
    private final BoundedTextWriter output = new BoundedTextWriter();
    private final XmlDrawingBuilder project = new XmlDrawingBuilder();
    private XMLStreamWriter svg;
    private int polygonIndex;

    @Override public void start(double width, double height, Color background) throws IOException {
        project.start(width, height, background);
        try {
            svg = XMLOutputFactory.newFactory().createXMLStreamWriter(output);
            svg.writeStartDocument("UTF-8", "1.0");
            svg.writeStartElement("svg");
            svg.writeDefaultNamespace(SVG_NAMESPACE);
            svg.writeAttribute("version", "1.1");
            number("width", width); number("height", height);
            svg.writeAttribute("viewBox", "0 0 " + width + " " + height);
            svg.writeStartElement("rect");
            number("x", 0); number("y", 0); number("width", width); number("height", height);
            paint("fill", background);
            svg.writeEndElement();
        } catch (XMLStreamException exception) { throw invalid(exception); }
    }

    @Override public void pixel(Pixel pixel) throws IOException {
        project.pixel(pixel);
        try {
            switch (pixel) {
                case Point point -> {
                    svg.writeStartElement("rect");
                    number("x", Math.floor(point.getX())); number("y", Math.floor(point.getY()));
                    number("width", 1); number("height", 1);
                }
                case Circle circle -> {
                    svg.writeStartElement("circle");
                    number("cx", circle.getX()); number("cy", circle.getY()); number("r", circle.getSize() / 2);
                }
                case Square square -> {
                    svg.writeStartElement("rect");
                    number("x", square.getX() - square.getSize() / 2); number("y", square.getY() - square.getSize() / 2);
                    number("width", square.getSize()); number("height", square.getSize());
                }
                default -> throw new IOException("Неизвестный тип растрового объекта");
            }
            paint("fill", pixel.getColor());
            svg.writeEndElement();
        } catch (XMLStreamException exception) { throw invalid(exception); }
    }

    @Override public void shape(Shape shape) throws IOException {
        project.shape(shape);
        VectorStyle style = VectorStyle.from(shape);
        try {
            if (style.shadowEnabled()) {
                String filter = "shadow-" + polygonIndex;
                if (style.shadowBlur() > 0) {
                    svg.writeStartElement("defs"); svg.writeStartElement("filter");
                    svg.writeAttribute("id", filter);
                    svg.writeAttribute("filterUnits", "userSpaceOnUse");
                    number("x", -8192); number("y", -8192); number("width", 20480); number("height", 20480);
                    svg.writeAttribute("color-interpolation-filters", "sRGB");
                    svg.writeEmptyElement("feGaussianBlur"); number("stdDeviation", style.shadowBlur() / 3);
                    svg.writeEndElement(); svg.writeEndElement();
                }
                svg.writeStartElement("polygon");
                points(shape.getVertices(), style.shadowOffsetX(), style.shadowOffsetY());
                paint("fill", style.shadowColor());
                if (style.shadowBlur() > 0) svg.writeAttribute("filter", "url(#" + filter + ")");
                svg.writeEndElement();
            }
            svg.writeStartElement("polygon"); points(shape.getVertices(), 0, 0);
            if (style.fillEnabled()) paint("fill", style.fillColor()); else svg.writeAttribute("fill", "none");
            if (style.borderEnabled() || !style.fillEnabled()) {
                paint("stroke", style.borderEnabled() ? style.borderColor() : Color.GRAY);
                number("stroke-width", style.borderEnabled() ? style.borderWidth() : 1);
                svg.writeAttribute("stroke-linejoin", "miter");
            }
            svg.writeEndElement();
            polygonIndex++;
        } catch (XMLStreamException exception) { throw invalid(exception); }
    }

    @Override public SvgFile finish() throws IOException {
        String editableProject = project.finish();
        try {
            svg.writeStartElement("metadata");
            svg.writeAttribute("id", PngProjectIO.PROJECT_KEYWORD);
            svg.writeCharacters(editableProject);
            svg.writeEndElement(); svg.writeEndElement(); svg.writeEndDocument(); svg.close();
            return new SvgFile(output.toString());
        } catch (XMLStreamException exception) { throw invalid(exception); }
    }

    private void points(List<Vertex> vertices, double offsetX, double offsetY) throws XMLStreamException {
        StringBuilder points = new StringBuilder();
        for (Vertex vertex : vertices) {
            if (!points.isEmpty()) points.append(' ');
            points.append(vertex.x() + offsetX).append(',').append(vertex.y() + offsetY);
        }
        svg.writeAttribute("points", points.toString());
    }

    private void paint(String property, Color color) throws XMLStreamException {
        svg.writeAttribute(property, "rgb(" + (color.getRed() * 100) + "%," + (color.getGreen() * 100) + "%," + (color.getBlue() * 100) + "%)");
        number(property + "-opacity", color.getOpacity());
    }

    private void number(String name, double value) throws XMLStreamException { svg.writeAttribute(name, Double.toString(value)); }
    private static IOException invalid(XMLStreamException exception) { return new IOException("Не удалось записать SVG", exception); }
}
