package org.example.graphiceditor.ui;

import javafx.scene.Node;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.example.graphiceditor.controller.EditorController;
import org.example.graphiceditor.controller.PolygonController;
import org.example.graphiceditor.model.DrawingModel;
import org.example.graphiceditor.model.Pixel;
import org.example.graphiceditor.model.Tool;
import org.example.graphiceditor.model.vector.Shape;
import org.example.graphiceditor.model.vector.VectorStyle;
import org.example.graphiceditor.model.vector.Vertex;

import java.util.List;

public final class EditorPane extends BorderPane {
    private final DrawingModel model;
    private final EditorController controller;
    private final PolygonController polygons;
    private final DrawingCanvas canvas;
    private final CanvasViewport viewport;
    private final ToggleGroup selection = new ToggleGroup();
    private final ToggleButton rasterMode = new ToggleButton("Кисти");
    private final ToggleButton polygonMode = new ToggleButton("Полигон");
    private final Label status = new Label();
    private final Slider brushSize = new Slider(1, 100, 16);
    private final Label sizeLabel = new Label();
    private final ColorPicker colorPicker;
    private final VBox rasterPanel = new VBox(12);
    private final VBox vectorPanel = new VBox(10);
    private final ListView<Shape> polygonList = new ListView<>();
    private final Button finish = new Button("Завершить полигон");
    private final Button cancel = new Button("Отменить вершины");
    private final Button apply = new Button("Применить оформление");
    private final Button delete = new Button("Удалить полигон");
    private final CheckBox fillEnabled = new CheckBox("Заливка");
    private final ColorPicker fillColor = new ColorPicker();
    private final CheckBox borderEnabled = new CheckBox("Контур");
    private final ColorPicker borderColor = new ColorPicker();
    private final Spinner<Double> borderWidth = spinner("border-width", 0.5, 20, 2, 0.5);
    private final CheckBox shadowEnabled = new CheckBox("Тень");
    private final ColorPicker shadowColor = new ColorPicker();
    private final Spinner<Double> shadowX = spinner("shadow-x", -100, 100, 8, 1);
    private final Spinner<Double> shadowY = spinner("shadow-y", -100, 100, 8, 1);
    private final Spinner<Double> shadowBlur = spinner("shadow-blur", 0, 50, 5, 1);
    private boolean vectorMode;
    private boolean syncingList;
    private Vertex dragPress;
    private Vertex dragOrigin;
    private boolean vertexMoved;

    public EditorPane() {
        this(new DrawingModel(900, 650));
    }

    private EditorPane(DrawingModel model) {
        this(model, new EditorController(model));
    }

    public EditorPane(DrawingModel model, EditorController controller) {
        this.model = model;
        this.controller = controller;
        polygons = new PolygonController(model);
        canvas = new DrawingCanvas(model);
        viewport = new CanvasViewport(canvas);
        colorPicker = new ColorPicker(controller.getColor());
        brushSize.setValue(controller.getBrushSize());
        getStyleClass().add("editor");
        setTop(header());
        setLeft(controls());
        setCenter(viewport);
        status.setId("drawing-status");
        HBox footer = new HBox(status);
        footer.getStyleClass().add("status-bar");
        setBottom(footer);
        connectDrawing();
        loadStyle(polygons.getStyle());
        refresh();
        addEventFilter(KeyEvent.KEY_PRESSED, this::handleShortcut);
        sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((property, oldWindow, newWindow) -> {
                    if (newWindow != null) {
                        newWindow.focusedProperty().addListener((focus, wasFocused, isFocused) -> {
                            if (!isFocused) {
                                controller.endStroke();
                                stopVertexDrag();
                            }
                        });
                    }
                });
            }
        });
    }

    private VBox header() {
        Label title = new Label("Графический редактор");
        title.getStyleClass().add("editor-title");
        Label subtitle = new Label("Кисти и полигоны • заливка, контур, тень");
        subtitle.getStyleClass().add("editor-subtitle");
        VBox header = new VBox(4, title, subtitle);
        header.getStyleClass().add("editor-header");
        return header;
    }

    private BorderPane controls() {
        ToggleGroup modes = new ToggleGroup();
        rasterMode.setId("mode-raster");
        polygonMode.setId("mode-polygon");
        rasterMode.setToggleGroup(modes);
        polygonMode.setToggleGroup(modes);
        rasterMode.setSelected(true);
        rasterMode.setTooltip(new Tooltip("Рисование точкой, круглой или квадратной кистью"));
        polygonMode.setTooltip(new Tooltip("Полигон • P. Для замыкания нажмите первую вершину; готовые вершины можно перетаскивать"));
        modes.selectedToggleProperty().addListener((observable, previous, selected) -> {
            if (selected == null) {
                if (previous != null) previous.setSelected(true);
                return;
            }
            vectorMode = selected == polygonMode;
            controller.endStroke();
            polygons.cancelVertexDrag();
            stopVertexDrag();
            polygons.cancelPolygon();
            refresh();
        });
        HBox modeButtons = new HBox(8, rasterMode, polygonMode);

        VBox tools = new VBox(8);
        for (Tool tool : Tool.values()) tools.getChildren().add(toolButton(tool));
        selection.selectedToggleProperty().addListener((observable, previous, selected) -> {
            if (selected == null) {
                if (previous != null) previous.setSelected(true);
                return;
            }
            controller.setTool((Tool) selected.getUserData());
            updateStatus();
        });
        colorPicker.setId("color-picker");
        colorPicker.setMaxWidth(Double.MAX_VALUE);
        colorPicker.valueProperty().addListener((observable, previous, color) -> {
            controller.setColor(color);
            updateStatus();
        });
        brushSize.setId("brush-size");
        brushSize.setShowTickLabels(true);
        brushSize.setShowTickMarks(true);
        brushSize.setMajorTickUnit(25);
        brushSize.setMinorTickCount(24);
        brushSize.setSnapToTicks(true);
        brushSize.setBlockIncrement(1);
        brushSize.valueProperty().addListener((observable, oldValue, newValue) -> {
            controller.setBrushSize(Math.round(newValue.doubleValue()));
            updateStatus();
        });
        rasterPanel.getChildren().addAll(section("Инструменты"), tools,
                new Separator(), section("Цвет"), colorPicker,
                new Separator(), section("Размер кисти"), brushSize, sizeLabel);
        buildVectorPanel();

        VBox content = new VBox(12, modeButtons, rasterPanel, vectorPanel);
        content.getStyleClass().add("controls-content");
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        Button clear = new Button("Очистить холст");
        clear.setId("clear-canvas");
        clear.setMaxWidth(Double.MAX_VALUE);
        clear.setTooltip(new Tooltip("Очистить холст • Ctrl/Cmd + Shift + Delete"));
        clear.setOnAction(event -> clearDrawing());
        VBox bottom = new VBox(10, new Separator(), clear);
        bottom.getStyleClass().add("clear-panel");
        BorderPane panel = new BorderPane(scroll);
        panel.setBottom(bottom);
        panel.setPrefWidth(280);
        panel.setMinWidth(280);
        return panel;
    }

    private void buildVectorPanel() {
        finish.setId("polygon-finish");
        finish.setTooltip(new Tooltip("Замкнуть фигуру с минимум тремя вершинами; Enter"));
        finish.setOnAction(event -> {
            try {
                polygons.setStyle(readStyle());
                polygons.finishPolygon();
                refresh();
            } catch (IllegalArgumentException | IllegalStateException exception) {
                status.setText(exception.getMessage());
            }
        });
        cancel.setId("polygon-cancel");
        cancel.setTooltip(new Tooltip("Отменить вершины или текущее перетаскивание • Escape"));
        cancel.setOnAction(event -> {
            polygons.cancelVertexDrag();
            stopVertexDrag();
            polygons.cancelPolygon();
            refresh();
        });
        polygonList.setId("polygon-list");
        polygonList.setPrefHeight(105);
        polygonList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Shape shape, boolean empty) {
                super.updateItem(shape, empty);
                setText(empty || shape == null ? null : "Полигон " + (getIndex() + 1)
                        + " • " + shape.getVertices().size() + " верш.");
            }
        });
        polygonList.getSelectionModel().selectedIndexProperty().addListener((observable, previous, index) -> {
            if (!syncingList) {
                stopVertexDrag();
                polygons.selectShape(index.intValue());
                if (polygons.getSelectedShape() != null) loadStyle(polygons.getStyle());
                refresh();
            }
        });
        fillEnabled.setId("fill-enabled");
        fillColor.setId("fill-color");
        borderEnabled.setId("border-enabled");
        borderColor.setId("border-color");
        shadowEnabled.setId("shadow-enabled");
        shadowColor.setId("shadow-color");
        fillEnabled.setTooltip(new Tooltip("Добавить или убрать независимую заливку полигона"));
        borderEnabled.setTooltip(new Tooltip("Добавить или убрать контур с выбранной толщиной"));
        shadowEnabled.setTooltip(new Tooltip("Добавить или убрать тень со смещением и размытием"));
        apply.setId("polygon-apply");
        apply.setTooltip(new Tooltip("Изменить оформление выбранного полигона • Ctrl/Cmd + Enter"));
        apply.setOnAction(event -> {
            stopVertexDrag();
            polygons.setStyle(readStyle());
            polygons.applyStyleToSelected();
            refresh();
        });
        delete.setId("polygon-delete");
        delete.setTooltip(new Tooltip("Удалить выбранный полигон • Delete / Backspace"));
        delete.setOnAction(event -> {
            stopVertexDrag();
            polygons.deleteSelected();
            refresh();
        });
        GridPane border = propertyGrid();
        border.add(new Label("Толщина"), 0, 0);
        border.add(borderWidth, 1, 0);
        GridPane shadow = propertyGrid();
        shadow.addRow(0, new Label("Сдвиг X"), shadowX);
        shadow.addRow(1, new Label("Сдвиг Y"), shadowY);
        shadow.addRow(2, new Label("Размытие"), shadowBlur);
        vectorPanel.getChildren().addAll(section("Вершины"), finish, cancel,
                section("Полигоны"), polygonList, delete,
                new Separator(), fillEnabled, fillColor,
                borderEnabled, borderColor, border,
                shadowEnabled, shadowColor, shadow, apply);
        for (Button button : List.of(finish, cancel, apply, delete)) button.setMaxWidth(Double.MAX_VALUE);
        for (ColorPicker picker : List.of(fillColor, borderColor, shadowColor)) picker.setMaxWidth(Double.MAX_VALUE);
    }

    private VectorStyle readStyle() {
        return new VectorStyle(fillEnabled.isSelected(), fillColor.getValue(),
                borderEnabled.isSelected(), borderColor.getValue(), borderWidth.getValue(),
                shadowEnabled.isSelected(), shadowColor.getValue(),
                shadowX.getValue(), shadowY.getValue(), shadowBlur.getValue());
    }

    private void loadStyle(VectorStyle style) {
        fillEnabled.setSelected(style.fillEnabled());
        fillColor.setValue(style.fillColor());
        borderEnabled.setSelected(style.borderEnabled());
        borderColor.setValue(style.borderColor());
        borderWidth.getValueFactory().setValue(style.borderWidth());
        shadowEnabled.setSelected(style.shadowEnabled());
        shadowColor.setValue(style.shadowColor());
        shadowX.getValueFactory().setValue(style.shadowOffsetX());
        shadowY.getValueFactory().setValue(style.shadowOffsetY());
        shadowBlur.getValueFactory().setValue(style.shadowBlur());
    }

    private ToggleButton toolButton(Tool tool) {
        ToggleButton button = new ToggleButton(tool.getDisplayName());
        button.setId("tool-" + tool.name().toLowerCase(java.util.Locale.ROOT));
        button.setUserData(tool);
        button.setToggleGroup(selection);
        button.setSelected(tool == controller.getTool());
        button.setMaxWidth(Double.MAX_VALUE);
        button.setMinHeight(36);
        button.setOnAction(event -> rasterMode.setSelected(true));
        button.setTooltip(new Tooltip(tool == Tool.POINT ? "Рисование точками размером 1 пиксель"
                : "Рисование " + (tool == Tool.CIRCLE ? "круглой" : "квадратной") + " кистью"));
        return button;
    }

    private void connectDrawing() {
        canvas.setCursor(Cursor.CROSSHAIR);
        canvas.setFocusTraversable(true);
        canvas.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                canvas.requestFocus();
                if (vectorMode) {
                    if (polygons.startVertexDrag(event.getX(), event.getY(), snapTolerance())) {
                        dragPress = new Vertex(event.getX(), event.getY());
                        dragOrigin = polygons.getSelectedShape().getVertices().stream().min(
                                java.util.Comparator.comparingDouble(vertex ->
                                        Math.hypot(vertex.x() - event.getX(), vertex.y() - event.getY()))).orElseThrow();
                        vertexMoved = false;
                        refresh();
                    } else placeVertex(event.getX(), event.getY());
                } else draw(controller.beginStroke(event.getX(), event.getY()));
                event.consume();
            }
        });
        canvas.setOnMouseDragged(event -> {
            if (event.isPrimaryButtonDown()) {
                if (vectorMode && dragPress != null && polygons.isDraggingVertex()) {
                    if (vertexMoved || Math.hypot(event.getX() - dragPress.x(),
                            event.getY() - dragPress.y()) * viewport.getZoom() >= 2) {
                        if (!vertexMoved) loadStyle(polygons.getStyle());
                        vertexMoved = true;
                        polygons.dragVertexTo(dragOrigin.x() + event.getX() - dragPress.x(),
                                dragOrigin.y() + event.getY() - dragPress.y());
                        refresh();
                    }
                } else if (!vectorMode) draw(controller.continueStroke(event.getX(), event.getY()));
                event.consume();
            }
        });
        canvas.setOnMouseMoved(event -> {
            if (vectorMode) {
                Vertex snap = polygons.findSnapVertex(event.getX(), event.getY(), snapTolerance());
                canvas.setSnapVertex(snap);
                canvas.setCursor(snap != null && polygons.getDraftVertices().isEmpty()
                        ? Cursor.HAND : Cursor.CROSSHAIR);
                canvas.renderAll();
            }
        });
        canvas.setOnMouseExited(event -> {
            controller.continueStroke(-1, -1);
            canvas.setSnapVertex(null);
            canvas.renderAll();
        });
        canvas.setOnMouseReleased(event -> {
            if (event.getButton() != MouseButton.PRIMARY) return;
            controller.endStroke();
            if (dragPress != null) {
                Vertex pressed = dragPress;
                boolean moved = vertexMoved;
                stopVertexDrag();
                if (!moved) placeVertex(pressed.x(), pressed.y());
                else refresh();
            }
        });
    }

    private void draw(List<Pixel> pixels) {
        if (model.getShapes().isEmpty()) canvas.drawPixels(pixels);
        else canvas.renderAll();
        updateStatus();
    }

    private void refresh() {
        rasterPanel.setVisible(!vectorMode);
        rasterPanel.setManaged(!vectorMode);
        vectorPanel.setVisible(vectorMode);
        vectorPanel.setManaged(vectorMode);
        int index = polygons.getSelectedIndex();
        syncingList = true;
        try {
            polygonList.getItems().setAll(model.getShapes());
            polygonList.getSelectionModel().select(index);
        } finally {
            syncingList = false;
        }
        finish.setDisable(polygons.getDraftVertices().size() < 3);
        cancel.setDisable(polygons.getDraftVertices().isEmpty() && !polygons.isDraggingVertex());
        apply.setDisable(index < 0);
        delete.setDisable(index < 0);
        canvas.setDraftVertices(polygons.getDraftVertices());
        canvas.setSelectedShape(vectorMode ? polygons.getSelectedShape() : null);
        canvas.setSnapVertex(null);
        canvas.setCursor(Cursor.CROSSHAIR);
        canvas.renderAll();
        updateStatus();
    }

    private void updateStatus() {
        brushSize.setDisable(controller.getTool() == Tool.POINT);
        sizeLabel.setText(controller.getTool() == Tool.POINT ? "Точка: 1 пиксель"
                : "Размер: %.0f пикс.".formatted(controller.getBrushSize()));
        status.setText(vectorMode
                ? "Полигон • Вершин: %d • Полигонов: %d".formatted(
                        polygons.getDraftVertices().size(), model.getShapes().size())
                : "Инструмент: %s • Элементов: %d • Полигонов: %d".formatted(
                        controller.getTool().getDisplayName(), model.size(), model.getShapes().size()));
    }

    private double snapTolerance() { return 10 / viewport.getZoom(); }

    private void placeVertex(double x, double y) {
        polygons.setStyle(readStyle());
        polygons.placeVertex(x, y, snapTolerance());
        refresh();
    }

    private void stopVertexDrag() {
        polygons.endVertexDrag();
        dragPress = null;
        dragOrigin = null;
        vertexMoved = false;
    }

    private void clearDrawing() {
        stopVertexDrag();
        controller.clear();
        polygons.clear();
        refresh();
    }

    private boolean inputHasFocus() {
        Node focus = getScene() == null ? null : getScene().getFocusOwner();
        for (Node node = focus; node != null; node = node.getParent()) {
            if (node instanceof TextInputControl || node instanceof Spinner<?> || node instanceof ComboBoxBase<?>) return true;
        }
        return false;
    }

    private void handleShortcut(KeyEvent event) {
        if (inputHasFocus() || event.isAltDown()) return;
        boolean shortcut = event.isShortcutDown() || event.isControlDown();
        boolean handled = true;
        if (shortcut) {
            switch (event.getCode()) {
                case PLUS, EQUALS, ADD -> viewport.zoomIn();
                case MINUS, SUBTRACT -> viewport.zoomOut();
                case DIGIT0, NUMPAD0 -> viewport.resetZoom();
                case DIGIT9, NUMPAD9 -> viewport.fitToViewport();
                case ENTER -> {
                    if (vectorMode && !apply.isDisabled()) apply.fire();
                    else handled = false;
                }
                case DELETE, BACK_SPACE -> {
                    if (event.isShiftDown()) clearDrawing();
                    else handled = false;
                }
                default -> handled = false;
            }
        } else {
            switch (event.getCode()) {
                case DIGIT1, NUMPAD1 -> ((ToggleButton) lookup("#tool-point")).fire();
                case DIGIT2, NUMPAD2 -> ((ToggleButton) lookup("#tool-circle")).fire();
                case DIGIT3, NUMPAD3 -> ((ToggleButton) lookup("#tool-square")).fire();
                case P -> polygonMode.setSelected(true);
                case B -> rasterMode.setSelected(true);
                case ENTER -> {
                    if (vectorMode && !finish.isDisabled()) finish.fire();
                    else handled = false;
                }
                case ESCAPE -> {
                    polygons.cancelVertexDrag();
                    stopVertexDrag();
                    polygons.cancelPolygon();
                    controller.endStroke();
                    refresh();
                }
                case DELETE, BACK_SPACE -> {
                    if (vectorMode && !delete.isDisabled()) delete.fire();
                    else handled = false;
                }
                default -> handled = false;
            }
        }
        if (handled) event.consume();
    }

    private static Spinner<Double> spinner(String id, double min, double max, double value, double step) {
        Spinner<Double> spinner = new Spinner<>(min, max, value, step);
        spinner.setId(id);
        spinner.setPrefWidth(110);
        return spinner;
    }

    private static GridPane propertyGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(6);
        return grid;
    }

    private static Label section(String name) {
        Label label = new Label(name);
        label.getStyleClass().add("section-title");
        label.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }
}
