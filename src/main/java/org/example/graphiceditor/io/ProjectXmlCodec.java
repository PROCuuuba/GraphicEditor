package org.example.graphiceditor.io;

import javafx.scene.paint.Color;
import org.example.graphiceditor.model.Circle;
import org.example.graphiceditor.model.DrawingModel;
import org.example.graphiceditor.model.Pixel;
import org.example.graphiceditor.model.Point;
import org.example.graphiceditor.model.Square;
import org.example.graphiceditor.model.vector.BorderDecorator;
import org.example.graphiceditor.model.vector.FillDecorator;
import org.example.graphiceditor.model.vector.Polygon;
import org.example.graphiceditor.model.vector.ShadowDecorator;
import org.example.graphiceditor.model.vector.Shape;
import org.example.graphiceditor.model.vector.ShapeDecorator;
import org.example.graphiceditor.model.vector.Vertex;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class ProjectXmlCodec {
    static final int MAX_TEXT_BYTES = 8 * 1024 * 1024;
    static final int MAX_SIDE = 4096;
    static final int MAX_PIXELS = 100_000;
    static final int MAX_POLYGONS = 10_000;
    static final int MAX_VERTICES = 100_000;
    private static final int MAX_POLYGON_VERTICES = 10_000;
    private static final int MAX_DECORATORS = 8;

    private ProjectXmlCodec() { }

    static String encode(DrawingModel model) throws IOException {
        return DrawingExporter.export(model, new XmlDrawingBuilder());
    }

    static DrawingModel decode(String text) throws IOException {
        if (text.getBytes(StandardCharsets.UTF_8).length > MAX_TEXT_BYTES) {
            throw invalid("данные проекта превышают 8 МиБ");
        }
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setXMLResolver((publicId, systemId, baseUri, namespace) -> {
            throw new XMLStreamException("Внешние XML-ресурсы запрещены");
        });
        XMLStreamReader reader = null;
        try {
            reader = factory.createXMLStreamReader(new StringReader(text));
            Parser parser = new Parser(reader);
            parser.start("graphic-editor-project");
            boolean hasBackground = reader.getAttributeValue(null, "background-r") != null;
            if (hasBackground) parser.attributes("version", "width", "height", "background-r", "background-g", "background-b", "background-a");
            else parser.attributes("version", "width", "height");
            if (!"1".equals(reader.getAttributeValue(null, "version"))) {
                throw invalid("неподдерживаемая версия проекта: " + reader.getAttributeValue(null, "version"));
            }
            double width = parser.number("width");
            double height = parser.number("height");
            dimensions(width, height);
            DrawingModel model = new DrawingModel(width, height);
            if (hasBackground) model.setBackgroundColor(new Color(parser.component("background-r"), parser.component("background-g"),
                    parser.component("background-b"), parser.component("background-a")));
            parser.start("pixels");
            parser.attributes();
            while (parser.tag() == XMLStreamConstants.START_ELEMENT) {
                String type = reader.getLocalName();
                if (!Set.of("point", "circle", "square").contains(type)) {
                    throw invalid("неизвестный тип мазка: " + type);
                }
                if (model.size() >= MAX_PIXELS) throw invalid("слишком много мазков");
                parser.attributes("x", "y", "size", "r", "g", "b", "a");
                double x = parser.number("x");
                double y = parser.number("y");
                double size = parser.number("size");
                coordinate(model, x, y);
                positive(size, "размер мазка");
                if (type.equals("point") && size != 1) throw invalid("размер точки должен быть равен 1");
                Color color = parser.color();
                Pixel pixel = switch (type) {
                    case "point" -> new Point(x, y, size, color);
                    case "circle" -> new Circle(x, y, size, color);
                    default -> new Square(x, y, size, color);
                };
                model.add(pixel);
                parser.end(type);
            }
            parser.currentEnd("pixels");
            parser.start("polygons");
            parser.attributes();
            int totalVertices = 0;
            while (parser.tag() == XMLStreamConstants.START_ELEMENT) {
                parser.currentStart("polygon");
                parser.attributes();
                if (model.getShapes().size() >= MAX_POLYGONS) throw invalid("слишком много фигур");
                parser.start("vertices");
                parser.attributes();
                List<Vertex> vertices = new ArrayList<>();
                while (parser.tag() == XMLStreamConstants.START_ELEMENT) {
                    parser.currentStart("vertex");
                    parser.attributes("x", "y");
                    double x = parser.number("x");
                    double y = parser.number("y");
                    coordinate(model, x, y);
                    vertices.add(new Vertex(x, y));
                    totalVertices++;
                    if (vertices.size() > MAX_POLYGON_VERTICES || totalVertices > MAX_VERTICES) {
                        throw invalid("слишком много вершин");
                    }
                    parser.end("vertex");
                }
                parser.currentEnd("vertices");
                vertexCount(vertices.size(), totalVertices);
                Shape shape = new Polygon(vertices);
                parser.start("decorators");
                parser.attributes();
                int decorators = 0;
                while (parser.tag() == XMLStreamConstants.START_ELEMENT) {
                    if (++decorators > MAX_DECORATORS) throw invalid("слишком много декораторов");
                    String type = reader.getLocalName();
                    shape = switch (type) {
                        case "fill" -> {
                            parser.attributes("r", "g", "b", "a");
                            yield new FillDecorator(shape, parser.color());
                        }
                        case "border" -> {
                            parser.attributes("r", "g", "b", "a", "width");
                            double borderWidth = parser.number("width");
                            positive(borderWidth, "толщина контура");
                            yield new BorderDecorator(shape, parser.color(), borderWidth);
                        }
                        case "shadow" -> {
                            parser.attributes("r", "g", "b", "a", "offset-x", "offset-y", "blur");
                            double offsetX = parser.number("offset-x");
                            double offsetY = parser.number("offset-y");
                            offset(offsetX);
                            offset(offsetY);
                            yield new ShadowDecorator(shape, parser.color(), offsetX, offsetY, parser.number("blur"));
                        }
                        default -> throw invalid("неизвестный декоратор: " + type);
                    };
                    parser.end(type);
                }
                parser.currentEnd("decorators");
                parser.end("polygon");
                model.addShape(shape);
            }
            parser.currentEnd("polygons");
            parser.end("graphic-editor-project");
            if (parser.tag() != XMLStreamConstants.END_DOCUMENT) throw invalid("лишние XML-данные");
            return model;
        } catch (XMLStreamException | IllegalArgumentException exception) {
            throw invalid(exception.getMessage(), exception);
        } finally {
            if (reader != null) {
                try { reader.close(); } catch (XMLStreamException ignored) { }
            }
        }
    }

    static void dimensions(double width, double height) throws IOException {
        if (!Double.isFinite(width) || !Double.isFinite(height) || width <= 0 || height <= 0
                || width > MAX_SIDE || height > MAX_SIDE || Math.ceil(width) * Math.ceil(height) > 16_777_216) {
            throw invalid("размер холста должен быть от 1 до 4096 пикселей по каждой стороне");
        }
    }

    static void coordinate(DrawingModel model, double x, double y) throws IOException {
        if (!model.contains(x, y)) throw invalid("координата находится за пределами холста");
    }

    static void positive(double value, String name) throws IOException {
        if (!Double.isFinite(value) || value <= 0 || value > MAX_SIDE * 2) {
            throw invalid(name + " должен быть положительным и не превышать 8192");
        }
    }

    static void offset(double value) throws IOException {
        if (!Double.isFinite(value) || Math.abs(value) > MAX_SIDE * 2) throw invalid("слишком большое смещение тени");
    }

    static void vertexCount(int count, int total) throws IOException {
        if (count < 3 || count > MAX_POLYGON_VERTICES || total > MAX_VERTICES) {
            throw invalid("недопустимое количество вершин");
        }
    }

    private static IOException invalid(String detail) { return invalid(detail, null); }

    private static IOException invalid(String detail, Throwable cause) {
        return new IOException("Некорректные данные проекта: " + detail, cause);
    }

    private record Parser(XMLStreamReader reader) {
        int tag() throws XMLStreamException, IOException {
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT || event == XMLStreamConstants.END_ELEMENT
                        || event == XMLStreamConstants.END_DOCUMENT) return event;
                if (event == XMLStreamConstants.COMMENT || event == XMLStreamConstants.START_DOCUMENT) continue;
                if ((event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.SPACE)
                        && reader.isWhiteSpace()) continue;
                throw invalid("неожиданное содержимое XML; DTD и сущности запрещены");
            }
            return XMLStreamConstants.END_DOCUMENT;
        }

        void start(String name) throws XMLStreamException, IOException {
            tag();
            currentStart(name);
        }

        void end(String name) throws XMLStreamException, IOException {
            tag();
            currentEnd(name);
        }

        void currentStart(String name) throws IOException {
            String namespace = reader.getEventType() == XMLStreamConstants.START_ELEMENT ? reader.getNamespaceURI() : null;
            if (reader.getEventType() != XMLStreamConstants.START_ELEMENT || !reader.getLocalName().equals(name)
                    || (namespace != null && !namespace.isEmpty())) throw invalid("ожидался элемент " + name);
        }

        void currentEnd(String name) throws IOException {
            if (reader.getEventType() != XMLStreamConstants.END_ELEMENT || !reader.getLocalName().equals(name)) {
                throw invalid("ожидалось завершение элемента " + name);
            }
        }

        void attributes(String... names) throws IOException {
            Set<String> expected = Set.of(names);
            if (reader.getAttributeCount() != names.length || reader.getNamespaceCount() != 0) {
                throw invalid("недопустимые атрибуты элемента " + reader.getLocalName());
            }
            for (int index = 0; index < reader.getAttributeCount(); index++) {
                String namespace = reader.getAttributeNamespace(index);
                if (!expected.contains(reader.getAttributeLocalName(index)) || (namespace != null && !namespace.isEmpty())) {
                    throw invalid("неизвестный XML-атрибут");
                }
            }
        }

        double number(String name) throws IOException {
            double value;
            try { value = Double.parseDouble(reader.getAttributeValue(null, name)); }
            catch (NumberFormatException exception) { throw invalid("недопустимое число " + name, exception); }
            if (!Double.isFinite(value)) throw invalid("число " + name + " должно быть конечным");
            return value;
        }

        Color color() throws IOException {
            return new Color(component("r"), component("g"), component("b"), component("a"));
        }

        private double component(String name) throws IOException {
            double value = number(name);
            if (value < 0 || value > 1) throw invalid("компонент цвета должен быть от 0 до 1");
            return value;
        }
    }
}
