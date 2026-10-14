package org.example.graphiceditor.model.vector;

import java.util.List;
import java.util.Objects;

public abstract class ShapeDecorator implements Shape {
    private final Shape wrapped;

    protected ShapeDecorator(Shape wrapped) {
        this.wrapped = Objects.requireNonNull(wrapped, "wrapped");
    }

    public final Shape getWrapped() {
        return wrapped;
    }

    @Override
    public final List<Vertex> getVertices() {
        return wrapped.getVertices();
    }
}
