package org.example.graphiceditor.controller;

import javafx.scene.paint.Color;
import org.example.graphiceditor.factory.PixelFactory;
import org.example.graphiceditor.model.DrawingModel;
import org.example.graphiceditor.model.Pixel;
import org.example.graphiceditor.model.Tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class EditorController {
    private final DrawingModel model;
    private Tool tool = Tool.POINT;
    private Color color = Color.BLACK;
    private double brushSize = 16;
    private PixelFactory strokeFactory;
    private Color strokeColor;
    private double strokeSize;
    private double lastX;
    private double lastY;
    private boolean drawing;
    private boolean connected;

    public EditorController(DrawingModel model) {
        this.model = Objects.requireNonNull(model, "model");
    }

    public Tool getTool() { return tool; }
    public Color getColor() { return color; }
    public double getBrushSize() { return brushSize; }

    public void setTool(Tool tool) {
        if (tool == null) {
            throw new IllegalArgumentException("Выберите инструмент");
        }
        this.tool = tool;
    }

    public void setColor(Color color) {
        this.color = Objects.requireNonNull(color, "color");
    }

    public void setBrushSize(double size) {
        if (!Double.isFinite(size) || size < 1 || size > 100) {
            throw new IllegalArgumentException("Размер кисти должен быть от 1 до 100");
        }
        brushSize = size;
    }

    public List<Pixel> beginStroke(double x, double y) {
        endStroke();
        if (!model.contains(x, y)) {
            return List.of();
        }
        strokeFactory = PixelFactory.forTool(tool);
        strokeColor = color;
        strokeSize = tool == Tool.POINT ? 1 : brushSize;
        drawing = true;
        connected = true;
        lastX = x;
        lastY = y;
        return stamp(x, y);
    }

    public List<Pixel> continueStroke(double x, double y) {
        if (!drawing) {
            return List.of();
        }
        if (!model.contains(x, y)) {
            connected = false;
            return List.of();
        }
        if (!connected) {
            connected = true;
            lastX = x;
            lastY = y;
            return stamp(x, y);
        }
        double distance = Math.hypot(x - lastX, y - lastY);
        if (distance == 0) {
            return List.of();
        }
        int steps = (int) Math.ceil(distance / Math.max(1, strokeSize / 4));
        List<Pixel> added = new ArrayList<>(steps);
        for (int step = 1; step <= steps; step++) {
            double fraction = (double) step / steps;
            added.addAll(stamp(lastX + (x - lastX) * fraction, lastY + (y - lastY) * fraction));
        }
        lastX = x;
        lastY = y;
        return List.copyOf(added);
    }

    private List<Pixel> stamp(double x, double y) {
        Pixel pixel = strokeFactory.create(x, y, strokeSize, strokeColor);
        return model.add(pixel) ? List.of(pixel) : List.of();
    }

    public void endStroke() {
        drawing = false;
        connected = false;
    }

    public void clear() {
        endStroke();
        model.clear();
    }
}
