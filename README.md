# GraphicEditor

Учебный графический редактор на JavaFX. В основной опубликованной ветке
завершён первый инкремент. Локальная ветка codex/increment-2 добавляет
векторные полигоны к растровому рисованию.

Возможности: реализованы точка, круг и квадрат; добавлены инструменты
создания графических элементов, выбор цвета и размера кисти, очистка холста.

Размер круглой/квадратной кисти — от 1 до 100 пикселей. Точка — 1 пиксель.
Выбор цвета влияет на новые штрихи, уже созданные элементы сохраняются.

Для запуска нужен JDK 25:

```sh
./mvnw javafx:run
```

Проверки модели и контроллера: `./mvnw test`.
Все проверки, включая JavaFX (нужен рабочий стол):
`./mvnw -DexcludedGroups= clean verify`.

Первый инкремент был реализован локально до выполнения Git-практики.
Исходная история сохранена. Ветки feature/point, feature/circle,
feature/square, feature/tools и feature/factory используются для настоящих
доработок, проверок и изучения PR, review и слияния. Они не выдаются за
историю первоначального написания уже готовых классов.

## История Git-практики

Схема соответствует реальным коммитам и слияниям:

```mermaid
gitGraph
    commit id: "f9b735e"
    branch feature/point
    commit id: "930a3cc"
    checkout main
    merge feature/point id: "5eaa3d7"
    branch feature/circle
    commit id: "c0593e9"
    checkout main
    merge feature/circle id: "aa713e9"
    branch feature/tools
    commit id: "6af0de3"
    checkout main
    branch feature/square
    commit id: "3fc546a"
    checkout main
    merge feature/square id: "cacb7d6"
    checkout feature/tools
    merge main id: "8f8ac10"
    checkout main
    merge feature/tools id: "7d4a1d1"
    branch feature/factory
    commit id: "99f8697"
    checkout main
    merge feature/factory id: "420876c"
```

Каждая ветка прошла Pull Request и самопроверку кода. Конфликт README
между feature/square и feature/tools возник при настоящем слиянии и
разрешён коммитом 8f8ac10. Review одним автором оформлен комментарием:
GitHub не позволяет одобрить собственный Pull Request.

## Второй инкремент

В режиме «Полигон» задайте не менее трёх вершин кликами и нажмите
«Завершить полигон» или Enter. «Отменить вершины» или Escape сбрасывает
черновик. Выберите созданную фигуру в списке, настройте независимые
заливку, контур и тень, затем нажмите «Применить оформление».
«Удалить полигон» удаляет выбранную фигуру. «Очистить холст» удаляет
растровые и векторные элементы. Кисти первого инкремента продолжают работать.

Polygon хранит геометрию. ShapeDecorator оборачивает Shape, а FillDecorator,
BorderDecorator и ShadowDecorator передают вызов дальше с дополненным
контекстом ShapeRenderer. После обхода цепочки тень, заливка и контур
отрисовываются в стабильном порядке независимо от порядка обёрток.

```mermaid
sequenceDiagram
    participant Canvas as DrawingCanvas
    participant Shadow as ShadowDecorator
    participant Border as BorderDecorator
    participant Fill as FillDecorator
    participant Polygon
    participant Renderer as ShapeRenderer
    Canvas->>Shadow: render(renderer)
    Shadow->>Renderer: withShadow(...)
    Shadow->>Border: render(context)
    Border->>Renderer: withBorder(...)
    Border->>Fill: render(context)
    Fill->>Renderer: withFill(...)
    Fill->>Polygon: render(context)
    Polygon->>Renderer: polygon(vertices)
    Renderer->>Canvas: тень, заливка, контур
```

При совместной работе растровый слой находится под полигонами.
Для инструментов предусмотрены всплывающие подсказки.
Сохранение, загрузка и Undo/Redo относятся к третьему инкременту
и здесь не реализованы.
