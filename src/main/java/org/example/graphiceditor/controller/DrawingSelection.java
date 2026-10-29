package org.example.graphiceditor.controller;

import org.example.graphiceditor.model.Circle;
import org.example.graphiceditor.model.Drawing;
import org.example.graphiceditor.model.Pixel;
import org.example.graphiceditor.model.Point;
import org.example.graphiceditor.model.Square;
import org.example.graphiceditor.model.vector.BorderDecorator;
import org.example.graphiceditor.model.vector.FillDecorator;
import org.example.graphiceditor.model.vector.Polygon;
import org.example.graphiceditor.model.vector.ShadowDecorator;
import org.example.graphiceditor.model.vector.Shape;
import org.example.graphiceditor.model.vector.VectorStyle;
import org.example.graphiceditor.model.vector.Vertex;

import java.util.List;
import java.util.Objects;

public final class DrawingSelection {
    private final Drawing drawing;
    private Object selected;

    public DrawingSelection(Drawing drawing) { this.drawing = Objects.requireNonNull(drawing, "drawing"); }

    public boolean selectAt(double x, double y, double tolerance) {
        if (!Double.isFinite(tolerance) || tolerance < 0) throw new IllegalArgumentException("Недопустимый радиус выбора");
        clear();
        if (!drawing.contains(x, y)) return false;
        List<Shape> shapes = drawing.getShapes();
        for (int index = shapes.size() - 1; index >= 0; index--) {
            VectorStyle style = VectorStyle.from(shapes.get(index));
            double hitRadius = style.borderEnabled() ? Math.max(tolerance, style.borderWidth() / 2) : tolerance;
            if (contains(shapes.get(index).getVertices(), x, y, hitRadius)) {
                selected = shapes.get(index);
                return true;
            }
        }
        List<Pixel> pixels = drawing.getPixels();
        for (int index = pixels.size() - 1; index >= 0; index--) {
            Pixel pixel = pixels.get(index);
            double half = pixel.getSize() / 2;
            boolean hit = pixel instanceof Circle
                    ? Math.hypot(x - pixel.getX(), y - pixel.getY()) <= half + tolerance
                    : pixel instanceof Point
                    ? x >= Math.floor(pixel.getX()) - tolerance && x <= Math.floor(pixel.getX()) + 1 + tolerance
                        && y >= Math.floor(pixel.getY()) - tolerance && y <= Math.floor(pixel.getY()) + 1 + tolerance
                    : Math.abs(x - pixel.getX()) <= half + tolerance && Math.abs(y - pixel.getY()) <= half + tolerance;
            if (hit) {
                selected = pixel;
                return true;
            }
        }
        return false;
    }

    private static boolean contains(List<Vertex> vertices, double x, double y, double tolerance) {
        boolean inside = false;
        for (int index = 0, previous = vertices.size() - 1; index < vertices.size(); previous = index++) {
            Vertex a = vertices.get(previous);
            Vertex b = vertices.get(index);
            double dx = b.x() - a.x();
            double dy = b.y() - a.y();
            double length = dx * dx + dy * dy;
            double position = length == 0 ? 0 : Math.max(0, Math.min(1, ((x - a.x()) * dx + (y - a.y()) * dy) / length));
            if (Math.hypot(x - a.x() - position * dx, y - a.y() - position * dy) <= tolerance) return true;
            if ((a.y() > y) != (b.y() > y) && x < (b.x() - a.x()) * (y - a.y()) / (b.y() - a.y()) + a.x()) inside = !inside;
        }
        return inside;
    }

    public void selectPixel(int index) { selected = index < 0 ? null : drawing.getPixels().get(index); }
    public void selectShape(int index) { selected = index < 0 ? null : drawing.getShape(index); }
    public void clear() { selected = null; }
    public int getSelectedPixelIndex() { return selected instanceof Pixel ? drawing.getPixels().indexOf(selected) : -1; }
    public int getSelectedShapeIndex() { return selected instanceof Shape ? drawing.getShapes().indexOf(selected) : -1; }
    public Object getSelectedObject() {
        if (getSelectedPixelIndex() < 0 && getSelectedShapeIndex() < 0) clear();
        return selected;
    }

    public boolean removeSelected() {
        int pixel = getSelectedPixelIndex();
        int shape = getSelectedShapeIndex();
        if (pixel >= 0) drawing.removePixel(pixel);
        else if (shape >= 0) drawing.removeShape(shape);
        else return false;
        clear();
        return true;
    }

    public boolean moveSelected(double dx, double dy) {
        if (!Double.isFinite(dx) || !Double.isFinite(dy)) throw new IllegalArgumentException("Смещение должно быть конечным");
        Object object = getSelectedObject();
        if (object == null) return false;
        List<Vertex> vertices = object instanceof Shape shape ? shape.getVertices()
                : List.of(new Vertex(((Pixel) object).getX(), ((Pixel) object).getY()));
        double minX = vertices.stream().mapToDouble(Vertex::x).min().orElseThrow();
        double maxX = vertices.stream().mapToDouble(Vertex::x).max().orElseThrow();
        double minY = vertices.stream().mapToDouble(Vertex::y).min().orElseThrow();
        double maxY = vertices.stream().mapToDouble(Vertex::y).max().orElseThrow();
        dx = Math.max(-minX, Math.min(Math.nextDown(drawing.getWidth()) - maxX, dx));
        dy = Math.max(-minY, Math.min(Math.nextDown(drawing.getHeight()) - maxY, dy));
        if (dx == 0 && dy == 0) return false;
        if (object instanceof Pixel pixel) {
            double x = pixel.getX() + dx;
            double y = pixel.getY() + dy;
            Pixel moved = switch (pixel) {
                case Point ignored -> new Point(x, y, 1, pixel.getColor());
                case Circle ignored -> new Circle(x, y, pixel.getSize(), pixel.getColor());
                case Square ignored -> new Square(x, y, pixel.getSize(), pixel.getColor());
                default -> throw new IllegalArgumentException("Неизвестный тип элемента");
            };
            drawing.replacePixel(getSelectedPixelIndex(), moved);
            selected = moved;
        } else {
            Shape moved = translated((Shape) object, dx, dy);
            drawing.replaceShape(getSelectedShapeIndex(), moved);
            selected = moved;
        }
        return true;
    }

    private static Shape translated(Shape shape, double dx, double dy) {
        return switch (shape) {
            case FillDecorator fill -> new FillDecorator(translated(fill.getWrapped(), dx, dy), fill.getColor());
            case BorderDecorator border -> new BorderDecorator(translated(border.getWrapped(), dx, dy), border.getColor(), border.getWidth());
            case ShadowDecorator shadow -> new ShadowDecorator(translated(shadow.getWrapped(), dx, dy), shadow.getColor(),
                    shadow.getOffsetX(), shadow.getOffsetY(), shadow.getBlur());
            case Polygon polygon -> new Polygon(polygon.getVertices().stream().map(vertex -> new Vertex(vertex.x() + dx, vertex.y() + dy)).toList());
            default -> throw new IllegalArgumentException("Неизвестный тип фигуры");
        };
    }
}
