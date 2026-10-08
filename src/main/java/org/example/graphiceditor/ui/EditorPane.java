package org.example.graphiceditor.ui;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

public final class EditorPane extends BorderPane {
    private final ToggleGroup selection = new ToggleGroup();
    private final Label status = new Label("Последняя фигура: —");

    public EditorPane() {
        getStyleClass().add("editor");
        Label title = new Label("Графический редактор");
        title.getStyleClass().add("editor-title");
        Label subtitle = new Label("Без названия");
        subtitle.getStyleClass().add("editor-subtitle");
        VBox header = new VBox(4, title, subtitle);
        header.getStyleClass().add("editor-header");
        setTop(header);
        setLeft(controls());

        Region sheet = new Region();
        sheet.setPrefSize(900, 650);
        sheet.setMaxSize(900, 650);
        sheet.getStyleClass().add("workspace-sheet");
        StackPane workspace = new StackPane(sheet);
        workspace.setAlignment(Pos.CENTER);
        workspace.getStyleClass().add("workspace");
        setCenter(workspace);

        HBox footer = new HBox(status);
        footer.getStyleClass().add("status-bar");
        setBottom(footer);
    }

    private ScrollPane controls() {
        GridPane parameters = grid();
        addField(parameters, 0, "X", new TextField("60"));
        addField(parameters, 1, "Y", new TextField("60"));
        addField(parameters, 2, "Ширина / диаметр", new TextField("160"));
        addField(parameters, 3, "Высота", new TextField("100"));
        addField(parameters, 4, "Радиус скругления", new TextField("20"));
        addField(parameters, 5, "Количество", new TextField("1"));
        GridPane colors = grid();
        addField(colors, 0, "Заливка", new ColorPicker(Color.web("#93c5fd")));
        addField(colors, 1, "Контур", new ColorPicker(Color.web("#1e3a8a")));
        VBox figures = new VBox(8, figureButton("Круг"), figureButton("Эллипс"),
                figureButton("Прямоугольник"), figureButton("Скруглённый прямоугольник"),
                figureButton("Квадрат"));
        Button clear = new Button("Очистить холст");
        clear.setMaxWidth(Double.MAX_VALUE);
        clear.setOnAction(event -> {
            selection.selectToggle(null);
            status.setText("Последняя фигура: —");
        });
        VBox content = new VBox(12, section("Параметры"), parameters,
                new Separator(), section("Цвета"), colors,
                new Separator(), section("Фигуры"), figures, new Separator(), clear);
        content.setPrefWidth(330);
        content.getStyleClass().add("controls-content");
        ScrollPane panel = new ScrollPane(content);
        panel.setFitToWidth(true);
        panel.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        panel.setPrefWidth(335);
        return panel;
    }

    private ToggleButton figureButton(String name) {
        ToggleButton button = new ToggleButton(name);
        button.setToggleGroup(selection);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setMinHeight(36);
        button.setOnAction(event -> status.setText(
                button.isSelected() ? "Выбрана фигура: " + name : "Последняя фигура: —"));
        return button;
    }

    private static GridPane grid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        return grid;
    }

    private static Label section(String name) {
        Label label = new Label(name);
        label.getStyleClass().add("section-title");
        label.setMinWidth(Region.USE_PREF_SIZE);
        label.setMinHeight(Region.USE_PREF_SIZE);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private static void addField(GridPane grid, int row, String title, javafx.scene.Node control) {
        grid.add(new Label(title), 0, row);
        grid.add(control, 1, row);
        if (control instanceof TextField field) {
            field.setPrefColumnCount(7);
        }
        GridPane.setHgrow(control, Priority.ALWAYS);
    }
}
