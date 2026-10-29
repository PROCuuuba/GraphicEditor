package org.example.graphiceditor.io;

import javafx.scene.paint.Color;
import org.example.graphiceditor.model.Circle;
import org.example.graphiceditor.model.DrawingModel;
import org.example.graphiceditor.model.Pixel;
import org.example.graphiceditor.model.Point;
import org.example.graphiceditor.model.vector.Polygon;
import org.example.graphiceditor.model.vector.Shape;
import org.example.graphiceditor.model.vector.Vertex;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DrawingExporterTest {
    @Test
    void oneDirectorTraversesRasterLayerThenVectorLayerOneObjectAtATime() throws IOException {
        DrawingModel model = new DrawingModel(100, 80);
        model.setBackgroundColor(Color.BEIGE);
        Point point = new Point(1, 2, 1, Color.RED);
        Circle circle = new Circle(10, 20, 6, Color.BLUE);
        Polygon triangle = new Polygon(List.of(new Vertex(10, 10), new Vertex(50, 10), new Vertex(10, 50)));
        model.add(point); model.add(circle); model.addShape(triangle);
        List<Object> events = new ArrayList<>();
        DrawingBuilder<Integer> builder = new DrawingBuilder<>() {
            @Override public void start(double width, double height, Color background) {
                assertEquals(100, width); assertEquals(80, height); assertEquals(Color.BEIGE, background);
                events.add("start");
            }
            @Override public void pixel(Pixel pixel) { events.add(pixel); }
            @Override public void shape(Shape shape) { events.add(shape); }
            @Override public Integer finish() { events.add("finish"); return events.size(); }
        };

        assertEquals(5, DrawingExporter.export(model, builder));

        assertEquals(List.of("start", point, circle, triangle, "finish"), events);
    }

    @Test
    void sameDrawingProducesUnrelatedJsonAndSvgProductsWithoutChangingTheDrawing() throws IOException {
        DrawingModel drawing = new DrawingModel(100, 80);
        drawing.add(new Circle(15.25, 25.75, 10, Color.RED));

        JsonFile json = DrawingExporter.export(drawing, new JsonDrawingBuilder());
        SvgFile svg = DrawingExporter.export(drawing, new SvgDrawingBuilder());

        assertFalse(JsonFile.class.isAssignableFrom(SvgFile.class));
        assertFalse(SvgFile.class.isAssignableFrom(JsonFile.class));
        assertTrue(json.content().contains("\"type\" : \"circle\""));
        assertTrue(svg.content().contains("<circle"));
        assertEquals(1, drawing.size());
        assertTrue(drawing.getShapes().isEmpty());
    }

    @Test
    void failingBuilderStopsTraversalImmediately() {
        DrawingModel model = new DrawingModel(100, 80);
        model.add(new Point(1, 2, 1, Color.RED));
        model.add(new Point(3, 4, 1, Color.BLUE));
        List<String> events = new ArrayList<>();
        DrawingBuilder<Void> builder = new DrawingBuilder<>() {
            @Override public void start(double width, double height, Color background) { events.add("start"); }
            @Override public void pixel(Pixel pixel) throws IOException { events.add("pixel"); throw new IOException("disk full"); }
            @Override public void shape(Shape shape) { fail("should stop"); }
            @Override public Void finish() { fail("should stop"); return null; }
        };

        assertThrows(IOException.class, () -> DrawingExporter.export(model, builder));
        assertEquals(List.of("start", "pixel"), events);
    }
}
