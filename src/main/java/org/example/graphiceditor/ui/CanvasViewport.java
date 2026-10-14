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
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Scale;

import java.util.Objects;

public final class CanvasViewport extends BorderPane {
    private final DrawingCanvas canvas;
    private final Scale scale = new Scale(1, 1, 0, 0);
    private final Group sheet;
    private final ScrollPane scroll;
    private final StackPane content;
    private final Label zoomLabel = new Label("100%");
    private double zoom = 1;
    private boolean automaticFit = true;
    private boolean fitting;
    private Runnable beforeZoom = () -> {};

    public CanvasViewport(DrawingCanvas canvas) {
        this.canvas = canvas;
        setId("canvas-viewport");
        StackPane frame = new StackPane(canvas);
        frame.getStyleClass().add("workspace-sheet");
        frame.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        sheet = new Group(frame);
        sheet.getTransforms().add(scale);
        content = new StackPane(new Group(sheet));
        content.setAlignment(Pos.CENTER);
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
                button("Вписать", "zoom-fit", "Автоматически вписывать холст при изменении окна • Ctrl/Cmd + 9", this::fitToViewport),
                shortcutsMenu());
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("zoom-toolbar");
        setTop(toolbar);
        scroll.viewportBoundsProperty().addListener((observable, previous, current) -> {
            if (automaticFit && !fitting) requestLayout();
        });
        scroll.addEventFilter(ScrollEvent.SCROLL, event -> {
            boolean modified = event.isShortcutDown() || event.isControlDown();
            if ((!event.isDirect() || modified) && !event.isAltDown() && !event.isShiftDown()
                    && Double.isFinite(event.getDeltaY()) && event.getDeltaY() != 0) {
                double factor = Math.exp(Math.clamp(event.getDeltaY(), -240, 240) * 0.005);
                gestureZoom(factor, event.getSceneX(), event.getSceneY());
                event.consume();
            }
        });
        scroll.addEventFilter(ZoomEvent.ZOOM, event -> {
            double factor = event.getZoomFactor();
            if (Double.isFinite(factor) && factor > 0) {
                gestureZoom(factor, event.getSceneX(), event.getSceneY());
            }
            event.consume();
        });
    }

    public double getZoom() { return zoom; }
    public void setBeforeZoom(Runnable action) { beforeZoom = Objects.requireNonNull(action); }
    public void zoomIn() { setZoom(zoom * 1.25); }
    public void zoomOut() { setZoom(zoom / 1.25); }
    public void resetZoom() { setZoom(1); }

    public void setZoom(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Некорректный масштаб");
        automaticFit = false;
        Bounds bounds = scroll.getViewportBounds();
        zoomAt(value, bounds.getWidth() / 2, bounds.getHeight() / 2);
    }

    public void fitToViewport() {
        automaticFit = true;
        requestLayout();
        relayout();
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        scroll.layout();
        if (!automaticFit || fitting) return;
        fitting = true;
        try {
            for (int attempt = 0; attempt < 3; attempt++) {
                Bounds bounds = scroll.getViewportBounds();
                Bounds sheetBounds = sheet.getLayoutBounds();
                if (bounds.getWidth() <= 0 || bounds.getHeight() <= 0
                        || sheetBounds.getWidth() <= 0 || sheetBounds.getHeight() <= 0) break;
                double width = Math.max(1, bounds.getWidth() - content.getInsets().getLeft()
                        - content.getInsets().getRight() - 2);
                double height = Math.max(1, bounds.getHeight() - content.getInsets().getTop()
                        - content.getInsets().getBottom() - 2);
                double next = Math.clamp(Math.min(width / sheetBounds.getWidth(),
                        height / sheetBounds.getHeight()), 0.25, 4);
                if (Math.abs(next - zoom) < 0.000001) break;
                updateScale(next);
                scroll.requestLayout();
                scroll.layout();
            }
            scroll.setHvalue(0.5);
            scroll.setVvalue(0.5);
        } finally {
            fitting = false;
        }
    }

    private void gestureZoom(double factor, double sceneX, double sceneY) {
        automaticFit = false;
        Point2D anchor = viewportNode().sceneToLocal(sceneX, sceneY);
        zoomAt(Math.clamp(zoom * factor, 0.25, 4), anchor.getX(), anchor.getY());
    }

    private void zoomAt(double value, double anchorX, double anchorY) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Некорректный масштаб");
        double next = Math.clamp(value, 0.25, 4);
        if (next == zoom) return;
        Node viewport = viewportNode();
        Point2D logical = canvas.sceneToLocal(viewport.localToScene(anchorX, anchorY));
        updateScale(next);
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

    private void updateScale(double value) {
        beforeZoom.run();
        zoom = value;
        scale.setX(zoom);
        scale.setY(zoom);
        canvas.setViewZoom(zoom);
        zoomLabel.setText("%.0f%%".formatted(zoom * 100));
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
                "Ctrl/Cmd + 0 — 100%, + 9 — авто-вписать", "Колесо мыши / жест прокрутки — масштаб",
                "Сведение / разведение пальцев — масштаб", "Shift + колесо — прокрутка холста",
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
