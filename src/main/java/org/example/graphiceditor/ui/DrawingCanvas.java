package org.example.graphiceditor.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.paint.Color;
import org.example.graphiceditor.model.Circle;
import org.example.graphiceditor.model.DrawingModel;
import org.example.graphiceditor.model.Pixel;
import org.example.graphiceditor.model.PixelRenderer;
import org.example.graphiceditor.model.Point;
import org.example.graphiceditor.model.Square;
import org.example.graphiceditor.model.vector.Shape;
import org.example.graphiceditor.model.vector.ShapeRenderer;
import org.example.graphiceditor.model.vector.VectorStyle;
import org.example.graphiceditor.model.vector.Vertex;

import java.util.List;

public final class DrawingCanvas extends Canvas implements PixelRenderer {
    private final DrawingModel model;
    private List<Vertex> draft = List.of();
    private Shape selected;

    public DrawingCanvas(DrawingModel model) {
        super(model.getWidth(), model.getHeight());
        this.model = model;
        setId("drawing-canvas");
        renderAll();
    }

    public void setDraftVertices(List<Vertex> vertices) {
        draft = List.copyOf(vertices);
    }

    public void setSelectedShape(Shape shape) {
        selected = shape;
    }

    public void renderAll() {
        GraphicsContext context = getGraphicsContext2D();
        context.setFill(Color.WHITE);
        context.fillRect(0, 0, getWidth(), getHeight());
        drawPixels(model.getPixels());
        for (Shape shape : model.getShapes()) {
            shape.render(new VectorRenderer(context, new VectorStyle(false, Color.BLACK,
                    false, Color.BLACK, 1, false, Color.BLACK, 0, 0, 0), 1));
        }
        drawOverlay(context);
    }

    public void drawPixels(List<Pixel> pixels) {
        for (Pixel pixel : pixels) {
            pixel.render(this);
        }
    }

    private void drawOverlay(GraphicsContext context) {
        context.save();
        context.setStroke(Color.DODGERBLUE);
        context.setFill(Color.DODGERBLUE);
        context.setLineWidth(1);
        if (selected != null && model.getShapes().contains(selected)) {
            List<Vertex> vertices = selected.getVertices();
            context.setLineDashes(5, 4);
            context.strokePolygon(xs(vertices, 0), ys(vertices, 0), vertices.size());
            context.setLineDashes();
            for (Vertex vertex : vertices) {
                context.strokeRect(vertex.x() - 3, vertex.y() - 3, 6, 6);
            }
        }
        if (!draft.isEmpty()) {
            context.strokePolyline(xs(draft, 0), ys(draft, 0), draft.size());
            for (Vertex vertex : draft) {
                context.fillOval(vertex.x() - 3, vertex.y() - 3, 6, 6);
            }
        }
        context.restore();
    }

    @Override
    public void point(Point point) {
        GraphicsContext context = getGraphicsContext2D();
        context.setFill(point.getColor());
        context.fillRect(Math.floor(point.getX()), Math.floor(point.getY()), 1, 1);
    }

    @Override
    public void circle(Circle circle) {
        GraphicsContext context = getGraphicsContext2D();
        context.setFill(circle.getColor());
        double radius = circle.getSize() / 2;
        context.fillOval(circle.getX() - radius, circle.getY() - radius, circle.getSize(), circle.getSize());
    }

    @Override
    public void square(Square square) {
        GraphicsContext context = getGraphicsContext2D();
        context.setFill(square.getColor());
        double half = square.getSize() / 2;
        context.fillRect(square.getX() - half, square.getY() - half, square.getSize(), square.getSize());
    }

    private static double[] xs(List<Vertex> vertices, double offset) {
        return vertices.stream().mapToDouble(vertex -> vertex.x() + offset).toArray();
    }

    private static double[] ys(List<Vertex> vertices, double offset) {
        return vertices.stream().mapToDouble(vertex -> vertex.y() + offset).toArray();
    }

    private record VectorRenderer(GraphicsContext context, VectorStyle style, double opacity)
            implements ShapeRenderer {
        @Override
        public ShapeRenderer withFill(Color color) {
            return new VectorRenderer(context, new VectorStyle(true, color, style.borderEnabled(),
                    style.borderColor(), style.borderWidth(), style.shadowEnabled(), style.shadowColor(),
                    style.shadowOffsetX(), style.shadowOffsetY(), style.shadowBlur()), opacity);
        }

        @Override
        public ShapeRenderer withBorder(Color color, double width) {
            return new VectorRenderer(context, new VectorStyle(style.fillEnabled(), style.fillColor(),
                    true, color, width, style.shadowEnabled(), style.shadowColor(),
                    style.shadowOffsetX(), style.shadowOffsetY(), style.shadowBlur()), opacity);
        }

        @Override
        public ShapeRenderer withShadow(Color color, double x, double y, double blur) {
            return new VectorRenderer(context, new VectorStyle(style.fillEnabled(), style.fillColor(),
                    style.borderEnabled(), style.borderColor(), style.borderWidth(), true, color, x, y, blur), opacity);
        }

        @Override
        public ShapeRenderer withOpacity(double value) {
            if (!Double.isFinite(value) || value < 0 || value > 1) {
                throw new IllegalArgumentException("Прозрачность должна быть от 0 до 1");
            }
            return new VectorRenderer(context, style, opacity * value);
        }

        @Override
        public void polygon(List<Vertex> vertices) {
            context.save();
            try {
                context.setGlobalAlpha(opacity);
                if (style.shadowEnabled()) {
                    context.setFill(style.shadowColor());
                    if (style.shadowBlur() > 0) {
                        context.setEffect(new GaussianBlur(style.shadowBlur()));
                    }
                    context.fillPolygon(xs(vertices, style.shadowOffsetX()),
                            ys(vertices, style.shadowOffsetY()), vertices.size());
                    context.setEffect(null);
                }
                double[] x = xs(vertices, 0);
                double[] y = ys(vertices, 0);
                if (style.fillEnabled()) {
                    context.setFill(style.fillColor());
                    context.fillPolygon(x, y, vertices.size());
                }
                if (style.borderEnabled() || !style.fillEnabled()) {
                    context.setStroke(style.borderEnabled() ? style.borderColor() : Color.GRAY);
                    context.setLineWidth(style.borderEnabled() ? style.borderWidth() : 1);
                    context.strokePolygon(x, y, vertices.size());
                }
            } finally {
                context.restore();
            }
        }
    }
}
