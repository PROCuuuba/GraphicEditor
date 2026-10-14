# Отображение декорированной фигуры

![Последовательность](sequence-v2.svg)

Сценарий соответствует цепочке, которую создаёт `VectorStyle.decorate()`
при включении всех трёх свойств:
`ShadowDecorator(BorderDecorator(FillDecorator(Polygon)))`.

```mermaid
sequenceDiagram
    participant C as DrawingCanvas
    participant S as ShadowDecorator
    participant B as BorderDecorator
    participant F as FillDecorator
    participant P as Polygon
    participant R as VectorRenderer r0…r3
    participant G as GraphicsContext
    C->>S: render(r0)
    S->>R: r0.withShadow(color, x, y, blur)
    R-->>S: r1 (новый контекст)
    S->>B: getWrapped().render(r1)
    B->>R: r1.withBorder(color, width)
    R-->>B: r2 (новый контекст)
    B->>F: getWrapped().render(r2)
    F->>R: r2.withFill(color)
    R-->>F: r3 (новый контекст)
    F->>P: getWrapped().render(r3)
    P->>R: r3.polygon(vertices)
    R->>G: save(); setGlobalAlpha(opacity)
    R->>G: тень со смещением и GaussianBlur
    R->>G: заливка
    R->>G: контур
    R->>G: restore() в finally
```

Каждый `with…()` возвращает новый `VectorRenderer` с дополненными
параметрами, сохраняя тот же `GraphicsContext`. Обозначение r0…r3 не
означает один изменяемый объект. Вызов геометрии происходит ровно один раз,
после прохождения цепочки от внешнего декоратора к внутреннему.

Декораторы не рисуют слои по отдельности. Они собирают контекст, а
`polygon()` рисует слои в порядке «тень → заливка → контур». Поэтому
перестановка различных декораторов не меняет итоговый порядок слоёв.
`save()` / `restore()` защищают следующий объект от утечки состояния.

Если выключены и заливка, и контур, базовая геометрия остаётся видна как
тонкий серый контур. Это служебное отображение `Polygon`, не дополнительный
декоратор.
