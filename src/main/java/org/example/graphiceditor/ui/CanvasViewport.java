package org.example.graphiceditor.ui;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Scale;

public final class CanvasViewport extends BorderPane {
    private final DrawingCanvas canvas;
    private final Scale scale = new Scale(1, 1, 0, 0);
    private final ScrollPane scroll;
    private final StackPane content;
    private final Label zoomLabel = new Label("100%");
    private double zoom = 1;

    public CanvasViewport(DrawingCanvas canvas) {
        this.canvas = canvas;
        setId("canvas-viewport");
        StackPane frame = new StackPane(canvas);
        frame.getStyleClass().add("workspace-sheet");
        frame.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        Group scaled = new Group(frame);
        scaled.getTransforms().add(scale);
        content = new StackPane(new Group(scaled));
        content.setAlignment(Pos.TOP_LEFT);
        content.getStyleClass().add("workspace");
        scroll = new ScrollPane(content);
        scroll.setId("canvas-scroll");
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setPannable(false);
        setCenter(scroll);
        zoomLabel.setId("zoom-label");
        HBox toolbar = new HBox(8,
                button("−", "zoom-out", "Уменьшить • Ctrl/Cmd + −", this::zoomOut),
                zoomLabel,
                button("+", "zoom-in", "Увеличить • Ctrl/Cmd + +", this::zoomIn),
                button("100%", "zoom-reset", "Масштаб 100% • Ctrl/Cmd + 0", this::resetZoom),
                button("Вписать", "zoom-fit", "Вписать холст в окно • Ctrl/Cmd + 9", this::fitToViewport),
                shortcutsMenu());
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("zoom-toolbar");
        setTop(toolbar);
        scroll.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, event -> {
            if ((event.isShortcutDown() || event.isControlDown()) && event.getDeltaY() != 0) {
                Node viewport = viewportNode();
                Point2D anchor = viewport.sceneToLocal(event.getSceneX(), event.getSceneY());
                zoomAt(zoom * (event.getDeltaY() > 0 ? 1.25 : 0.8), anchor.getX(), anchor.getY());
                event.consume();
            }
        });
    }

    public double getZoom() { return zoom; }
    public void zoomIn() { setZoom(zoom * 1.25); }
    public void zoomOut() { setZoom(zoom / 1.25); }
    public void resetZoom() { setZoom(1); }

    public void setZoom(double value) {
        Bounds bounds = scroll.getViewportBounds();
        zoomAt(value, bounds.getWidth() / 2, bounds.getHeight() / 2);
    }

    public void fitToViewport() {
        Bounds bounds = scroll.getViewportBounds();
        if (bounds.getWidth() <= 0 || bounds.getHeight() <= 0) return;
        double width = Math.max(1, bounds.getWidth() - content.getInsets().getLeft()
                - content.getInsets().getRight() - 24);
        double height = Math.max(1, bounds.getHeight() - content.getInsets().getTop()
                - content.getInsets().getBottom() - 24);
        setZoom(Math.min(width / canvas.getWidth(), height / canvas.getHeight()));
        scroll.setHvalue(0);
        scroll.setVvalue(0);
    }

    private void zoomAt(double value, double anchorX, double anchorY) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Некорректный масштаб");
        double next = Math.clamp(value, 0.25, 4);
        if (next == zoom) return;
        Node viewport = viewportNode();
        Point2D logical = canvas.sceneToLocal(viewport.localToScene(anchorX, anchorY));
        zoom = next;
        scale.setX(zoom);
        scale.setY(zoom);
        canvas.setViewZoom(zoom);
        zoomLabel.setText("%.0f%%".formatted(zoom * 100));
        relayout();
        Point2D moved = viewport.sceneToLocal(canvas.localToScene(logical));
        double extraX = Math.max(0, content.getLayoutBounds().getWidth() - scroll.getViewportBounds().getWidth());
        double extraY = Math.max(0, content.getLayoutBounds().getHeight() - scroll.getViewportBounds().getHeight());
        scroll.setHvalue(extraX == 0 ? 0 : Math.clamp(
                scroll.getHvalue() + (moved.getX() - anchorX) / extraX, 0, 1));
        scroll.setVvalue(extraY == 0 ? 0 : Math.clamp(
                scroll.getVvalue() + (moved.getY() - anchorY) / extraY, 0, 1));
        relayout();
    }

    private Node viewportNode() {
        Node viewport = scroll.lookup(".viewport");
        return viewport == null ? scroll : viewport;
    }

    private void relayout() {
        if (getScene() != null) {
            getScene().getRoot().applyCss();
            getScene().getRoot().layout();
        } else {
            content.layout();
        }
    }

    private static MenuButton shortcutsMenu() {
        MenuButton menu = new MenuButton("Клавиши");
        for (String shortcut : new String[]{
                "1 / 2 / 3 — точка / круг / квадрат", "P — полигон, B — кисти",
                "Enter — завершить, Esc — отменить", "Delete / Backspace — удалить полигон",
                "Ctrl/Cmd + Enter — применить оформление", "Ctrl/Cmd + + / − — масштаб",
                "Ctrl/Cmd + 0 — 100%, + 9 — вписать", "Ctrl/Cmd + колесо — масштаб",
                "Ctrl/Cmd + Shift + Delete — очистить"}) {
            MenuItem item = new MenuItem(shortcut);
            item.setDisable(true);
            menu.getItems().add(item);
        }
        return menu;
    }

    private static Button button(String text, String id, String help, Runnable action) {
        Button button = new Button(text);
        button.setId(id);
        button.setTooltip(new Tooltip(help));
        button.setOnAction(event -> action.run());
        return button;
    }
}
