package org.example.graphiceditor.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class DrawingModel {
    private final double width;
    private final double height;
    private final List<Pixel> pixels = new ArrayList<>();
    private final List<Pixel> view = Collections.unmodifiableList(pixels);

    public DrawingModel(double width, double height) {
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

    public List<Pixel> getPixels() { return view; }
    public int size() { return pixels.size(); }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public void clear() { pixels.clear(); }
}
