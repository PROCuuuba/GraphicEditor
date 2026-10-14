package org.example.graphiceditor.model.vector;

import java.util.List;

public interface Shape {
    List<Vertex> getVertices();
    void render(ShapeRenderer renderer);
}
