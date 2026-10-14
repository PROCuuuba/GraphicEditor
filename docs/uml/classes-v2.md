# UML классов 0.2.0

![Классы](classes-v2.svg)

Показаны основные классы и методы двух инкрементов. Элементы JavaFX,
вспомогательные UI-поля, конструкторы и обычные getters опущены.
Методы и связи соответствуют исходникам, а не примерным именам из методички.

```mermaid
classDiagram
    class EditorPane
    class CanvasViewport {
        +getZoom() double
        +setZoom(double)
        +fitToViewport()
    }
    class DrawingCanvas {
        +renderAll()
        +drawPixels(List~Pixel~)
        +setDraftVertices(List~Vertex~)
        +setSelectedShape(Shape)
    }
    class EditorController {
        +setTool(Tool)
        +beginStroke(double, double) List~Pixel~
        +continueStroke(double, double) List~Pixel~
        +endStroke()
    }
    class PolygonController {
        +placeVertex(double, double, double) VertexAction
        +finishPolygon() Shape
        +selectShape(int)
        +applyStyleToSelected() boolean
        +startVertexDrag(double, double, double) boolean
        +dragVertexTo(double, double) boolean
        +deleteSelected() boolean
    }
    class DrawingModel {
        -List~Pixel~ pixels
        -List~Shape~ shapes
        +add(Pixel) boolean
        +addShape(Shape) boolean
        +replaceShape(int, Shape)
        +clear()
    }
    class Pixel {
        <<abstract>>
        +render(PixelRenderer)*
    }
    class Point
    class Circle
    class Square
    class PixelFactory {
        <<abstract>>
        +create(double, double, double, Color) Pixel*
        +forTool(Tool) PixelFactory$
    }
    class PointFactory
    class CircleFactory
    class SquareFactory
    class PixelRenderer {
        <<interface>>
        +point(Point)
        +circle(Circle)
        +square(Square)
    }
    class Shape {
        <<interface>>
        +getVertices() List~Vertex~
        +render(ShapeRenderer)
    }
    class Polygon {
        -List~Vertex~ vertices
        +getVertices() List~Vertex~
        +render(ShapeRenderer)
    }
    class Vertex {
        <<record>>
        +x() double
        +y() double
    }
    class ShapeDecorator {
        <<abstract>>
        -Shape wrapped
        +getWrapped() Shape
        +getVertices() List~Vertex~
    }
    class FillDecorator {
        -Color color
        +render(ShapeRenderer)
    }
    class BorderDecorator {
        -Color color
        -double width
        +render(ShapeRenderer)
    }
    class ShadowDecorator {
        -Color color
        -double offsetX
        -double offsetY
        -double blur
        +render(ShapeRenderer)
    }
    class VectorStyle {
        <<record>>
        +decorate(Shape) Shape
        +from(Shape) VectorStyle$
        +geometryOf(Shape) Polygon$
    }
    class ShapeRenderer {
        <<interface>>
        +withFill(Color) ShapeRenderer
        +withBorder(Color, double) ShapeRenderer
        +withShadow(Color, double, double, double) ShapeRenderer
        +withOpacity(double) ShapeRenderer
        +polygon(List~Vertex~)
    }
    class VectorRenderer {
        <<private nested record>>
        -GraphicsContext context
        -VectorStyle style
        -double opacity
    }
    EditorPane --> EditorController
    EditorPane --> PolygonController
    EditorPane --> DrawingModel
    EditorPane --> DrawingCanvas
    EditorPane --> CanvasViewport
    CanvasViewport --> DrawingCanvas
    DrawingCanvas --> DrawingModel
    EditorController --> DrawingModel
    PolygonController --> DrawingModel
    EditorController --> PixelFactory
    PixelFactory <|-- PointFactory
    PixelFactory <|-- CircleFactory
    PixelFactory <|-- SquareFactory
    PointFactory ..> Point : creates
    CircleFactory ..> Circle : creates
    SquareFactory ..> Square : creates
    Pixel <|-- Point
    Pixel <|-- Circle
    Pixel <|-- Square
    Pixel ..> PixelRenderer
    DrawingCanvas ..|> PixelRenderer
    DrawingModel o-- "0..*" Pixel
    DrawingModel o-- "0..*" Shape
    Shape <|.. Polygon
    Shape <|.. ShapeDecorator
    ShapeDecorator o-- "1" Shape : wrapped
    ShapeDecorator <|-- FillDecorator
    ShapeDecorator <|-- BorderDecorator
    ShapeDecorator <|-- ShadowDecorator
    Polygon o-- "3..*" Vertex
    Shape ..> ShapeRenderer
    VectorStyle ..> Polygon
    VectorStyle ..> FillDecorator
    VectorStyle ..> BorderDecorator
    VectorStyle ..> ShadowDecorator
    PolygonController --> VectorStyle
    DrawingCanvas ..> VectorRenderer : creates
    VectorRenderer ..|> ShapeRenderer
    VectorRenderer --> VectorStyle
```

`ShapeDecorator` реализует `Shape`, но оставляет `render()` конкретному
декоратору. Каждый декоратор имеет ровно одну ссылку `wrapped` на `Shape`;
она может указывать как на `Polygon`, так и на другой декоратор.

`VectorRenderer` — приватный вложенный record в `DrawingCanvas`, не отдельный
публичный класс. `VectorStyle` — неизменяемые параметры и адаптер между
элементами управления и цепочкой декораторов, не подкласс `Shape`.

Расширяемость проверяется тестовым `TransparencyDecorator` в
`ShapeDecoratorTest`: он наследует `ShapeDecorator` и вызывает
`renderer.withOpacity()` без изменения `Polygon`. В пользовательском
интерфейсе отдельного инструмента прозрачности нет.
