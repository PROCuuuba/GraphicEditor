# Архитектура 0.2.0

![Архитектура](architecture-v2.svg)

Схема отражает текущий код. Сплошные стрелки показывают управление и
использование объектов; пунктирные — чтение модели при отображении.
`DrawingModel` не вызывает UI и не знает о масштабе.

```mermaid
flowchart TB
    App[GraphicsEditorApplication] --> UI[EditorPane]
    UI --> Raster[EditorController]
    UI --> Vector[PolygonController]
    UI --> View[CanvasViewport]
    UI --> Canvas[DrawingCanvas]
    View -->|Scale: только представление| Canvas
    Raster --> Factory[PixelFactory]
    Factory --> Pixels[Point / Circle / Square : Pixel]
    Raster --> Model[DrawingModel]
    Vector --> Style[VectorStyle]
    Style --> Shapes[Polygon + ShapeDecorator : Shape]
    Vector --> Model
    Model -->|хранит| Pixels
    Model -->|хранит| Shapes
    Canvas -.->|читает коллекции| Model
    Canvas -->|PixelRenderer| Pixels
    Canvas --> Renderer[DrawingCanvas.VectorRenderer]
    Canvas -->|render| Shapes
    Shapes -->|ShapeRenderer| Renderer
    Renderer --> Graphics[JavaFX GraphicsContext]
```

Контроллер растрового режима создаёт объекты через фабрику и записывает их в
модель. Контроллер полигонов хранит черновик и выбор, создаёт `Polygon` и
оборачивает его выбранными декораторами. При изменении геометрии или стиля
готовая фигура заменяется в модели, а не изменяется внутри `Polygon`.

`DrawingCanvas.renderAll()` рисует белый фон, растровые элементы, полигоны и
служебные маркеры. `CanvasViewport` оборачивает Canvas в JavaFX-контейнеры
для прокрутки и масштаба; размер модели остаётся 900×650.
