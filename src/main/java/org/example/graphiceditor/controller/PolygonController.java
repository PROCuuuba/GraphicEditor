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
    public enum VertexAction { ADDED, IGNORED, CLOSED }

    private final DrawingModel model;
    private final List<Vertex> draftVertices = new ArrayList<>();
    private VectorStyle style = VectorStyle.defaults();
    private Shape selectedShape;
    private Shape originalDraggedShape;
    private int draggedVertexIndex = -1;

    public PolygonController(DrawingModel model) {
        this.model = Objects.requireNonNull(model, "model");
    }

    public List<Vertex> getDraftVertices() { return List.copyOf(draftVertices); }
    public VectorStyle getStyle() { return style; }

    public void setStyle(VectorStyle style) {
        this.style = Objects.requireNonNull(style, "style");
    }

    public Vertex findSnapVertex(double x, double y, double tolerance) {
        if (!Double.isFinite(tolerance) || tolerance < 0) {
            throw new IllegalArgumentException("Радиус привязки должен быть конечным и неотрицательным");
        }
        if (!model.contains(x, y)) {
            return null;
        }
        Vertex nearest = null;
        double nearestDistance = tolerance;
        for (Vertex vertex : draftVertices) {
            double distance = Math.hypot(vertex.x() - x, vertex.y() - y);
            if (distance <= tolerance && (nearest == null || distance < nearestDistance)) {
                nearest = vertex;
                nearestDistance = distance;
            }
        }
        for (Shape shape : model.getShapes()) {
            for (Vertex vertex : shape.getVertices()) {
                double distance = Math.hypot(vertex.x() - x, vertex.y() - y);
                if (distance <= tolerance && (nearest == null || distance < nearestDistance)) {
                    nearest = vertex;
                    nearestDistance = distance;
                }
            }
        }
        return nearest;
    }

    public VertexAction placeVertex(double x, double y, double tolerance) {
        if (!model.contains(x, y)) {
            return VertexAction.IGNORED;
        }
        Vertex snapped = findSnapVertex(x, y, tolerance);
        if (snapped != null && !draftVertices.isEmpty() && snapped.equals(draftVertices.getFirst())) {
            if (draftVertices.size() < 3) {
                return VertexAction.IGNORED;
            }
            finishPolygon();
            return VertexAction.CLOSED;
        }
        double vertexX = snapped == null ? x : snapped.x();
        double vertexY = snapped == null ? y : snapped.y();
        return addVertex(vertexX, vertexY) ? VertexAction.ADDED : VertexAction.IGNORED;
    }

    public boolean startVertexDrag(double x, double y, double tolerance) {
        endVertexDrag();
        if (!Double.isFinite(tolerance) || tolerance < 0) {
            throw new IllegalArgumentException("Радиус выбора должен быть конечным и неотрицательным");
        }
        if (!draftVertices.isEmpty() || !model.contains(x, y)) {
            return false;
        }
        int shapeIndex = getSelectedIndex();
        int vertexIndex = shapeIndex < 0 ? -1 : nearestVertexIndex(selectedShape, x, y, tolerance);
        if (vertexIndex < 0) {
            shapeIndex = -1;
            double nearestDistance = tolerance;
            List<Shape> shapes = model.getShapes();
            for (int index = shapes.size() - 1; index >= 0; index--) {
                Shape shape = shapes.get(index);
                int candidateIndex = nearestVertexIndex(shape, x, y, tolerance);
                if (candidateIndex < 0) {
                    continue;
                }
                Vertex candidate = shape.getVertices().get(candidateIndex);
                double distance = Math.hypot(candidate.x() - x, candidate.y() - y);
                if (shapeIndex < 0 || distance < nearestDistance) {
                    shapeIndex = index;
                    vertexIndex = candidateIndex;
                    nearestDistance = distance;
                }
            }
        }
        if (shapeIndex < 0) {
            return false;
        }
        selectShape(shapeIndex);
        originalDraggedShape = selectedShape;
        draggedVertexIndex = vertexIndex;
        return true;
    }

    public boolean isDraggingVertex() {
        if (originalDraggedShape != null && getSelectedIndex() < 0) {
            endVertexDrag();
        }
        return originalDraggedShape != null;
    }

    public boolean dragVertexTo(double x, double y) {
        if (!isDraggingVertex() || !model.contains(x, y)) {
            return false;
        }
        Vertex vertex = new Vertex(x, y);
        if (selectedShape.getVertices().get(draggedVertexIndex).equals(vertex)) {
            return false;
        }
        List<Vertex> vertices = new ArrayList<>(selectedShape.getVertices());
        vertices.set(draggedVertexIndex, vertex);
        Shape replacement = VectorStyle.from(originalDraggedShape).decorate(new Polygon(vertices));
        model.replaceShape(getSelectedIndex(), replacement);
        selectedShape = replacement;
        return true;
    }

    public void endVertexDrag() {
        originalDraggedShape = null;
        draggedVertexIndex = -1;
    }

    public void cancelVertexDrag() {
        if (isDraggingVertex()) {
            model.replaceShape(getSelectedIndex(), originalDraggedShape);
            selectedShape = originalDraggedShape;
        }
        endVertexDrag();
    }

    private int nearestVertexIndex(Shape shape, double x, double y, double tolerance) {
        int nearestIndex = -1;
        double nearestDistance = tolerance;
        List<Vertex> vertices = shape.getVertices();
        for (int index = 0; index < vertices.size(); index++) {
            Vertex vertex = vertices.get(index);
            double distance = Math.hypot(vertex.x() - x, vertex.y() - y);
            if (distance <= tolerance && (nearestIndex < 0 || distance < nearestDistance)) {
                nearestIndex = index;
                nearestDistance = distance;
            }
        }
        return nearestIndex;
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

    public void cancelPolygon() {
        endVertexDrag();
        draftVertices.clear();
    }

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
            endVertexDrag();
            selectedShape = null;
            return;
        }
        Shape shape = model.getShape(index);
        VectorStyle selectedStyle = VectorStyle.from(shape);
        endVertexDrag();
        selectedShape = shape;
        style = selectedStyle;
    }

    public boolean applyStyleToSelected() {
        int index = getSelectedIndex();
        if (index < 0) {
            return false;
        }
        endVertexDrag();
        Shape replacement = style.decorate(VectorStyle.geometryOf(selectedShape));
        model.replaceShape(index, replacement);
        selectedShape = replacement;
        return true;
    }

    public boolean deleteSelected() {
        endVertexDrag();
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
