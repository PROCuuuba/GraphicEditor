package org.example.graphiceditor.ui;

import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.example.graphiceditor.controller.EditorController;
import org.example.graphiceditor.model.DrawingModel;
import org.example.graphiceditor.model.Pixel;
import org.example.graphiceditor.model.Tool;

import java.util.List;

public final class EditorPane extends BorderPane {
    private final DrawingModel model;
    private final EditorController controller;
    private final DrawingCanvas canvas;
    private final ToggleGroup selection = new ToggleGroup();
    private final Label status = new Label();
    private final Slider brushSize = new Slider(1, 100, 16);
    private final Label sizeLabel = new Label();
    private final ColorPicker colorPicker;

    public EditorPane() {
        this(new DrawingModel(900, 650));
    }

    private EditorPane(DrawingModel model) {
        this(model, new EditorController(model));
    }

    public EditorPane(DrawingModel model, EditorController controller) {
        this.model = model;
        this.controller = controller;
        canvas = new DrawingCanvas(model);
        colorPicker = new ColorPicker(controller.getColor());
        brushSize.setValue(controller.getBrushSize());
        getStyleClass().add("editor");
        setTop(header());
        setLeft(controls());
        setCenter(workspace());
        status.setId("drawing-status");
        HBox footer = new HBox(status);
        footer.getStyleClass().add("status-bar");
        setBottom(footer);
        connectDrawing();
        updateStatus();
        sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((property, oldWindow, newWindow) -> {
                    if (newWindow != null) {
                        newWindow.focusedProperty().addListener((focus, wasFocused, isFocused) -> {
                            if (!isFocused) {
                                controller.endStroke();
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
        Label subtitle = new Label("Растровое рисование • точка, круг и квадрат");
        subtitle.getStyleClass().add("editor-subtitle");
        VBox header = new VBox(4, title, subtitle);
        header.getStyleClass().add("editor-header");
        return header;
    }

    private VBox controls() {
        VBox tools = new VBox(8);
        for (Tool tool : Tool.values()) {
            tools.getChildren().add(toolButton(tool));
        }
        selection.selectedToggleProperty().addListener((observable, previous, selected) -> {
            if (selected == null) {
                if (previous != null) {
                    previous.setSelected(true);
                }
                return;
            }
            controller.setTool((Tool) selected.getUserData());
            updateStatus();
        });

        colorPicker.setId("color-picker");
        colorPicker.setMaxWidth(Double.MAX_VALUE);
        colorPicker.valueProperty().addListener((observable, previous, selected) -> {
            controller.setColor(selected);
            updateStatus();
        });

        brushSize.setId("brush-size");
        brushSize.setShowTickLabels(true);
        brushSize.setShowTickMarks(true);
        brushSize.setMajorTickUnit(25);
        brushSize.setBlockIncrement(1);
        brushSize.valueProperty().addListener((observable, oldValue, newValue) -> {
            controller.setBrushSize(newValue.doubleValue());
            updateStatus();
        });
        Label hint = new Label("Нажмите на холст для отпечатка. Удерживайте левую кнопку мыши и ведите курсор для рисования.");
        hint.setWrapText(true);
        hint.getStyleClass().add("help-text");
        Button clear = new Button("Очистить холст");
        clear.setId("clear-canvas");
        clear.setMaxWidth(Double.MAX_VALUE);
        clear.setOnAction(event -> {
            controller.clear();
            canvas.renderAll();
            updateStatus();
        });
        VBox content = new VBox(12, section("Инструменты"), tools,
                new Separator(), section("Цвет"), colorPicker,
                new Separator(), section("Размер кисти"), brushSize, sizeLabel,
                new Separator(), hint, clear);
        content.setPrefWidth(260);
        content.setMinWidth(260);
        content.getStyleClass().add("controls-content");
        return content;
    }

    private ScrollPane workspace() {
        StackPane frame = new StackPane(canvas);
        frame.getStyleClass().add("workspace-sheet");
        frame.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane surround = new StackPane(frame);
        surround.setAlignment(Pos.TOP_LEFT);
        surround.getStyleClass().add("workspace");
        ScrollPane scroll = new ScrollPane(surround);
        scroll.setPannable(false);
        return scroll;
    }

    private ToggleButton toolButton(Tool tool) {
        ToggleButton button = new ToggleButton(tool.getDisplayName());
        button.setId("tool-" + tool.name().toLowerCase(java.util.Locale.ROOT));
        button.setUserData(tool);
        button.setToggleGroup(selection);
        button.setSelected(tool == controller.getTool());
        button.setMaxWidth(Double.MAX_VALUE);
        button.setMinHeight(36);
        button.setTooltip(new Tooltip(tool == Tool.POINT
                ? "Рисование точками размером 1 пиксель"
                : "Рисование " + (tool == Tool.CIRCLE ? "круглой" : "квадратной") + " кистью"));
        return button;
    }

    private void connectDrawing() {
        canvas.setCursor(Cursor.CROSSHAIR);
        canvas.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                draw(controller.beginStroke(event.getX(), event.getY()));
                event.consume();
            }
        });
        canvas.setOnMouseDragged(event -> {
            if (event.isPrimaryButtonDown()) {
                draw(controller.continueStroke(event.getX(), event.getY()));
                event.consume();
            }
        });
        canvas.setOnMouseExited(event -> controller.continueStroke(-1, -1));
        canvas.setOnMouseReleased(event -> controller.endStroke());
    }

    private void draw(List<Pixel> pixels) {
        canvas.drawPixels(pixels);
        updateStatus();
    }

    private void updateStatus() {
        brushSize.setDisable(controller.getTool() == Tool.POINT);
        sizeLabel.setText(controller.getTool() == Tool.POINT ? "Точка: 1 пиксель"
                : "Размер: %.0f пикс.".formatted(controller.getBrushSize()));
        status.setText("Инструмент: %s • Элементов: %d • Холст: %.0f × %.0f"
                .formatted(controller.getTool().getDisplayName(), model.size(),
                        model.getWidth(), model.getHeight()));
    }

    private static Label section(String name) {
        Label label = new Label(name);
        label.getStyleClass().add("section-title");
        label.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }
}
