package org.example.graphiceditor.controller;

import org.example.graphiceditor.model.DrawingModel;
import org.example.graphiceditor.model.vector.Polygon;
import org.example.graphiceditor.model.vector.Shape;
import org.example.graphiceditor.model.vector.VectorStyle;
import org.example.graphiceditor.model.vector.Vertex;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class PolygonController {
    private final DrawingModel model;
    private final List<Vertex> draftVertices = new ArrayList<>();
    private VectorStyle style = VectorStyle.defaults();
    private Shape selectedShape;

    public PolygonController(DrawingModel model) {
        this.model = Objects.requireNonNull(model, "model");
    }

    public List<Vertex> getDraftVertices() { return List.copyOf(draftVertices); }
    public VectorStyle getStyle() { return style; }

    public void setStyle(VectorStyle style) {
        this.style = Objects.requireNonNull(style, "style");
    }

    public boolean addVertex(double x, double y) {
        if (!model.contains(x, y)) {
            return false;
        }
        Vertex vertex = new Vertex(x, y);
        if (!draftVertices.isEmpty() && draftVertices.getLast().equals(vertex)) {
            return false;
        }
        draftVertices.add(vertex);
        return true;
    }

    public Shape finishPolygon() {
        if (draftVertices.size() < 3) {
            throw new IllegalStateException("Добавьте не меньше трёх вершин многоугольника");
        }
        Shape shape = style.decorate(new Polygon(draftVertices));
        if (!model.addShape(shape)) {
            throw new IllegalStateException("Вершины фигуры должны находиться внутри холста");
        }
        selectedShape = shape;
        draftVertices.clear();
        return shape;
    }

    public void cancelPolygon() { draftVertices.clear(); }

    public int getSelectedIndex() {
        if (selectedShape != null) {
            List<Shape> shapes = model.getShapes();
            for (int index = 0; index < shapes.size(); index++) {
                if (shapes.get(index) == selectedShape) {
                    return index;
                }
            }
            selectedShape = null;
        }
        return -1;
    }

    public Shape getSelectedShape() {
        return getSelectedIndex() >= 0 ? selectedShape : null;
    }

    public void selectShape(int index) {
        if (index == -1) {
            selectedShape = null;
            return;
        }
        Shape shape = model.getShape(index);
        VectorStyle selectedStyle = VectorStyle.from(shape);
        selectedShape = shape;
        style = selectedStyle;
    }

    public boolean applyStyleToSelected() {
        int index = getSelectedIndex();
        if (index < 0) {
            return false;
        }
        Shape replacement = style.decorate(VectorStyle.geometryOf(selectedShape));
        model.replaceShape(index, replacement);
        selectedShape = replacement;
        return true;
    }

    public boolean deleteSelected() {
        int index = getSelectedIndex();
        if (index < 0) {
            return false;
        }
        model.removeShape(index);
        selectedShape = null;
        return true;
    }

    public void clear() {
        cancelPolygon();
        selectedShape = null;
        model.clear();
    }
}
