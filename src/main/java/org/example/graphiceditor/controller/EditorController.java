package org.example.graphiceditor.controller;

import javafx.scene.paint.Color;
import org.example.graphiceditor.factory.PixelFactory;
import org.example.graphiceditor.model.DrawingModel;
import org.example.graphiceditor.model.Pixel;
import org.example.graphiceditor.model.Tool;
import org.example.graphiceditor.model.EditorSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class EditorController {
    private final DrawingModel model;
    private final EditorSettings settings;
    private PixelFactory strokeFactory;
    private Color strokeColor;
    private double strokeSize;
    private double lastX;
    private double lastY;
    private boolean drawing;
    private boolean connected;

    public EditorController(DrawingModel model) {
        this(model, new EditorSettings());
    }

    public EditorController(DrawingModel model, EditorSettings settings) {
        this.model = Objects.requireNonNull(model, "model");
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    public EditorSettings getSettings() { return settings; }
    public Tool getTool() { return settings.getTool(); }
    public Color getColor() { return settings.getColor(); }
    public double getBrushSize() { return settings.getBrushSize(); }
    public void setTool(Tool tool) { settings.setTool(tool); }
    public void setColor(Color color) { settings.setColor(color); }
    public void setBrushSize(double size) { settings.setBrushSize(size); }

    public List<Pixel> beginStroke(double x, double y) {
        endStroke();
        if (!model.contains(x, y)) {
            return List.of();
        }
        strokeFactory = PixelFactory.forTool(getTool());
        strokeColor = getColor();
        strokeSize = getTool() == Tool.POINT ? 1 : getBrushSize();
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
