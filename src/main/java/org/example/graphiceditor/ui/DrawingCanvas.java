package org.example.graphiceditor.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.example.graphiceditor.model.Circle;
import org.example.graphiceditor.model.DrawingModel;
import org.example.graphiceditor.model.Pixel;
import org.example.graphiceditor.model.PixelRenderer;
import org.example.graphiceditor.model.Point;
import org.example.graphiceditor.model.Square;

import java.util.List;

public final class DrawingCanvas extends Canvas implements PixelRenderer {
    private final DrawingModel model;

    public DrawingCanvas(DrawingModel model) {
        super(model.getWidth(), model.getHeight());
        this.model = model;
        setId("drawing-canvas");
        renderAll();
    }

    public void renderAll() {
        GraphicsContext context = getGraphicsContext2D();
        context.setFill(Color.WHITE);
        context.fillRect(0, 0, getWidth(), getHeight());
        drawPixels(model.getPixels());
    }

    public void drawPixels(List<Pixel> pixels) {
        for (Pixel pixel : pixels) {
            pixel.render(this);
        }
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
        context.fillOval(circle.getX() - radius, circle.getY() - radius,
                circle.getSize(), circle.getSize());
    }

    @Override
    public void square(Square square) {
        GraphicsContext context = getGraphicsContext2D();
        context.setFill(square.getColor());
        double halfSize = square.getSize() / 2;
        context.fillRect(square.getX() - halfSize, square.getY() - halfSize,
                square.getSize(), square.getSize());
    }
}
