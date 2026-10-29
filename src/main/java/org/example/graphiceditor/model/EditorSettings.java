package org.example.graphiceditor.model;

import javafx.scene.paint.Color;
import org.example.graphiceditor.model.vector.VectorStyle;

import java.util.Objects;

public final class EditorSettings {
    private Tool tool = Tool.POINT;
    private Color color = Color.BLACK;
    private double brushSize = 16;
    private VectorStyle vectorStyle = VectorStyle.defaults();
    private Color backgroundColor = Color.WHITE;
    private boolean snapEnabled = true;
    private double snapTolerance = 10;

    public Tool getTool() { return tool; }
    public void setTool(Tool tool) {
        if (tool == null) throw new IllegalArgumentException("Выберите инструмент");
        this.tool = tool;
    }

    public Color getColor() { return color; }
    public void setColor(Color color) { this.color = Objects.requireNonNull(color, "color"); }
    public double getBrushSize() { return brushSize; }
    public void setBrushSize(double size) {
        if (!Double.isFinite(size) || size < 1 || size > 100) {
            throw new IllegalArgumentException("Размер кисти должен быть от 1 до 100");
        }
        brushSize = size;
    }

    public VectorStyle getVectorStyle() { return vectorStyle; }
    public void setVectorStyle(VectorStyle style) { vectorStyle = Objects.requireNonNull(style, "style"); }
    public Color getBackgroundColor() { return backgroundColor; }
    public void setBackgroundColor(Color color) { backgroundColor = Objects.requireNonNull(color, "color"); }
    public boolean isSnapEnabled() { return snapEnabled; }
    public void setSnapEnabled(boolean enabled) { snapEnabled = enabled; }
    public double getSnapTolerance() { return snapTolerance; }
    public void setSnapTolerance(double value) {
        if (!Double.isFinite(value) || value < 0 || value > 100) {
            throw new IllegalArgumentException("Радиус привязки должен быть от 0 до 100");
        }
        snapTolerance = value;
    }
}
