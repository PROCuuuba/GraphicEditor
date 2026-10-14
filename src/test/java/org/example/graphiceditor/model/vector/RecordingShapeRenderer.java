package org.example.graphiceditor.model.vector;

import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

final class RecordingShapeRenderer implements ShapeRenderer {
    final Observation observation;
    private final VectorStyle style;
    private final double opacity;

    RecordingShapeRenderer() {
        this(new Observation(), undecoratedStyle(), 1);
    }

    private RecordingShapeRenderer(Observation observation, VectorStyle style, double opacity) {
        this.observation = observation;
        this.style = style;
        this.opacity = opacity;
    }

    @Override
    public ShapeRenderer withFill(Color color) {
        observation.calls.add("fill");
        return new RecordingShapeRenderer(observation,
                new VectorStyle(true, color, style.borderEnabled(), style.borderColor(),
                        style.borderWidth(), style.shadowEnabled(), style.shadowColor(),
                        style.shadowOffsetX(), style.shadowOffsetY(), style.shadowBlur()), opacity);
    }

    @Override
    public ShapeRenderer withBorder(Color color, double width) {
        observation.calls.add("border");
        return new RecordingShapeRenderer(observation,
                new VectorStyle(style.fillEnabled(), style.fillColor(), true, color, width,
                        style.shadowEnabled(), style.shadowColor(), style.shadowOffsetX(),
                        style.shadowOffsetY(), style.shadowBlur()), opacity);
    }

    @Override
    public ShapeRenderer withShadow(Color color, double offsetX, double offsetY, double blur) {
        observation.calls.add("shadow");
        return new RecordingShapeRenderer(observation,
                new VectorStyle(style.fillEnabled(), style.fillColor(), style.borderEnabled(),
                        style.borderColor(), style.borderWidth(), true, color, offsetX, offsetY, blur), opacity);
    }

    @Override
    public ShapeRenderer withOpacity(double opacity) {
        observation.calls.add("opacity");
        return new RecordingShapeRenderer(observation, style, this.opacity * opacity);
    }

    @Override
    public void polygon(List<Vertex> vertices) {
        observation.calls.add("polygon");
        observation.vertices = vertices;
        observation.style = style;
        observation.opacity = opacity;
        observation.polygonCount++;
    }

    private static VectorStyle undecoratedStyle() {
        VectorStyle defaults = VectorStyle.defaults();
        return new VectorStyle(false, defaults.fillColor(), false, defaults.borderColor(),
                defaults.borderWidth(), false, defaults.shadowColor(), defaults.shadowOffsetX(),
                defaults.shadowOffsetY(), defaults.shadowBlur());
    }

    static final class Observation {
        final List<String> calls = new ArrayList<>();
        List<Vertex> vertices;
        VectorStyle style;
        double opacity;
        int polygonCount;
    }
}
