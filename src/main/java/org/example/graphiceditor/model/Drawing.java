package org.example.graphiceditor.model;

import org.example.graphiceditor.model.vector.Shape;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Drawing {
    private Color backgroundColor = Color.WHITE;
    private final double width;
    private final double height;
    private final List<Pixel> pixels = new ArrayList<>();
    private final List<Pixel> view = Collections.unmodifiableList(pixels);
    private final List<Shape> shapes = new ArrayList<>();
    private final List<Shape> shapeView = Collections.unmodifiableList(shapes);

    public Drawing(double width, double height) {
        if (!Double.isFinite(width) || !Double.isFinite(height) || width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Размеры холста должны быть конечными и положительными");
        }
        this.width = width;
        this.height = height;
    }

    public boolean contains(double x, double y) {
        return Double.isFinite(x) && Double.isFinite(y)
                && x >= 0 && y >= 0 && x < width && y < height;
    }

    public boolean add(Pixel pixel) {
        Objects.requireNonNull(pixel, "pixel");
        if (!contains(pixel.getX(), pixel.getY())) {
            return false;
        }
        pixels.add(pixel);
        return true;
    }

    public boolean addShape(Shape shape) {
        Objects.requireNonNull(shape, "shape");
        if (!shape.getVertices().stream().allMatch(vertex -> contains(vertex.x(), vertex.y()))) {
            return false;
        }
        shapes.add(shape);
        return true;
    }

    public List<Shape> getShapes() { return shapeView; }
    public Shape getShape(int index) { return shapes.get(index); }

    public void replaceShape(int index, Shape shape) {
        Objects.requireNonNull(shape, "shape");
        if (!shape.getVertices().stream().allMatch(vertex -> contains(vertex.x(), vertex.y()))) {
            throw new IllegalArgumentException("Вершины фигуры должны находиться внутри холста");
        }
        shapes.set(index, shape);
    }

    public void removeShape(int index) { shapes.remove(index); }
    public void removePixel(int index) { pixels.remove(index); }

    public void replacePixel(int index, Pixel pixel) {
        Objects.requireNonNull(pixel, "pixel");
        if (!contains(pixel.getX(), pixel.getY())) throw new IllegalArgumentException("Элемент находится вне холста");
        pixels.set(index, pixel);
    }

    public List<Object> getObjects() {
        List<Object> objects = new ArrayList<>(pixels);
        objects.addAll(shapes);
        return List.copyOf(objects);
    }

    public Color getBackgroundColor() { return backgroundColor; }
    public void setBackgroundColor(Color color) { backgroundColor = Objects.requireNonNull(color, "color"); }

    public Snapshot snapshot() { return new Snapshot(pixels, shapes, backgroundColor); }

    public void restore(Snapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (snapshot.pixels().stream().anyMatch(pixel -> !contains(pixel.getX(), pixel.getY()))
                || snapshot.shapes().stream().flatMap(shape -> shape.getVertices().stream())
                .anyMatch(vertex -> !contains(vertex.x(), vertex.y()))) {
            throw new IllegalArgumentException("Объекты снимка находятся вне холста");
        }
        pixels.clear();
        pixels.addAll(snapshot.pixels());
        shapes.clear();
        shapes.addAll(snapshot.shapes());
        backgroundColor = snapshot.backgroundColor();
    }

    public record Snapshot(List<Pixel> pixels, List<Shape> shapes, Color backgroundColor) {
        public Snapshot {
            pixels = List.copyOf(pixels);
            shapes = List.copyOf(shapes);
            Objects.requireNonNull(backgroundColor, "backgroundColor");
        }
    }

    public List<Pixel> getPixels() { return view; }
    public int size() { return pixels.size(); }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public void clear() {
        pixels.clear();
        shapes.clear();
    }
}
