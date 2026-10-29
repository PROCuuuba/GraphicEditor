package org.example.graphiceditor.io;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamReadFeature;
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
import org.example.graphiceditor.model.vector.Vertex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class JsonProjectReader {
    private final JsonParser json;
    private int totalVertices;

    private JsonProjectReader(JsonParser json) { this.json = json; }

    static DrawingModel read(String text) throws IOException {
        JsonFactory factory = JsonFactory.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(16).maxStringLength(128).maxNumberLength(32).build()).build();
        try (JsonParser json = factory.createParser(text)) {
            return new JsonProjectReader(json).drawing();
        } catch (IllegalArgumentException | ClassCastException exception) {
            throw new IOException("Некорректные данные JSON-проекта", exception);
        }
    }

    private DrawingModel drawing() throws IOException {
        expect(json.nextToken(), JsonToken.START_OBJECT);
        Map<String, Object> header = new HashMap<>();
        List<Pixel> pixels = new ArrayList<>();
        List<Shape> shapes = new ArrayList<>();
        while (json.nextToken() != JsonToken.END_OBJECT) {
            expect(json.currentToken(), JsonToken.FIELD_NAME);
            String name = json.currentName();
            json.nextToken();
            Object value = switch (name) {
                case "format" -> string();
                case "version", "width", "height" -> number();
                case "background" -> color();
                case "objects" -> {
                    expect(json.currentToken(), JsonToken.START_ARRAY);
                    boolean vectors = false;
                    while (json.nextToken() != JsonToken.END_ARRAY) {
                        Map<String, Object> object = object(false);
                        String type = (String) object.get("type");
                        if ("polygon".equals(type)) {
                            vectors = true;
                            if (shapes.size() >= ProjectXmlCodec.MAX_POLYGONS) throw invalid("слишком много полигонов");
                            shapes.add(polygon(object));
                        } else {
                            if (vectors) throw invalid("растровые объекты должны предшествовать полигонам");
                            if (pixels.size() >= ProjectXmlCodec.MAX_PIXELS) throw invalid("слишком много мазков");
                            pixels.add(pixel(object));
                        }
                    }
                    yield Boolean.TRUE;
                }
                default -> throw invalid("неизвестное поле " + name);
            };
            header.put(name, value);
        }
        fields(header, "format", "version", "width", "height", "background", "objects");
        if (!"GraphicEditor".equals(header.get("format"))) throw invalid("неизвестный формат");
        if (number(header, "version") != 1) throw invalid("неподдерживаемая версия");
        double width = number(header, "width");
        double height = number(header, "height");
        ProjectXmlCodec.dimensions(width, height);
        DrawingModel model = new DrawingModel(width, height);
        model.setBackgroundColor((Color) header.get("background"));
        for (Pixel pixel : pixels) if (!model.add(pixel)) throw invalid("мазок вне холста");
        for (Shape shape : shapes) if (!model.addShape(shape)) throw invalid("вершины вне холста");
        if (json.nextToken() != null) throw invalid("лишние данные после проекта");
        ProjectXmlCodec.encode(model);
        return model;
    }

    private Map<String, Object> object(boolean decorator) throws IOException {
        expect(json.currentToken(), JsonToken.START_OBJECT);
        Map<String, Object> values = new HashMap<>();
        while (json.nextToken() != JsonToken.END_OBJECT) {
            expect(json.currentToken(), JsonToken.FIELD_NAME);
            String name = json.currentName();
            json.nextToken();
            Object value = switch (name) {
                case "type" -> string();
                case "x", "y", "size", "width", "offsetX", "offsetY", "blur" -> number();
                case "color" -> color();
                case "vertices" -> {
                    if (decorator) throw invalid("неожиданные вершины декоратора");
                    expect(json.currentToken(), JsonToken.START_ARRAY);
                    List<Vertex> vertices = new ArrayList<>();
                    while (json.nextToken() != JsonToken.END_ARRAY) {
                        expect(json.currentToken(), JsonToken.START_ARRAY);
                        json.nextToken(); double x = number();
                        json.nextToken(); double y = number();
                        expect(json.nextToken(), JsonToken.END_ARRAY);
                        if (vertices.size() >= 10_000 || ++totalVertices > ProjectXmlCodec.MAX_VERTICES) throw invalid("слишком много вершин");
                        vertices.add(new Vertex(x, y));
                    }
                    yield vertices;
                }
                case "decorators" -> {
                    if (decorator) throw invalid("вложенные декораторы запрещены");
                    expect(json.currentToken(), JsonToken.START_ARRAY);
                    List<Map<String, Object>> decorators = new ArrayList<>();
                    while (json.nextToken() != JsonToken.END_ARRAY) {
                        if (decorators.size() >= 8) throw invalid("слишком много декораторов");
                        decorators.add(object(true));
                    }
                    yield decorators;
                }
                default -> throw invalid("неизвестное свойство " + name);
            };
            values.put(name, value);
        }
        if (!values.containsKey("type")) throw invalid("не указан тип объекта");
        return values;
    }

    private Pixel pixel(Map<String, Object> values) throws IOException {
        fields(values, "type", "x", "y", "size", "color");
        double x = number(values, "x"), y = number(values, "y"), size = number(values, "size");
        ProjectXmlCodec.positive(size, "размер мазка");
        Color color = (Color) values.get("color");
        return switch ((String) values.get("type")) {
            case "point" -> {
                if (size != 1) throw invalid("размер точки должен быть 1");
                yield new Point(x, y, size, color);
            }
            case "circle" -> new Circle(x, y, size, color);
            case "square" -> new Square(x, y, size, color);
            default -> throw invalid("неизвестный тип объекта");
        };
    }

    @SuppressWarnings("unchecked")
    private Shape polygon(Map<String, Object> values) throws IOException {
        fields(values, "type", "vertices", "decorators");
        Shape shape = new Polygon((List<Vertex>) values.get("vertices"));
        for (Map<String, Object> decorator : (List<Map<String, Object>>) values.get("decorators")) {
            String type = (String) decorator.get("type");
            shape = switch (type) {
                case "fill" -> {
                    fields(decorator, "type", "color");
                    yield new FillDecorator(shape, (Color) decorator.get("color"));
                }
                case "border" -> {
                    fields(decorator, "type", "color", "width");
                    yield new BorderDecorator(shape, (Color) decorator.get("color"), number(decorator, "width"));
                }
                case "shadow" -> {
                    fields(decorator, "type", "color", "offsetX", "offsetY", "blur");
                    yield new ShadowDecorator(shape, (Color) decorator.get("color"), number(decorator, "offsetX"),
                            number(decorator, "offsetY"), number(decorator, "blur"));
                }
                default -> throw invalid("неизвестный декоратор");
            };
        }
        return shape;
    }

    private Color color() throws IOException {
        expect(json.currentToken(), JsonToken.START_ARRAY);
        double[] rgba = new double[4];
        for (int index = 0; index < 4; index++) {
            json.nextToken(); rgba[index] = number();
            if (rgba[index] < 0 || rgba[index] > 1) throw invalid("компонент цвета вне диапазона 0..1");
        }
        expect(json.nextToken(), JsonToken.END_ARRAY);
        return new Color(rgba[0], rgba[1], rgba[2], rgba[3]);
    }

    private double number() throws IOException {
        if (json.currentToken() != JsonToken.VALUE_NUMBER_FLOAT && json.currentToken() != JsonToken.VALUE_NUMBER_INT) throw invalid("ожидалось число");
        double value = json.getDoubleValue();
        if (!Double.isFinite(value)) throw invalid("число должно быть конечным");
        return value;
    }

    private String string() throws IOException { expect(json.currentToken(), JsonToken.VALUE_STRING); return json.getText(); }
    private static double number(Map<String, Object> values, String name) throws IOException {
        if (!(values.get(name) instanceof Double value)) throw invalid("ожидалось число " + name);
        return value;
    }
    private static void fields(Map<String, Object> values, String... fields) throws IOException {
        if (!values.keySet().equals(Set.of(fields))) throw invalid("неверный набор полей объекта");
    }
    private static void expect(JsonToken actual, JsonToken expected) throws IOException {
        if (actual != expected) throw invalid("ожидался элемент " + expected);
    }
    private static IOException invalid(String detail) { return new IOException("Некорректный JSON-проект: " + detail); }
}
