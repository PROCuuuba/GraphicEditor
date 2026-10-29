# Контрольная карта второго инкремента

Основание: [второй инкремент задания](https://github.com/olgmina/SWEngineering-technics.github.io/blob/gh-pages/Labs/prTask_GraphEditor.md#инкремент-2-векторный-редактор)
и [ведение репозитория](https://github.com/olgmina/SWEngineering-technics.github.io/blob/gh-pages/5_Release/Techniques/Введение_репо.md).
Карта связывает требования с существующими тестами; она не заменяет результат
конкретного запуска и не утверждает, что аппаратные жесты проверены на всех устройствах.

## Функциональность

| Требование | Проверка в `src/test/java/org/example/graphiceditor/` |
|---|---|
| FR-2.1: минимум три вершины | `model/vector/PolygonTest`, `controller/PolygonControllerTest.finishingPolygonStoresStyledGeometryAndSelectsIt`, `ui/VectorAcceptanceTest.polygonModeShowsItsControlsAndRequiresThreeVertices` |
| FR-2.2: координаты и отображение | `model/vector/PolygonTest`, `ui/VectorAcceptanceTest.applyChangesOnlyTheSelectedPolygonAndDrawsFillBorderAndShadow` |
| FR-2.3: инструмент полигона | `ui/VectorAcceptanceTest.polygonModeShowsItsControlsAndRequiresThreeVertices`, `ui/EditorInteractionTest.keyboardSwitchesToolsAndModesAndFinishesOrCancelsPolygonDrafts` |
| FR-2.4–2.6: заливка, контур, тень | `model/vector/ShapeDecoratorTest.fillAddsItsPropertyAndDelegatesToTheSamePolygon`, `borderAddsItsColorAndWidth`, `shadowAllowsNegativeOffsetsAndRetainsItsProperties`; снимки холста в `ui/VectorAcceptanceTest` |
| FR-2.7: независимые сочетания | `model/vector/ShapeDecoratorTest.allEightIndependentPropertyCombinationsAreRendered`, `everyPermutationPreservesAllPropertiesAndDelegatesInOrder`; `ui/VectorAcceptanceTest.allDecoratorOrdersRenderEquivalentFillBorderAndShadowWithoutStateLeaks` |
| FR-2.8: изменение готовой фигуры | `controller/PolygonControllerTest.applyingStyleReplacesSelectedDecoratorsWithoutChangingVerticesOrOrder`, `ui/VectorAcceptanceTest.appearanceControlsImmediatelyUpdateOnlyTheSelectedPolygon` |
| FR-2.9: несколько полигонов | `ui/VectorAcceptanceTest.finishingSeveralPolygonsMaintainsSeparateGeometryAndSelectsTheLatest` |
| FR-2.10: оба вида объектов | `ui/VectorAcceptanceTest.rasterDrawingStillWorksAfterPolygonDrawingAndVectorsStayAboveRaster` |
| FR-2.11: удаление выбранного | `controller/PolygonControllerTest.deletingSelectionRemovesOnlyItAndDeselects`, `ui/VectorAcceptanceTest.selectingAnotherPolygonRestoresItsOwnStyleAndDeleteRemovesOnlySelection` |
| FR-2.12: встроенные подсказки | `ui/VectorAcceptanceTest.polygonControlTooltipsCanBeDisplayedAsActualPopups` |

## Проверки T2-01–T2-20 из методички

| ID | Подтверждение |
|---|---|
| T2-01, T2-02 | `PolygonTest`: построение, координаты, неизменяемость и вызов `ShapeRenderer.polygon()` |
| T2-03, T2-11 | `VectorAcceptanceTest`: переключение режима и создание фигуры событиями мыши; инструмент является состоянием UI, не дополнительным значением растрового `Tool` |
| T2-04–T2-06 | `ShapeDecoratorTest`: каждый декоратор независимо передаёт свои параметры |
| T2-07, T2-09 | `completeChainDelegatesFromOuterToInnerAndRendersOneGeometry`: вызовы shadow → border → fill → polygon и одна геометрия |
| T2-08 | `allEightIndependentPropertyCombinationsAreRendered`: все 8 сочетаний включения свойств |
| T2-10 | `customTransparencyDecoratorComposesWithExistingDecorators`: новый тестовый декоратор без изменения `Polygon` |
| T2-12, T2-15 | `applyChangesOnlyTheSelectedPolygonAndDrawsFillBorderAndShadow`: модель и реальные цвета снимка Canvas |
| T2-13 | Модель: все 6 перестановок декораторов; GUI: эквивалентное отображение и отсутствие утечки контекста |
| T2-14 | `finishingSeveralPolygonsMaintainsSeparateGeometryAndSelectsTheLatest` |
| T2-16 | `EditorAcceptanceTest`, `rasterDrawingStillWorksAfterPolygonDrawingAndVectorsStayAboveRaster` |
| T2-17 | `factory/PixelFactoryTest`: `Point`, `Circle`, `Square`, ошибочные параметры |
| T2-18 | `clearRemovesRasterPolygonsAndUnfinishedDraft`, `DrawingModelVectorTest`, `PolygonControllerTest` |
| T2-19 | `polygonControlTooltipsCanBeDisplayedAsActualPopups` |
| T2-20 | `rasterDrawingStillWorksAfterPolygonDrawingAndVectorsStayAboveRaster` |

В таблице методички T2-16–18 подписаны FR-2.8, хотя это регрессионные
проверки первого инкремента. Здесь сохраняется смысл действий, а FR-2.8
отдельно проверяется изменением оформления.

Дополнительные приёмочные сценарии завершают отдельные функциональные задачи:

| Задача | GUI-проверка |
|---|---|
| #6: полигон | `PolygonFinalAcceptanceTest`: шесть кликов, вогнутый полигон, залитые части и белый вырез |
| #7: заливка | `FillFinalAcceptanceTest`: выключение и повторное включение с новым цветом, сохранение контура и тени |
| #8: контур | `BorderFinalAcceptanceTest`: толщины 0.5 и 20, выключение контура, сохранение заливки и тени |
| #9: тень | `ShadowFinalAcceptanceTest`: отрицательные смещения, очистка прежних пикселей и выключение тени |

Все четыре сценария проверяют реальные пиксели снимка холста и сохранность геометрии.

## Дополнительные удобства и границы

- `PolygonSnappingTest` проверяет привязку, точные координаты и запрет
  раннего замыкания; `PolygonVertexDragTest` — движение, сохранение стиля,
  границы и отмену жеста.
- `EditorInteractionTest` проверяет масштаб, центрирование и размер окна,
  координаты при масштабе, мышь, `ScrollEvent`, `ZoomEvent`, горячие клавиши
  и сохранение обычного ввода в полях.
- GUI-тесты работают с настоящими сценами JavaFX и снимками Canvas,
  но ввод создаётся программными событиями. Физический жест конкретной
  мыши/трекпада не равнозначен этому тесту и требует проверки на устройстве.
- Дополнительные масштаб, привязка, горячие клавиши и перетаскивание не
  заменяют обязательные функции. В 0.2.1 добавлено сохранение/открытие PNG
  проекта; группы объектов, сохранение настроек и Undo/Redo по-прежнему
  отсутствуют, третий инкремент целиком не реализован.
- Независимые декораторы не требуют подкласса `Polygon` для каждой комбинации.
  Изменение цепочки и геометрии выполняется заменой неизменяемого объекта.

## PNG проекта в версии 0.2.1

Дополнение второго инкремента сохраняет стандартное изображение 900×650
и описание модели в XML с версией формата. XML хранится в блоке PNG `iTXt`
под ключом `GraphicEditor.Project`; исходные элементы кистей, вершины и
оформление полигонов сохраняются независимо от растрового изображения.

Приёмочные сценарии для дополнения:

| Сценарий | Ожидаемый результат |
|---|---|
| Нарисовать кистями и создать несколько полигонов с разным оформлением; сохранить, очистить и открыть PNG | Восстановлены исходные типы, координаты, порядок и оформление объектов |
| После открытия выбрать прежний полигон, передвинуть вершину и изменить заливку, контур или тень | Изменяется выбранный объект, остальные фигуры сохраняются |
| Открыть сохранённый PNG обычным просмотрщиком | Видно изображение 900×650 без маркеров выделения |
| Открыть непустой проект и отменить подтверждение замены | Текущий рисунок не меняется |
| Выбрать обычный PNG без проекта или повреждённый файл | Понятная ошибка, текущий рисунок не меняется |
| Нажать кнопки вверху окна или Ctrl/Cmd + O и Ctrl/Cmd + S | Открывается соответствующий диалог выбора файла |

Эта таблица задаёт ожидаемое поведение и сама по себе не является
свидетельством прогона. Формат не совместим с проектами draw.io;
распознавание фигур из произвольного растра не предусмотрено. Метаданные
могут исчезнуть после обработки сторонним приложением, поэтому для
повторного редактирования нужен исходный файл PNG.

## Репозиторий и документация

Архитектура и UML находятся в `docs/`; SVG включены в README, Mermaid
сохранён рядом как редактируемый источник. Схемы базовой архитектуры 0.2.0
сохранены; пакет `io` для PNG проекта описан в README. Версия 0.2.1 указана в `pom.xml`,
README и CHANGELOG. Тесты находятся в `src/test/java` по структуре Maven.
Задачи, PR, самопроверка, тег и Release фиксируются в GitHub при публикации
инкремента; история первоначальных локальных коммитов сохраняется.
Первый инкремент находится в существующей `increment1`, второй — в существующей
`increment2`; прежний тег 0.2.0 не меняется.

Итоговая проверка: `./mvnw -DexcludedGroups= clean verify`, затем проверка
`git status`, истории коммитов, `.gitignore`, ссылок документации и версии.

Результат локальной проверки перед публикацией 2026-10-14: **150 тестов,
0 ошибок, 0 пропусков** — 97 модульных/контроллерных и 53 GUI-сценария.

Проверка дополнения 2026-10-29: `./mvnw -q -DexcludedGroups= clean verify` —
**161 тест, 0 сбоев, 0 ошибок, 0 пропусков**. Добавлены 9 проверок формата
в `io/PngProjectIOTest` и 2 сценария `ui/PngProjectAcceptanceTest`:
сохранение без масштаба/выделения, восстановление кистей, изменение заливки
и перетаскивание вершины импортированного полигона, повторное сохранение,
сохранность рисунка при ошибке чтения и запрет потери незавершённого полигона.
Нативные диалоги выбора файла и кнопки подтверждения этой автоматизацией
не проверялись; GUI-сценарии вызывают тот же код сохранения и открытия напрямую.
